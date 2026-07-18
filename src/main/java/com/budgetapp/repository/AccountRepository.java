package com.budgetapp.repository;

import com.budgetapp.model.Account;
import com.budgetapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByOwner(User owner);
}
