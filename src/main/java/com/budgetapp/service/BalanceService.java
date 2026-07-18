package com.budgetapp.service;

import com.budgetapp.model.Account;
import com.budgetapp.model.Debt;
import com.budgetapp.model.Transaction;
import com.budgetapp.repository.AccountRepository;
import com.budgetapp.repository.DebtRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class BalanceService {

    private final AccountRepository accountRepository;
    private final DebtRepository debtRepository;

    public BalanceService(AccountRepository accountRepository, DebtRepository debtRepository) {
        this.accountRepository = accountRepository;
        this.debtRepository = debtRepository;
    }

    /** Applies a posted transaction's effect on its account balance and (if linked) a debt's balance. */
    public void applyTransaction(Transaction tx) {
        Account account = tx.getAccount();
        account.setBalance(account.getBalance().add(tx.getAmount()));
        accountRepository.save(account);

        if (tx.getDebt() != null && tx.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            Debt debt = tx.getDebt();
            BigDecimal newBalance = debt.getCurrentBalance().add(tx.getAmount()); // amount is negative
            debt.setCurrentBalance(newBalance.max(BigDecimal.ZERO));
            if (debt.getCurrentBalance().compareTo(BigDecimal.ZERO) == 0 && !debt.isPaidOff()) {
                debt.setPaidOff(true);
                debt.setPaidOffDate(LocalDate.now());
            }
            debtRepository.save(debt);
        }
    }
}
