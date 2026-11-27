package com.example.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BeneficiaryRequest(
        @NotBlank(message = "Nickname is required")
        @Size(max = 100, message = "Nickname must be at most 100 characters")
        String nickname,

        @NotBlank(message = "Legal name is required")
        @Size(max = 100, message = "Legal name must be at most 100 characters")
        String legalName,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "INR|USD|EUR|GBP|AUD|CAD|SGD", message = "Unsupported currency")
        String currency,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must be at most 100 characters")
        String country,

        @NotBlank(message = "Account number is required")
        @Size(min = 9, max = 34, message = "Account number must be 9 to 34 characters")
        String accountNumber,

        @Pattern(regexp = "^$|^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC must look like SBIN0123456")
        String ifsc,

        @Size(max = 100, message = "Bank name must be at most 100 characters")
        String bankName,

        @Size(max = 100, message = "Wisetag must be at most 100 characters")
        String wiseTag
) {
}