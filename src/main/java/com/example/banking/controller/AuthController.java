package com.example.banking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.dto.LoginRequest;
import com.example.banking.dto.LoginResponse;
import com.example.banking.dto.RegisterRequest;
import com.example.banking.dto.UserResponse;
import com.example.banking.security.JwtService;
import com.example.banking.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // UserService.verifyCredentials throws InvalidCredentialsException (-> 401, see
        // GlobalExceptionHandler) for BOTH an unknown email and a wrong password, with the
        // exact same message, so this endpoint never reveals which emails are registered.
        UserResponse user = userService.verifyCredentials(request.email(), request.password());
        String token = jwtService.generateToken(user.id(), user.email());
        return new LoginResponse(token, user);
    }
}