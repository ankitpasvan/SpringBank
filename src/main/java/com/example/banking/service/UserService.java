package com.example.banking.service;

import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.banking.dto.RegisterRequest;
import com.example.banking.dto.UserResponse;
import com.example.banking.exception.DuplicateEmailException;
import com.example.banking.exception.InvalidCredentialsException;
import com.example.banking.exception.ResourceNotFoundException;
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


    public UserResponse verifyCredentials(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        String hashToCheck = user != null ? user.getPasswordHash() : DUMMY_HASH_FOR_TIMING_SAFETY;

        boolean passwordMatches = passwordEncoder.matches(password, hashToCheck);

        if (user == null || !passwordMatches) {
            log.info("Login failed for email={}", normalizedEmail);
            throw new InvalidCredentialsException();
        }

        log.info("Login succeeded for id={}", user.getId());
        return UserResponse.from(user);
    }


    public UserResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return UserResponse.from(user);
    }

    public UserResponse updateProfile(String userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setName(name.trim());
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        log.info("Profile updated for id={}", userId);
        return UserResponse.from(saved);
    }


    private static final String DUMMY_HASH_FOR_TIMING_SAFETY =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5uD1DkY.i.9Q5B/D6UwynMBFhCynW";
}
