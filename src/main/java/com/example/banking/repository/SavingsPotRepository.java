package com.example.banking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.model.SavingsPot;

public interface SavingsPotRepository extends JpaRepository<SavingsPot, String> {
    List<SavingsPot> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<SavingsPot> findByIdAndUserId(String id, String userId);
}