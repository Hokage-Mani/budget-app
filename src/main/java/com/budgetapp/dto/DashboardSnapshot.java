package com.budgetapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class DashboardSnapshot {
    private BigDecimal totalBalance;
    private BigDecimal totalDebt;
    private List<AccountBalance> accountBalances;
    private List<DebtStatus> activeDebts;
    private List<DebtStatus> paidOffDebts;
    private List<ChartPoint> chartSeries;          // for the color-coded progress/impact chart
    private List<PaymentRecommendation> recommendations;

    @Data
    @AllArgsConstructor
    public static class AccountBalance {
        private Long accountId;
        private String name;
        private BigDecimal balance;
    }

    @Data
    @AllArgsConstructor
    public static class DebtStatus {
        private Long debtId;
        private String name;
        private BigDecimal currentBalance;
        private BigDecimal originalBalance;
        private BigDecimal percentPaidOff;
        private String dueDate; // ISO date, nullable if paid off
    }

    @Data
    @AllArgsConstructor
    public static class ChartPoint {
        private String date;       // ISO date
        private BigDecimal amount; // positive = progress (income/paydown), negative = impact (expense)
        private String label;
    }

    @Data
    @AllArgsConstructor
    public static class PaymentRecommendation {
        private Long debtId;
        private String debtName;
        private BigDecimal recommendedAmount;
        private BigDecimal recommendedPercent;
        private String rationale;
    }
}
