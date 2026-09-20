package com.example.banking.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document stored in the "accounts" collection.
 * The balance is a BigDecimal (never double) because money needs exact decimal math.
 */
@Document(collection = "accounts")
public class Account {

    @Id
    private String id;

    // unique index = database-level safety net against duplicate account numbers
    @Indexed(unique = true)
    private String accountNumber;

    // normal index = fast lookup of all accounts of one user
    @Indexed
    private String userId;

    private AccountType accountType;

    private BigDecimal balance;

    private Instant createdAt;

    private Instant updatedAt;

    public Account(String accountNumber, String userId, AccountType accountType, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.userId = userId;
        this.accountType = accountType;
        this.balance = balance;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getUserId() {
        return userId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
