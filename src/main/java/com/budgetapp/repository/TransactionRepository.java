package com.budgetapp.repository;

import com.budgetapp.model.Account;
import com.budgetapp.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountOrderByDateDesc(Account account);
    List<Transaction> findByAccount_Owner_IdAndDateBetween(Long ownerId, LocalDate start, LocalDate end);
}
