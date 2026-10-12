package com.example.banking.controller;

import java.security.Principal;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.dto.UpdateProfileRequest;
import com.example.banking.dto.UserResponse;
import com.example.banking.service.UserService;


@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getProfile(Principal principal) {
        return userService.getProfile(principal.getName());
    }

    @PutMapping("/me")
    public UserResponse updateProfile(Principal principal,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.getName(), request.name());
    }
}
