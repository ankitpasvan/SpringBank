package com.example.banking.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.dto.BeneficiaryRequest;
import com.example.banking.dto.BeneficiaryResponse;
import com.example.banking.exception.InvalidRequestException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.Beneficiary;
import com.example.banking.repository.BeneficiaryRepository;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
    }

    public List<BeneficiaryResponse> list(String userId) {
        return beneficiaryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(BeneficiaryResponse::from)
                .toList();
    }

    @Transactional
    public BeneficiaryResponse create(String userId, BeneficiaryRequest request) {
        if (beneficiaryRepository.existsByUserIdAndAccountNumberAndCurrency(
                userId, request.accountNumber().trim(), request.currency())) {
            throw new InvalidRequestException("This beneficiary already exists");
        }
        String ifsc = request.ifsc() == null || request.ifsc().isBlank() ? null : request.ifsc().trim().toUpperCase();
        Beneficiary saved = beneficiaryRepository.save(new Beneficiary(
                userId,
                request.nickname().trim(),
                request.legalName().trim(),
                request.currency(),
                request.country().trim(),
                request.accountNumber().trim(),
                ifsc,
                request.bankName() == null ? null : request.bankName().trim(),
                request.wiseTag() == null ? null : request.wiseTag().trim()));
        return BeneficiaryResponse.from(saved);
    }

    @Transactional
    public BeneficiaryResponse verify(String userId, String id) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found with id: " + id));
        // Mock verification (sandbox): Indian IFSC format or a 9+ digit account number passes.
        boolean ifscOk = beneficiary.getIfsc() != null && beneficiary.getIfsc().matches("^[A-Z]{4}0[A-Z0-9]{6}$");
        boolean accountOk = beneficiary.getAccountNumber() != null && beneficiary.getAccountNumber().length() >= 9;
        if (!ifscOk && !accountOk) {
            throw new InvalidRequestException("Beneficiary could not be verified with the given details");
        }
        beneficiary.setVerified(true);
        return BeneficiaryResponse.from(beneficiaryRepository.save(beneficiary));
    }

    @Transactional
    public void delete(String userId, String id) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found with id: " + id));
        beneficiaryRepository.delete(beneficiary);
    }
}