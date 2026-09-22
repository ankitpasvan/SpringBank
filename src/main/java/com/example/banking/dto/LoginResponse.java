package com.example.banking.dto;

/**
 * JSON returned by POST /api/auth/login on success: the JWT the client must send back
 * as "Authorization: Bearer {token}" on every later request, plus the logged-in user's
 * own safe profile (never the password/passwordHash).
 */
public record LoginResponse(String token, UserResponse user) {
}
