package com.budgetapp.service;

import com.budgetapp.exception.UserAlreadyExistsException;
import com.budgetapp.model.User;
import com.budgetapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerNewUser(User user) {
        // 1. Check if the email is already in the database
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            // Throw the custom exception we just made!
            throw new UserAlreadyExistsException("An account with this email already exists.");
        }

        // 2. Encrypt the raw text password using BCrypt
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 3. Save the secure user to the database
        return userRepository.save(user);
    }
}