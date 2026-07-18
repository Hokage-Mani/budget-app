package com.budgetapp.service;

import com.budgetapp.dto.DashboardSnapshot;
import com.budgetapp.dto.DashboardSnapshot.*;
import com.budgetapp.model.*;
import com.budgetapp.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final AccountRepository accountRepository;
    private final DebtRepository debtRepository;
    private final TransactionRepository transactionRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final DebtPayoffRecommendationService recommendationService;
    private final SimpMessagingTemplate messagingTemplate;

    public DashboardService(AccountRepository accountRepository, DebtRepository debtRepository,
                             TransactionRepository transactionRepository, SavingsGoalRepository savingsGoalRepository,
                             DebtPayoffRecommendationService recommendationService,
                             SimpMessagingTemplate messagingTemplate) {
        this.accountRepository = accountRepository;
        this.debtRepository = debtRepository;
        this.transactionRepository = transactionRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.recommendationService = recommendationService;
        this.messagingTemplate = messagingTemplate;
    }

    public DashboardSnapshot buildSnapshot(User user) {
        List<Account> accounts = accountRepository.findByOwner(user);
        List<Debt> active = debtRepository.findByOwnerAndPaidOffFalse(user);
        List<Debt> paidOff = debtRepository.findByOwnerAndPaidOffTrue(user);
        List<SavingsGoal> goals = savingsGoalRepository.findByOwner(user);
        SavingsGoal primaryGoal = goals.isEmpty() ? null : goals.get(0);

        BigDecimal totalBalance = accounts.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDebt = active.stream().map(Debt::getCurrentBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AccountBalance> accountBalances = accounts.stream()
                .map(a -> new AccountBalance(a.getId(), a.getName(), a.getBalance()))
                .collect(Collectors.toList());

        List<DebtStatus> activeDebts = active.stream().map(this::toDebtStatus).collect(Collectors.toList());
        List<DebtStatus> paidOffDebts = paidOff.stream().map(this::toDebtStatus).collect(Collectors.toList());

        List<ChartPoint> chartSeries = buildChartSeries(user);

        List<PaymentRecommendation> recommendations = recommendationService.recommend(active, primaryGoal);

        return new DashboardSnapshot(totalBalance, totalDebt, accountBalances, activeDebts, paidOffDebts,
                chartSeries, recommendations);
    }

    /** Last 90 days of transactions, one chart point per transaction: positive=progress, negative=impact. */
    private List<ChartPoint> buildChartSeries(User user) {
        LocalDate start = LocalDate.now().minusDays(90);
        LocalDate end = LocalDate.now();
        return transactionRepository.findByAccount_Owner_IdAndDateBetween(user.getId(), start, end).stream()
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .map(tx -> new ChartPoint(tx.getDate().toString(), tx.getAmount(), tx.getDescription()))
                .collect(Collectors.toList());
    }

    private DebtStatus toDebtStatus(Debt d) {
        BigDecimal percentPaid = d.getOriginalBalance().compareTo(BigDecimal.ZERO) > 0
                ? d.getOriginalBalance().subtract(d.getCurrentBalance())
                    .divide(d.getOriginalBalance(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        return new DebtStatus(d.getId(), d.getName(), d.getCurrentBalance(), d.getOriginalBalance(),
                percentPaid.setScale(1, RoundingMode.HALF_UP),
                d.getDueDate() != null ? d.getDueDate().toString() : null);
    }

    /** Call this after any transaction/debt/account mutation to push a fresh snapshot to the user's open tabs. */
    public void broadcast(User user) {
        DashboardSnapshot snapshot = buildSnapshot(user);
        messagingTemplate.convertAndSend("/topic/dashboard/" + user.getId(), snapshot);
    }
}
