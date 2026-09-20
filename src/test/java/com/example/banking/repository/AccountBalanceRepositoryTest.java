package com.example.banking.repository;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import com.example.banking.model.Account;
import com.example.banking.model.AccountType;

// Checks WHAT we ask MongoDB to do (filter + update). It does not talk to a real database.
@ExtendWith(MockitoExtension.class)
class AccountBalanceRepositoryTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private AccountBalanceRepository repository;

    private final Account updated =
            new Account("123456789012", "user-1", AccountType.SAVINGS, new BigDecimal("900.00"));

    @BeforeEach
    void setUp() {
        repository = new AccountBalanceRepository(mongoTemplate);
    }

    private void stubTemplate(Account result) {
        when(mongoTemplate.findAndModify(any(Query.class), any(UpdateDefinition.class),
                any(FindAndModifyOptions.class), eq(Account.class))).thenReturn(result);
    }

    private Document capturedFilter;
    private Document capturedUpdate;

    private void captureRequest() {
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<UpdateDefinition> updateCaptor = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongoTemplate).findAndModify(queryCaptor.capture(), updateCaptor.capture(),
                any(FindAndModifyOptions.class), eq(Account.class));
        capturedFilter = queryCaptor.getValue().getQueryObject();
        capturedUpdate = updateCaptor.getValue().getUpdateObject();
    }

    @Test
    void withdraw_filtersOnSufficientBalance_andDecrementsAtomically() {
        stubTemplate(updated);

        Optional<Account> result = repository.withdraw("acc-1", new BigDecimal("100.00"));

        assertTrue(result.isPresent());
        captureRequest();
        assertEquals("acc-1", capturedFilter.get("id"));
        Map<?, ?> balanceCondition = (Map<?, ?>) capturedFilter.get("balance");
        assertNotNull(balanceCondition, "withdraw must guard with balance >= amount");
        assertEquals(new BigDecimal("100.00"), balanceCondition.get("$gte"));
        Map<?, ?> inc = (Map<?, ?>) capturedUpdate.get("$inc");
        assertEquals(new BigDecimal("-100.00"), inc.get("balance"));
    }

    @Test
    void deposit_hasNoBalanceCondition_andIncrementsAtomically() {
        stubTemplate(updated);

        Optional<Account> result = repository.deposit("acc-1", new BigDecimal("100.00"));

        assertTrue(result.isPresent());
        captureRequest();
        assertEquals("acc-1", capturedFilter.get("id"));
        assertFalse(capturedFilter.containsKey("balance"));
        Map<?, ?> inc = (Map<?, ?>) capturedUpdate.get("$inc");
        assertEquals(new BigDecimal("100.00"), inc.get("balance"));
    }

    @Test
    void withdraw_nothingMatched_returnsEmpty() {
        stubTemplate(null);

        assertTrue(repository.withdraw("acc-1", new BigDecimal("100.00")).isEmpty());
    }
}
