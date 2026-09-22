package com.example.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
import com.example.banking.exception.InvalidCredentialsException;
import com.example.banking.model.User;
import com.example.banking.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String EMAIL = "ankit@example.com";
    private static final String PASSWORD = "SecurePassword123";

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
                new RegisterRequest("Ankit", EMAIL, PASSWORD));

        assertEquals("Ankit", response.name());
        assertEquals(EMAIL, response.email());
    }

    @Test
    void register_storesHashedPassword_notPlaintext() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.register(new RegisterRequest("Ankit", EMAIL, PASSWORD));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String storedHash = captor.getValue().getPasswordHash();

        assertNotEquals(PASSWORD, storedHash);
        assertTrue(storedHash.startsWith("$2"), "BCrypt hashes start with $2");
        assertTrue(passwordEncoder.matches(PASSWORD, storedHash));
    }

    @Test
    void register_normalizesEmailToLowerCase() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.register(
                new RegisterRequest("Ankit", "  Ankit@Example.COM ", PASSWORD));

        assertEquals(EMAIL, response.email());
        verify(userRepository).existsByEmail(EMAIL);
    }

    @Test
    void register_duplicateEmail_throwsAndDoesNotSave() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.register(
                new RegisterRequest("Ankit", EMAIL, PASSWORD)));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_duplicateKeyFromDatabase_isConvertedToDuplicateEmailException() {
        when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThrows(DuplicateEmailException.class, () -> userService.register(
                new RegisterRequest("Ankit", EMAIL, PASSWORD)));
    }

    private User existingUser() {
        return new User("Ankit", EMAIL, passwordEncoder.encode(PASSWORD));
    }

    @Test
    void verifyCredentials_correctPassword_returnsUserResponse() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser()));

        UserResponse response = userService.verifyCredentials(EMAIL, PASSWORD);

        assertEquals(EMAIL, response.email());
        assertEquals("Ankit", response.name());
    }

    @Test
    void verifyCredentials_normalizesEmailBeforeLookup() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser()));

        userService.verifyCredentials("  Ankit@Example.COM ", PASSWORD);

        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void verifyCredentials_wrongPassword_throwsInvalidCredentials() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser()));

        assertThrows(InvalidCredentialsException.class,
                () -> userService.verifyCredentials(EMAIL, "WrongPassword999"));
    }

    @Test
    void verifyCredentials_unknownEmail_throwsInvalidCredentials() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> userService.verifyCredentials("ghost@example.com", PASSWORD));
    }

    @Test
    void verifyCredentials_unknownEmailAndWrongPassword_giveTheExactSameMessage() {
        // Same message for both failure cases, so the API cannot be used to discover
        // which emails are registered (no user enumeration).
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser()));
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        InvalidCredentialsException wrongPassword = assertThrows(InvalidCredentialsException.class,
                () -> userService.verifyCredentials(EMAIL, "WrongPassword999"));
        InvalidCredentialsException unknownEmail = assertThrows(InvalidCredentialsException.class,
                () -> userService.verifyCredentials("ghost@example.com", PASSWORD));

        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
    }
}