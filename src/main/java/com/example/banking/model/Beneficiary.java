package com.example.banking.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "beneficiaries", indexes = {
        @Index(name = "beneficiaries_user_idx", columnList = "userId")
})
public class Beneficiary {

    @Id
    @UuidGenerator
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(nullable = false, length = 100)
    private String legalName;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 34)
    private String accountNumber;

    @Column(length = 11)
    private String ifsc;

    @Column(length = 100)
    private String bankName;

    @Column(length = 100)
    private String wiseTag;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Beneficiary() {
    }

    public Beneficiary(String userId, String nickname, String legalName, String currency,
                       String country, String accountNumber, String ifsc, String bankName, String wiseTag) {
        this.userId = userId;
        this.nickname = nickname;
        this.legalName = legalName;
        this.currency = currency;
        this.country = country;
        this.accountNumber = accountNumber;
        this.ifsc = ifsc;
        this.bankName = bankName;
        this.wiseTag = wiseTag;
        this.verified = false;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getLegalName() { return legalName; }
    public String getCurrency() { return currency; }
    public String getCountry() { return country; }
    public String getAccountNumber() { return accountNumber; }
    public String getIfsc() { return ifsc; }
    public String getBankName() { return bankName; }
    public String getWiseTag() { return wiseTag; }
    public boolean isVerified() { return verified; }
    public Instant getCreatedAt() { return createdAt; }

    public void setVerified(boolean verified) { this.verified = verified; }
}