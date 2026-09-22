package com.example.banking.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One row of the account statement, stored in the "transactions" collection.
 * A transaction is never updated or deleted after it is written (it is a ledger entry).
 */
@Document(collection = "transactions")
// Serves the history query: "all transactions of one account, newest first".
@CompoundIndex(name = "account_created_idx", def = "{'accountId': 1, 'createdAt': -1}")
public class Transaction {

    @Id
    private String id;

    // normal index = fast lookup of the history of one account
    @Indexed
    private String accountId;

    private TransactionType type;

    private BigDecimal amount;

    // balance of the account right after this transaction was applied
    private BigDecimal balanceAfter;

    private String description;

    private Instant createdAt;

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