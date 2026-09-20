
package com.example.banking.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.example.banking.dto.AccountResponse;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.Account;
import com.example.banking.model.AccountType;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.util.AccountNumberGenerator;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("0.00");
    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 5;

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository,
                          AccountNumberGenerator accountNumberGenerator) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountNumberGenerator = accountNumberGenerator;
    }

    public AccountResponse createAccount(String userId, AccountType accountType) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        for (int attempt = 1; attempt <= MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            String accountNumber = accountNumberGenerator.generate();

            if (accountRepository.existsByAccountNumber(accountNumber)) {
                log.warn("Account number collision on attempt {}, generating a new one", attempt);
                continue;
            }

            try {
                Account saved = accountRepository.save(
                        new Account(accountNumber, userId, accountType, INITIAL_BALANCE));
                log.info("Account created with id={}", saved.getId());
                return AccountResponse.from(saved);
            } catch (DuplicateKeyException ex) {
                // Another request took the same number between our check and our save;
                // the unique index caught it, so we simply try a new number.
                log.warn("Account number collision at save on attempt {}, retrying", attempt);
            }
        }

        throw new IllegalStateException("Could not generate a unique account number");
    }

    public AccountResponse getById(String id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
    }

    public List<AccountResponse> getByUserId(String userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(AccountResponse::from)
                .toList();
    }
}