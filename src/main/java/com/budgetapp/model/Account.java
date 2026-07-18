package com.budgetapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User owner;

    private String name;              // "Checking", "Cash", "Emergency Fund"

    @Enumerated(EnumType.STRING)
    private AccountType type;         // CHECKING, SAVINGS, CASH, CREDIT

    // Running balance, updated whenever a transaction posts against this account.
    // Kept denormalized on the entity so reads are O(1) instead of summing transactions every time.
    private BigDecimal balance = BigDecimal.ZERO;

    public enum AccountType { CHECKING, SAVINGS, CASH, CREDIT }
}
