package com.example.banking.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.dto.AccountResponse;
import com.example.banking.dto.CreateAccountRequest;
import com.example.banking.service.AccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(Principal principal, @Valid @RequestBody CreateAccountRequest request) {
        // The owner is the authenticated caller (JwtAuthenticationFilter sets this as the
        // token's userId), never a value the client could supply - see CreateAccountRequest.
        return accountService.createAccount(principal.getName(), request.accountType());
    }

    @GetMapping("/{id}")
    public AccountResponse getAccountById(Principal principal, @PathVariable("id") String id) {
        return accountService.getById(id, principal.getName());
    }

    @GetMapping
    public List<AccountResponse> getAccountsByUserId(@RequestParam("userId") String userId) {
        return accountService.getByUserId(userId);
    }
}