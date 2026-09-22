package com.example.banking.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.banking.dto.PageResponse;
import com.example.banking.dto.TransactionResponse;
import com.example.banking.model.TransactionType;
import com.example.banking.service.TransactionHistoryService;

@RestController
@RequestMapping("/api/accounts/{accountId}/transactions")
public class TransactionHistoryController {

    private final TransactionHistoryService transactionHistoryService;

    public TransactionHistoryController(TransactionHistoryService transactionHistoryService) {
        this.transactionHistoryService = transactionHistoryService;
    }

    @GetMapping
    public PageResponse<TransactionResponse> getHistory(
            @PathVariable("accountId") String accountId,
            @RequestParam(name = "type", required = false) TransactionType type,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return transactionHistoryService.getHistory(accountId, type, from, to, page, size);
    }
}
