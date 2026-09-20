package com.example.banking.controller;

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
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request.userId(), request.accountType());
    }

    @GetMapping("/{id}")
    public AccountResponse getAccountById(@PathVariable("id") String id) {
        return accountService.getById(id);
    }

    @GetMapping
    public List<AccountResponse> getAccountsByUserId(@RequestParam("userId") String userId) {
        return accountService.getByUserId(userId);
    }
}
