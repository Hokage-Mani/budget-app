package com.budgetapp.controller;

import com.budgetapp.model.Debt;
import com.budgetapp.model.User;
import com.budgetapp.repository.DebtRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/debts")
public class DebtController {

    private final DebtRepository debtRepository;

    public DebtController(DebtRepository debtRepository) {
        this.debtRepository = debtRepository;
    }

    public record DebtRequest(String name, BigDecimal balance, BigDecimal interestRateApr,
                               BigDecimal minimumPayment, LocalDate dueDate) {}

    @GetMapping
    public List<Debt> list(Authentication auth) {
        return debtRepository.findByOwner((User) auth.getPrincipal());
    }

    @PostMapping
    public Debt create(@RequestBody DebtRequest req, Authentication auth) {
        Debt debt = new Debt();
        debt.setOwner((User) auth.getPrincipal());
        debt.setName(req.name());
        debt.setOriginalBalance(req.balance());
        debt.setCurrentBalance(req.balance());
        debt.setInterestRateApr(req.interestRateApr());
        debt.setMinimumPayment(req.minimumPayment());
        debt.setDueDate(req.dueDate());
        return debtRepository.save(debt);
    }
}
