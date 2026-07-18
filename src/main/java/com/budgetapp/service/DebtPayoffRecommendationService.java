package com.budgetapp.service;

import com.budgetapp.dto.DashboardSnapshot.PaymentRecommendation;
import com.budgetapp.model.Debt;
import com.budgetapp.model.SavingsGoal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns a user's monthly extra allocation into a concrete $ and % recommendation
 * per active debt. Three strategies:
 *
 *  AVALANCHE      - all extra $ to the highest interest rate debt first (mathematically
 *                    cheapest over time), minimums to everything else.
 *  SNOWBALL       - all extra $ to the smallest balance first (fastest visible wins,
 *                    good for motivation), minimums to everything else.
 *  GOAL_WEIGHTED  - splits the extra proportionally to each debt's interest cost,
 *                    so every debt gets some progress every month instead of one
 *                    debt hogging all the extra payment.
 */
@Service
public class DebtPayoffRecommendationService {

    public List<PaymentRecommendation> recommend(List<Debt> activeDebts, SavingsGoal goal) {
        List<PaymentRecommendation> results = new ArrayList<>();
        if (activeDebts.isEmpty()) return results;

        BigDecimal totalMinimums = activeDebts.stream()
                .map(Debt::getMinimumPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal extra = goal != null && goal.getMonthlyAllocation() != null
                ? goal.getMonthlyAllocation().subtract(totalMinimums).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        SavingsGoal.Strategy strategy = goal != null ? goal.getStrategy() : SavingsGoal.Strategy.AVALANCHE;

        switch (strategy) {
            case SNOWBALL -> applyFocused(activeDebts, extra, results,
                    Comparator.comparing(Debt::getCurrentBalance),
                    "Smallest balance — snowball strategy for fastest payoff momentum");
            case GOAL_WEIGHTED -> applyWeighted(activeDebts, extra, results);
            case AVALANCHE -> applyFocused(activeDebts, extra, results,
                    Comparator.comparing(Debt::getInterestRateApr).reversed(),
                    "Highest interest rate — avalanche strategy minimizes total interest paid");
            default -> applyFocused(activeDebts, extra, results,
                    Comparator.comparing(Debt::getInterestRateApr).reversed(), "Highest interest rate");
        }

        BigDecimal totalRecommended = totalMinimums.add(extra);
        for (PaymentRecommendation r : results) {
            BigDecimal pct = totalRecommended.compareTo(BigDecimal.ZERO) > 0
                    ? r.getRecommendedAmount().divide(totalRecommended, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            r.setRecommendedPercent(pct.setScale(1, RoundingMode.HALF_UP));
        }
        return results;
    }

    /** Minimums to everyone, all "extra" dollars to a single targeted debt chosen by comparator. */
    private void applyFocused(List<Debt> debts, BigDecimal extra, List<PaymentRecommendation> results,
                               Comparator<Debt> priorityOrder, String rationale) {
        Debt priority = debts.stream().min(priorityOrder).orElse(null);

        for (Debt d : debts) {
            BigDecimal amount = d.getMinimumPayment();
            String note = "Minimum payment to stay current";
            if (priority != null && d.getId().equals(priority.getId())) {
                amount = amount.add(extra);
                note = rationale;
            }
            results.add(new PaymentRecommendation(d.getId(), d.getName(), amount, BigDecimal.ZERO, note));
        }
    }

    /** Minimums to everyone, extra split proportionally to each debt's monthly interest cost. */
    private void applyWeighted(List<Debt> debts, BigDecimal extra, List<PaymentRecommendation> results) {
        BigDecimal totalInterestCost = debts.stream()
                .map(d -> monthlyInterestCost(d))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Debt d : debts) {
            BigDecimal amount = d.getMinimumPayment();
            String note = "Minimum payment to stay current";
            if (totalInterestCost.compareTo(BigDecimal.ZERO) > 0 && extra.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal share = monthlyInterestCost(d).divide(totalInterestCost, 6, RoundingMode.HALF_UP);
                BigDecimal weighted = extra.multiply(share).setScale(2, RoundingMode.HALF_UP);
                amount = amount.add(weighted);
                note = "Weighted by this debt's share of your total monthly interest cost";
            }
            results.add(new PaymentRecommendation(d.getId(), d.getName(), amount, BigDecimal.ZERO, note));
        }
    }

    private BigDecimal monthlyInterestCost(Debt d) {
        return d.getCurrentBalance()
                .multiply(d.getInterestRateApr())
                .divide(BigDecimal.valueOf(1200), 6, RoundingMode.HALF_UP); // APR% / 12 months / 100
    }
}
