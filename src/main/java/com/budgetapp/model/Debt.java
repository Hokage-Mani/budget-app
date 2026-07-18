package com.budgetapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User owner;

    private String name;              // "Visa Card", "Car Loan", "Student Loan"

    private BigDecimal originalBalance;
    private BigDecimal currentBalance;

    private BigDecimal interestRateApr;   // e.g. 22.99 for 22.99%
    private BigDecimal minimumPayment;

    private LocalDate dueDate;            // recurring day of month is derived from this on the frontend

    private boolean paidOff = false;
    private LocalDate paidOffDate;
}
