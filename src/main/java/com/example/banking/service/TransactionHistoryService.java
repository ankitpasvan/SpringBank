package com.example.banking.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.banking.dto.PageResponse;
import com.example.banking.dto.TransactionResponse;
import com.example.banking.exception.InvalidRequestException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionSearchRepository;

@Service
public class TransactionHistoryService {

    static final int MAX_PAGE_SIZE = 100;

    private final AccountRepository accountRepository;
    private final TransactionSearchRepository transactionSearchRepository;

    public TransactionHistoryService(AccountRepository accountRepository,
                                     TransactionSearchRepository transactionSearchRepository) {
        this.accountRepository = accountRepository;
        this.transactionSearchRepository = transactionSearchRepository;
    }

    /**
     * Newest transactions first. Dates are calendar days in UTC: "from" is inclusive from 00:00,
     * "to" is inclusive until the end of that day.
     */
    public PageResponse<TransactionResponse> getHistory(String accountId, TransactionType type,
                                                        LocalDate from, LocalDate to, int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException("Page index must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("'from' date must not be after 'to' date");
        }
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account not found with id: " + accountId);
        }

        Instant fromInstant = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toExclusive = to == null ? null : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // id is a tie-breaker: two transactions in the same millisecond still have a stable order
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<Transaction> result = transactionSearchRepository.search(accountId, type, fromInstant,
                toExclusive, pageable);

        return PageResponse.from(result.map(TransactionResponse::from));
    }
}
