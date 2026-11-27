package com.example.banking.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.dto.PotRequest;
import com.example.banking.dto.PotResponse;
import com.example.banking.exception.InvalidRequestException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.SavingsPot;
import com.example.banking.repository.AccountBalanceRepository;
import com.example.banking.repository.SavingsPotRepository;

@Service
public class SavingsPotService {

    private final SavingsPotRepository potRepository;
    private final AccountBalanceRepository accountBalanceRepository;

    public SavingsPotService(SavingsPotRepository potRepository,
                             AccountBalanceRepository accountBalanceRepository) {
        this.potRepository = potRepository;
        this.accountBalanceRepository = accountBalanceRepository;
    }

    public List<PotResponse> list(String userId) {
        return potRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PotResponse::from)
                .toList();
    }

    @Transactional
    public PotResponse create(String userId, PotRequest request) {
        SavingsPot saved = potRepository.save(new SavingsPot(
                userId,
                request.name().trim(),
                request.currency(),
                request.targetAmount(),
                request.icon() == null || request.icon().isBlank() ? "target" : request.icon().trim()));
        return PotResponse.from(saved);
    }

    @Transactional
    public PotResponse addMoney(String userId, String potId, String accountId, BigDecimal amount) {
        validateAmount(amount);
        SavingsPot pot = potRepository.findByIdAndUserId(potId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings pot not found with id: " + potId));
        accountBalanceRepository.withdraw(accountId, userId, amount)
                .orElseThrow(() -> new InvalidRequestException("Insufficient funds or account not found"));
        pot.setBalance(pot.getBalance().add(amount));
        pot.setUpdatedAt(Instant.now());
        return PotResponse.from(potRepository.save(pot));
    }

    @Transactional
    public PotResponse withdrawMoney(String userId, String potId, String accountId, BigDecimal amount) {
        validateAmount(amount);
        SavingsPot pot = potRepository.findByIdAndUserId(potId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings pot not found with id: " + potId));
        if (pot.getBalance().compareTo(amount) < 0) {
            throw new InvalidRequestException("Pot balance is less than the requested amount");
        }
        accountBalanceRepository.deposit(accountId, userId, amount)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        pot.setBalance(pot.getBalance().subtract(amount));
        pot.setUpdatedAt(Instant.now());
        return PotResponse.from(potRepository.save(pot));
    }

    @Transactional
    public void delete(String userId, String potId) {
        SavingsPot pot = potRepository.findByIdAndUserId(potId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings pot not found with id: " + potId));
        if (pot.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidRequestException("Withdraw all money from the pot before deleting it");
        }
        potRepository.delete(pot);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Amount must be greater than zero");
        }
    }
}