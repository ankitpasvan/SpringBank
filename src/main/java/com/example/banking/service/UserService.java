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
import com.example.banking.exception.InvalidCredentialsException;
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

    /**
     * Verifies email + password for login (Step 1 of JWT auth: credential check only,
     * no token yet). Returns the safe UserResponse on success.
     *
     * SECURITY: unknown email and wrong password both throw the SAME exception with the
     * SAME message, and we always run passwordEncoder.matches() even when the user is not
     * found (against a fixed dummy hash). Two reasons:
     *   1) different error messages would let an attacker discover which emails are registered.
     *   2) skipping the BCrypt check entirely for "user not found" would make that path faster
     *      than a wrong-password path, which is itself a timing side-channel an attacker could
     *      use to guess valid emails.
     */
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

    // A pre-computed BCrypt hash of a random value. It matches no real password;
    // it exists only so a "user not found" login still costs one BCrypt comparison.
    private static final String DUMMY_HASH_FOR_TIMING_SAFETY =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5uD1DkY.i.9Q5B/D6UwynMBFhCynW";
}