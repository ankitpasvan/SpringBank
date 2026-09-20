package com.example.banking.controller;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.dto.TransactionRequest;
import com.example.banking.dto.TransactionResponse;
import com.example.banking.service.TransactionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts/{accountId}")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(@PathVariable("accountId") String accountId,
                                       @Valid @RequestBody TransactionRequest request) {
        return transactionService.deposit(accountId, request.amount(), request.description());
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse withdraw(@PathVariable("accountId") String accountId,
                                        @Valid @RequestBody TransactionRequest request) {
        return transactionService.withdraw(accountId, request.amount(), request.description());
    }
}
