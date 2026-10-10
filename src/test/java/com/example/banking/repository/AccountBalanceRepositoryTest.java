package com.example.banking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.banking.model.Account;
import com.example.banking.model.AccountType;

// Real persistence tests against an in-memory database: they prove the balance changes
// are atomic and the ownership/balance guards actually hold, instead of only checking
// what query object we built (as the old MongoTemplate-mock tests did).
@DataJpaTest
@Import(AccountBalanceRepository.class)
class AccountBalanceRepositoryTest {

    private static final String USER_ID = "user-1";
    private static final String OTHER_USER_ID = "user-2";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountBalanceRepository repository;

    private String accountId;

    @BeforeEach
    void setUp() {
        Account account = accountRepository.save(
                new Account("123456789012", USER_ID, AccountType.SAVINGS, new BigDecimal("1000.00")));
        accountId = account.getId();
    }

    @Test
    void deposit_addsAmountAndReturnsUpdatedAccount() {
        Optional<Account> result = repository.deposit(accountId, USER_ID, new BigDecimal("100.00"));

        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("1100.00").compareTo(result.get().getBalance()));
        // the stored row agrees with the returned one
        assertEquals(0, new BigDecimal("1100.00")
                .compareTo(accountRepository.findById(accountId).orElseThrow().getBalance()));
    }

    @Test
    void deposit_wrongOwner_returnsEmptyAndBalanceUnchanged() {
        assertTrue(repository.deposit(accountId, OTHER_USER_ID, new BigDecimal("100.00")).isEmpty());
        assertEquals(0, new BigDecimal("1000.00")
                .compareTo(accountRepository.findById(accountId).orElseThrow().getBalance()));
    }

    @Test
    void deposit_missingAccount_returnsEmpty() {
        assertTrue(repository.deposit("no-such-id", USER_ID, new BigDecimal("100.00")).isEmpty());
    }

    @Test
    void withdraw_subtractsWhenBalanceSufficient() {
        Optional<Account> result = repository.withdraw(accountId, USER_ID, new BigDecimal("100.00"));

        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("900.00").compareTo(result.get().getBalance()));
    }

    @Test
    void withdraw_insufficientFunds_returnsEmptyAndBalanceUnchanged() {
        assertTrue(repository.withdraw(accountId, USER_ID, new BigDecimal("1000.01")).isEmpty());
        assertEquals(0, new BigDecimal("1000.00")
                .compareTo(accountRepository.findById(accountId).orElseThrow().getBalance()));
    }

    @Test
    void withdraw_exactBalance_succeedsAndLeavesZero() {
        Optional<Account> result = repository.withdraw(accountId, USER_ID, new BigDecimal("1000.00"));

        assertTrue(result.isPresent());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get().getBalance()));
    }

    @Test
    void withdraw_wrongOwner_returnsEmptyAndBalanceUnchanged() {
        assertTrue(repository.withdraw(accountId, OTHER_USER_ID, new BigDecimal("100.00")).isEmpty());
        assertEquals(0, new BigDecimal("1000.00")
                .compareTo(accountRepository.findById(accountId).orElseThrow().getBalance()));
    }
}
