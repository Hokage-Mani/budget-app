package com.budgetapp.controller;

import com.budgetapp.model.Account;
import com.budgetapp.model.User;
import com.budgetapp.repository.AccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public record AccountRequest(String name, String type, BigDecimal openingBalance) {}

    @GetMapping
    public List<Account> list(Authentication auth) {
        return accountRepository.findByOwner((User) auth.getPrincipal());
    }

    @PostMapping
    public Account create(@RequestBody AccountRequest req, Authentication auth) {
        Account account = new Account();
        account.setOwner((User) auth.getPrincipal());
        account.setName(req.name());
        account.setType(Account.AccountType.valueOf(req.type()));
        account.setBalance(req.openingBalance() != null ? req.openingBalance() : BigDecimal.ZERO);
        return accountRepository.save(account);
    }
}
