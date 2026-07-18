package com.budgetapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    // If this transaction is a debt payment, link it so paid-off progress can be tracked.
    @ManyToOne
    @JoinColumn(name = "debt_id")
    private Debt debt;

    private String description;

    // Positive = income/deposit, Negative = expense/payment.
    // This sign convention is what feeds the color-coded chart (green vs red).
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Category category;   // INCOME, DEBT_PAYMENT, BILL, GROCERIES, SAVINGS_TRANSFER, OTHER

    private LocalDate date;

    public enum Category { INCOME, DEBT_PAYMENT, BILL, GROCERIES, SAVINGS_TRANSFER, OTHER }
}
