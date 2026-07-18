package com.budgetapp.controller;

import com.budgetapp.model.SavingsGoal;
import com.budgetapp.model.User;
import com.budgetapp.repository.SavingsGoalRepository;
import com.budgetapp.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {

    private final SavingsGoalRepository savingsGoalRepository;
    private final DashboardService dashboardService;

    public SavingsGoalController(SavingsGoalRepository savingsGoalRepository, DashboardService dashboardService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.dashboardService = dashboardService;
    }

    public record GoalRequest(String name, BigDecimal targetAmount, LocalDate targetDate,
                               BigDecimal monthlyAllocation, String strategy) {}

    @GetMapping
    public List<SavingsGoal> list(Authentication auth) {
        return savingsGoalRepository.findByOwner((User) auth.getPrincipal());
    }

    @PostMapping
    public SavingsGoal create(@RequestBody GoalRequest req, Authentication auth) {
        User user = (User) auth.getPrincipal();
        SavingsGoal goal = new SavingsGoal();
        goal.setOwner(user);
        goal.setName(req.name());
        goal.setTargetAmount(req.targetAmount());
        goal.setTargetDate(req.targetDate());
        goal.setMonthlyAllocation(req.monthlyAllocation());
        goal.setStrategy(SavingsGoal.Strategy.valueOf(req.strategy()));
        SavingsGoal saved = savingsGoalRepository.save(goal);

        // Recommendations depend on this goal, so push a refreshed dashboard immediately.
        dashboardService.broadcast(user);
        return saved;
    }
}
