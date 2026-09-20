package com.example.banking.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.banking.dto.RegisterRequest;
import com.example.banking.dto.UserResponse;
import com.example.banking.exception.DuplicateEmailException;
import com.example.banking.model.User;
import com.example.banking.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    // Real encoder on purpose: we want to prove real BCrypt hashing happens.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void register_success_returnsUserResponse() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.register(
                new RegisterRequest("Ankit", "ankit@example.com", "SecurePassword123"));

        assertEquals("Ankit", response.name());
        assertEquals("ankit@example.com", response.email());
    }

    @Test
    void register_storesHashedPassword_notPlaintext() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.register(new RegisterRequest("Ankit", "ankit@example.com", "SecurePassword123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String storedHash = captor.getValue().getPasswordHash();

        assertNotEquals("SecurePassword123", storedHash);
        assertTrue(storedHash.startsWith("$2"), "BCrypt hashes start with $2");
        assertTrue(passwordEncoder.matches("SecurePassword123", storedHash));
    }

    @Test
    void register_normalizesEmailToLowerCase() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.register(
                new RegisterRequest("Ankit", "  Ankit@Example.COM ", "SecurePassword123"));

        assertEquals("ankit@example.com", response.email());
        verify(userRepository).existsByEmail("ankit@example.com");
    }

    @Test
    void register_duplicateEmail_throwsAndDoesNotSave() {
        when(userRepository.existsByEmail("ankit@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.register(
                new RegisterRequest("Ankit", "ankit@example.com", "SecurePassword123")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_duplicateKeyFromDatabase_isConvertedToDuplicateEmailException() {
        when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThrows(DuplicateEmailException.class, () -> userService.register(
                new RegisterRequest("Ankit", "ankit@example.com", "SecurePassword123")));
    }
}

