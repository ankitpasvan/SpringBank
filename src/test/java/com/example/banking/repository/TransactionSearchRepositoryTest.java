package com.example.banking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;

// Checks WHAT filter we send to MongoDB (via MongoTemplate). Does not talk to a real database.
//
// NOTE on the type-filter test below: Query.getQueryObject() returns the raw, UNCONVERTED
// Criteria contents. Spring Data MongoDB only converts a Java enum to its BSON String form
// (via MappingMongoConverter) at actual query EXECUTION time, inside the real MongoTemplate.
// Since MongoTemplate is mocked here, that conversion never runs, so the captured filter
// still holds the raw TransactionType enum value, not the String "DEPOSIT".
@ExtendWith(MockitoExtension.class)
class TransactionSearchRepositoryTest {

    private static final String ACCOUNT_ID = "acc-1";

    @Mock
    private MongoTemplate mongoTemplate;

    private TransactionSearchRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TransactionSearchRepository(mongoTemplate);
    }

    private Query captureCountFilter() {
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(captor.capture(), eq(Transaction.class));
        return captor.getValue();
    }

    @Test
    void search_noFilters_onlyFiltersOnAccountId() {
        when(mongoTemplate.count(any(Query.class), eq(Transaction.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Transaction.class))).thenReturn(List.of());
        Pageable pageable = PageRequest.of(0, 20);

        repository.search(ACCOUNT_ID, null, null, null, pageable);

        Query filter = captureCountFilter();
        assertEquals(ACCOUNT_ID, filter.getQueryObject().get("accountId"));
        assertEquals(1, filter.getQueryObject().keySet().size(), "no extra filter fields expected");
    }

    @Test
    void search_withTypeFilter_addsTypeToQuery() {
        when(mongoTemplate.count(any(Query.class), eq(Transaction.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Transaction.class))).thenReturn(List.of());
        Pageable pageable = PageRequest.of(0, 20);

        repository.search(ACCOUNT_ID, TransactionType.DEPOSIT, null, null, pageable);

        Query filter = captureCountFilter();
        assertEquals(TransactionType.DEPOSIT, filter.getQueryObject().get("type"));
    }

    @Test
    void search_withDateRange_addsGteAndLtOnCreatedAt() {
        when(mongoTemplate.count(any(Query.class), eq(Transaction.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Transaction.class))).thenReturn(List.of());
        Pageable pageable = PageRequest.of(0, 20);
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");

        repository.search(ACCOUNT_ID, null, from, to, pageable);

        Query filter = captureCountFilter();
        Object createdAt = filter.getQueryObject().get("createdAt");
        assertEquals(true, createdAt instanceof org.bson.Document);
        org.bson.Document range = (org.bson.Document) createdAt;
        assertEquals(from, range.get("$gte"));
        assertEquals(to, range.get("$lt"));
    }

    @Test
    void search_returnsPageWithCorrectTotals() {
        when(mongoTemplate.count(any(Query.class), eq(Transaction.class))).thenReturn(45L);
        when(mongoTemplate.find(any(Query.class), eq(Transaction.class))).thenReturn(List.of(
                new Transaction(ACCOUNT_ID, TransactionType.DEPOSIT, new java.math.BigDecimal("10.00"),
                        new java.math.BigDecimal("10.00"), null)));
        Pageable pageable = PageRequest.of(0, 20);

        Page<Transaction> page = repository.search(ACCOUNT_ID, null, null, null, pageable);

        assertEquals(45L, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        assertEquals(1, page.getContent().size());
    }
}