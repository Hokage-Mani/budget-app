package com.budgetapp.repository;

import com.budgetapp.model.SavingsGoal;
import com.budgetapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByOwner(User owner);
}
