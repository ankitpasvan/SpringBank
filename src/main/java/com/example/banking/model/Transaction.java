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
 * One row of the account statement, stored in the "transactions" table.
 * A transaction is never updated or deleted after it is written (it is a ledger entry).
 */
@Entity
@Table(name = "transactions", indexes = {
        // Serves the history query: "all transactions of one account, newest first".
        @Index(name = "account_created_idx", columnList = "accountId, createdAt")
})
public class Transaction {

    @Id
    @UuidGenerator
    private String id;

    @Column(nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // balance of the account right after this transaction was applied
    @Column(precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected Transaction() {
    }

    public Transaction(String accountId, TransactionType type, BigDecimal amount,
                       BigDecimal balanceAfter, String description) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
