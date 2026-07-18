package com.budgetapp.service;

import com.budgetapp.dto.DashboardSnapshot.PaymentRecommendation;
import com.budgetapp.model.Debt;
import com.budgetapp.model.SavingsGoal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DebtPayoffRecommendationServiceTest {

    private DebtPayoffRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new DebtPayoffRecommendationService();
    }

    @Test
    void testGoalWeightedStrategy() {
        // 1. Setup Mock Debts
        Debt highInterestCreditCard = new Debt();
        highInterestCreditCard.setId(1L);
        highInterestCreditCard.setName("Visa");
        highInterestCreditCard.setCurrentBalance(new BigDecimal("5000.00"));
        highInterestCreditCard.setInterestRateApr(new BigDecimal("24.00")); // 24% APR
        highInterestCreditCard.setMinimumPayment(new BigDecimal("100.00"));

        Debt lowInterestLoan = new Debt();
        lowInterestLoan.setId(2L);
        lowInterestLoan.setName("Car Loan");
        lowInterestLoan.setCurrentBalance(new BigDecimal("10000.00"));
        lowInterestLoan.setInterestRateApr(new BigDecimal("6.00")); // 6% APR
        lowInterestLoan.setMinimumPayment(new BigDecimal("200.00"));

        List<Debt> activeDebts = List.of(highInterestCreditCard, lowInterestLoan);

        // 2. Setup Mock Goal (Total Allocation = $500. Total minimums = $300. Extra = $200)
        SavingsGoal goal = new SavingsGoal();
        goal.setMonthlyAllocation(new BigDecimal("500.00"));
        goal.setStrategy(SavingsGoal.Strategy.GOAL_WEIGHTED);

        // 3. Execute
        List<PaymentRecommendation> recommendations = service.recommend(activeDebts, goal);

        // 4. Assert Math
        // Visa Monthly Interest: 5000 * 24 / 1200 = $100
        // Car Loan Monthly Interest: 10000 * 6 / 1200 = $50
        // Total Interest = $150.
        // Visa Share: 100/150 (66.66%). Car Share: 50/150 (33.33%).
        // Extra pool: $200.
        // Visa should get $100 (min) + $133.33 (extra) = $233.33
        // Car should get $200 (min) + $66.67 (extra) = $266.67

        PaymentRecommendation visaRec = recommendations.stream()
                .filter(r -> r.getDebtName().equals("Visa")).findFirst().get();
        PaymentRecommendation carRec = recommendations.stream()
                .filter(r -> r.getDebtName().equals("Car Loan")).findFirst().get();

        assertEquals(new BigDecimal("233.33"), visaRec.getRecommendedAmount());
        assertEquals(new BigDecimal("266.67"), carRec.getRecommendedAmount());
    }
}