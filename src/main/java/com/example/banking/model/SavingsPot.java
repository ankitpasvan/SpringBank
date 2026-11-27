package com.example.banking.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "savings_pots", indexes = {
        @Index(name = "savings_pots_user_idx", columnList = "userId")
})
public class SavingsPot {

    @Id
    @UuidGenerator
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(length = 50)
    private String icon;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected SavingsPot() {
    }

    public SavingsPot(String userId, String name, String currency, BigDecimal targetAmount, String icon) {
        this.userId = userId;
        this.name = name;
        this.currency = currency;
        this.targetAmount = targetAmount;
        this.balance = BigDecimal.ZERO;
        this.icon = icon;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getCurrency() { return currency; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public BigDecimal getBalance() { return balance; }
    public String getIcon() { return icon; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}