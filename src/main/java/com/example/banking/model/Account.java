package com.example.banking.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

/**
 * Bank account, stored in the "accounts" table.
 * The balance is a BigDecimal (never double) because money needs exact decimal math.
 */
@Entity
@Table(name = "accounts", indexes = {
        // fast lookup of all accounts of one user
        @Index(name = "accounts_user_idx", columnList = "userId")
})
public class Account {

    @Id
    @UuidGenerator
    private String id;

    // unique constraint = database-level safety net against duplicate account numbers
    @Column(unique = true, nullable = false, length = 12)
    private String accountNumber;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    /** Required by JPA. */
    protected Account() {
    }

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

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
