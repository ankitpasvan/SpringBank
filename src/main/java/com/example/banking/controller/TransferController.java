package com.example.banking.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.dto.TransferRequest;
import com.example.banking.dto.TransferResponse;
import com.example.banking.service.TransferService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts/{accountId}")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(Principal principal, @PathVariable("accountId") String accountId,
                                     @Valid @RequestBody TransferRequest request) {
        return transferService.transfer(accountId, principal.getName(), request.toAccountId(), request.amount(),
                request.description());
    }
}
