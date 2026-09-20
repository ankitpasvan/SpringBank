package com.example.banking.service;


import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.banking.dto.RegisterRequest;
import com.example.banking.dto.UserResponse;
import com.example.banking.exception.DuplicateEmailException;
import com.example.banking.model.User;
import com.example.banking.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request) {
        // "Ankit@Example.com" and "ankit@example.com" must be the same account
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.name().trim(), email, passwordHash);

        try {
            User saved = userRepository.save(user);
            log.info("User registered with id={}", saved.getId());
            return UserResponse.from(saved);
        } catch (DuplicateKeyException ex) {
            // Two requests with the same email at the same time can both pass the
            // check above; the unique index catches the second one.
            throw new DuplicateEmailException(email);
        }
    }
}
