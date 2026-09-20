package com.example.banking.dto;



import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Incoming JSON for POST /api/auth/register.
 */
public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        // max 72 because BCrypt only uses the first 72 bytes of a password
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$",
                message = "Password must contain at least one letter and one digit")
        String password) {

    // Records auto-generate toString() with ALL fields. Override it so the
    // password can never leak into logs by accident.
    @Override
    public String toString() {
        return "RegisterRequest[name=" + name + ", email=" + email + ", password=***]";
    }
}
