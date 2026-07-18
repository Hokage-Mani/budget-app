package com.budgetapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User owner;

    private String name;                  // "Emergency Fund", "Debt-Free by 2027"
    private BigDecimal targetAmount;
    private BigDecimal currentAmount = BigDecimal.ZERO;
    private LocalDate targetDate;

    // How much extra money per month the user has available to allocate
    // beyond minimum payments/expenses. This is the number the recommendation
    // engine splits across debts.
    private BigDecimal monthlyAllocation;

    @Enumerated(EnumType.STRING)
    private Strategy strategy = Strategy.AVALANCHE;

    public enum Strategy { AVALANCHE, SNOWBALL, GOAL_WEIGHTED }
}
