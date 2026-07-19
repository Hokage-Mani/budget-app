package com.budgetapp.controller;

import com.budgetapp.dto.AuthDtos.*;
import com.budgetapp.exception.UserAlreadyExistsException;
import com.budgetapp.model.User;
import com.budgetapp.repository.UserRepository;
import com.budgetapp.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {

        // 1. Throw the custom exception if the email exists
        if (userRepository.existsByEmail(req.email())) {
            throw new UserAlreadyExistsException("An account with this email already exists.");
        }

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setDisplayName(req.displayName());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getDisplayName()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        User user = userRepository.findByEmail(req.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getDisplayName()));
    }
}
