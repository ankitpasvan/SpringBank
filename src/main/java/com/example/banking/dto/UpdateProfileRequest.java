package com.example.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of PUT /api/users/me. Only the display name can be changed through
 * the basic profile endpoint.
 */
public record UpdateProfileRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name) {
}
