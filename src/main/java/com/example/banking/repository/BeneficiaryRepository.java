package com.example.banking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.model.Beneficiary;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, String> {

    List<Beneficiary> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Beneficiary> findByIdAndUserId(String id, String userId);

    boolean existsByUserIdAndAccountNumberAndCurrency(String userId, String accountNumber, String currency);
}