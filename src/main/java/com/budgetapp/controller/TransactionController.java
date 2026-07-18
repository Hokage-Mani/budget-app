package com.budgetapp.controller;

import com.budgetapp.model.Account;
import com.budgetapp.model.Debt;
import com.budgetapp.model.Transaction;
import com.budgetapp.model.User;
import com.budgetapp.repository.AccountRepository;
import com.budgetapp.repository.DebtRepository;
import com.budgetapp.repository.TransactionRepository;
import com.budgetapp.service.BalanceService;
import com.budgetapp.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final DebtRepository debtRepository;
    private final BalanceService balanceService;
    private final DashboardService dashboardService;

    public TransactionController(TransactionRepository transactionRepository, AccountRepository accountRepository,
                                  DebtRepository debtRepository, BalanceService balanceService,
                                  DashboardService dashboardService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.debtRepository = debtRepository;
        this.balanceService = balanceService;
        this.dashboardService = dashboardService;
    }

    public record TransactionRequest(Long accountId, Long debtId, String description,
                                      BigDecimal amount, String category, LocalDate date) {}

    @PostMapping
    public Transaction create(@RequestBody TransactionRequest req, Authentication auth) {
        User user = (User) auth.getPrincipal();

        Account account = accountRepository.findById(req.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setDescription(req.description());
        tx.setAmount(req.amount());
        tx.setCategory(Transaction.Category.valueOf(req.category()));
        tx.setDate(req.date() != null ? req.date() : LocalDate.now());

        if (req.debtId() != null) {
            Debt debt = debtRepository.findById(req.debtId()).orElse(null);
            tx.setDebt(debt);
        }

        Transaction saved = transactionRepository.save(tx);
        balanceService.applyTransaction(saved);

        // Push a fresh snapshot to every open tab for this user immediately.
        dashboardService.broadcast(user);

        return saved;
    }
}
