package com.budgetapp.repository;

import com.budgetapp.model.Debt;
import com.budgetapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DebtRepository extends JpaRepository<Debt, Long> {
    List<Debt> findByOwnerAndPaidOffFalse(User owner);
    List<Debt> findByOwnerAndPaidOffTrue(User owner);
    List<Debt> findByOwner(User owner);
}
