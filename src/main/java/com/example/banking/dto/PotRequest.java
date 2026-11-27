
package com.example.banking.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PotRequest(

        @NotBlank(message = "Pot name is required")
        @Size(max = 100, message = "Pot name must be at most 100 characters")
        String name,

        @NotBlank(message = "Currency is required")
        @Pattern(
                regexp = "INR|USD|EUR|GBP|AUD|CAD|SGD",
                message = "Unsupported currency"
        )
        String currency,

        @NotNull(message = "Target amount is required")
        @DecimalMin(
                value = "0.00",
                message = "Target amount must be zero or more"
        )
        BigDecimal targetAmount,

        @Size(max = 50, message = "Icon must be at most 50 characters")
        String icon

) {
}
