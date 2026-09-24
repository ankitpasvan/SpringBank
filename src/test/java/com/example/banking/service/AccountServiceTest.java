package com.example.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.banking.dto.AccountResponse;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.model.Account;
import com.example.banking.model.AccountType;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.util.AccountNumberGenerator;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final String USER_ID = "user-1";
    private static final String ACCOUNT_NUMBER = "123456789012";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, userRepository, accountNumberGenerator);
    }

    @Test
    void createAccount_success_savesAccountAndReturnsResponse() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(accountNumberGenerator.generate()).thenReturn(ACCOUNT_NUMBER);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.createAccount(USER_ID, AccountType.SAVINGS);

        assertEquals(ACCOUNT_NUMBER, response.accountNumber());
        assertEquals(USER_ID, response.userId());
        assertEquals(AccountType.SAVINGS, response.accountType());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertEquals(AccountType.SAVINGS, captor.getValue().getAccountType());
    }

    @Test
    void createAccount_userNotFound_throwsAndDoesNotSave() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> accountService.createAccount(USER_ID, AccountType.CURRENT));

        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(accountNumberGenerator);
    }

    @Test
    void createAccount_generatedAccountNumber_has12DigitsAndNoLeadingZero() {
        // Real generator on purpose: we want to check the real number format end to end.
        AccountService serviceWithRealGenerator =
                new AccountService(accountRepository, userRepository, new AccountNumberGenerator());
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = serviceWithRealGenerator.createAccount(USER_ID, AccountType.SAVINGS);

        assertTrue(response.accountNumber().matches("[1-9][0-9]{11}"),
                "Expected 12 digits, no leading zero, but was: " + response.accountNumber());
    }

    @Test
    void createAccount_initialBalanceIsZero() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(accountNumberGenerator.generate()).thenReturn(ACCOUNT_NUMBER);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.createAccount(USER_ID, AccountType.SAVINGS);

        assertEquals(0, BigDecimal.ZERO.compareTo(response.balance()));
    }

    @Test
    void createAccount_accountNumberCollision_generatesNewNumber() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(accountNumberGenerator.generate()).thenReturn("111111111111", "222222222222");
        when(accountRepository.existsByAccountNumber("111111111111")).thenReturn(true);
        when(accountRepository.existsByAccountNumber("222222222222")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.createAccount(USER_ID, AccountType.SAVINGS);

        assertEquals("222222222222", response.accountNumber());
        verify(accountNumberGenerator, times(2)).generate();
    }

    @Test
    void createAccount_duplicateKeyOnSave_retriesWithNewNumber() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(accountNumberGenerator.generate()).thenReturn("111111111111", "222222222222");
        when(accountRepository.save(any(Account.class)))
                .thenThrow(new DuplicateKeyException("dup"))
                .thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.createAccount(USER_ID, AccountType.SAVINGS);

        assertEquals("222222222222", response.accountNumber());
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void createAccount_everyNumberCollides_throwsIllegalState() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(accountNumberGenerator.generate()).thenReturn(ACCOUNT_NUMBER);
        when(accountRepository.existsByAccountNumber(ACCOUNT_NUMBER)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> accountService.createAccount(USER_ID, AccountType.SAVINGS));

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getById_ownerMatches_returnsAccount() {
        Account account = new Account(ACCOUNT_NUMBER, USER_ID, AccountType.CURRENT, new BigDecimal("0.00"));
        ReflectionTestUtils.setField(account, "id", "acc-1");
        when(accountRepository.findById("acc-1")).thenReturn(java.util.Optional.of(account));

        AccountResponse response = accountService.getById("acc-1", USER_ID);

        assertEquals("acc-1", response.id());
        assertEquals(ACCOUNT_NUMBER, response.accountNumber());
        assertEquals(AccountType.CURRENT, response.accountType());
    }

    @Test
    void getById_notFound_throwsResourceNotFound() {
        when(accountRepository.findById("missing")).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getById("missing", USER_ID));
    }

    @Test
    void getById_differentOwner_throwsResourceNotFound() {
        // Account exists, but belongs to someone else - must look identical to "not found",
        // never a distinct error, so account ids can't be enumerated by their HTTP status.
        Account account = new Account(ACCOUNT_NUMBER, "someone-else", AccountType.CURRENT, new BigDecimal("0.00"));
        ReflectionTestUtils.setField(account, "id", "acc-1");
        when(accountRepository.findById("acc-1")).thenReturn(java.util.Optional.of(account));

        assertThrows(ResourceNotFoundException.class, () -> accountService.getById("acc-1", USER_ID));
    }

    @Test
    void getByUserId_returnsAllAccountsOfUser() {
        when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(
                new Account("111111111111", USER_ID, AccountType.SAVINGS, new BigDecimal("0.00")),
                new Account("222222222222", USER_ID, AccountType.CURRENT, new BigDecimal("0.00"))));

        List<AccountResponse> responses = accountService.getByUserId(USER_ID);

        assertEquals(2, responses.size());
        assertEquals("111111111111", responses.get(0).accountNumber());
        assertEquals("222222222222", responses.get(1).accountNumber());
    }

    @Test
    void getByUserId_noAccounts_returnsEmptyList() {
        when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of());

        assertTrue(accountService.getByUserId(USER_ID).isEmpty());
    }
}