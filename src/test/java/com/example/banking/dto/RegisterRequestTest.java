package com.example.banking.dto;


import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class RegisterRequestTest {

    @Test
    void toString_doesNotContainPassword() {
        RegisterRequest request = new RegisterRequest("Ankit", "ankit@example.com", "SecurePassword123");

        assertFalse(request.toString().contains("SecurePassword123"));
    }
}
