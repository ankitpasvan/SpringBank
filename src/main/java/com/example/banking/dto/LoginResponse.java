package com.example.banking.dto;


public record LoginResponse(String token, UserResponse user) {
}
