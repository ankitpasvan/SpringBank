package com.example.banking.dto;

import java.time.Instant;

import com.example.banking.model.Beneficiary;

public record BeneficiaryResponse(
        String id,
        String nickname,
        String legalName,
        String currency,
        String country,
        String accountNumber,
        String ifsc,
        String bankName,
        String wiseTag,
        boolean verified,
        Instant createdAt
) {

    public static BeneficiaryResponse from(Beneficiary b) {
        return new BeneficiaryResponse(
                b.getId(), b.getNickname(), b.getLegalName(), b.getCurrency(), b.getCountry(),
                b.getAccountNumber(), b.getIfsc(), b.getBankName(), b.getWiseTag(),
                b.isVerified(), b.getCreatedAt());
    }
}