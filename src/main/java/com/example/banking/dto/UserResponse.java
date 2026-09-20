package com.example.banking.dto;



import java.time.Instant;

import com.example.banking.model.User;

/**
 * Safe JSON returned to the client. It has NO password / passwordHash field,
 * so it is impossible to leak it by mistake.
 */
public record UserResponse(String id, String name, String email, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
