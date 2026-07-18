package com.budgetapp.controller;

import com.budgetapp.dto.DashboardSnapshot;
import com.budgetapp.model.User;
import com.budgetapp.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardSnapshot getDashboard(org.springframework.security.core.Authentication auth) {
        User user = (User) auth.getPrincipal();
        return dashboardService.buildSnapshot(user);
    }
}
