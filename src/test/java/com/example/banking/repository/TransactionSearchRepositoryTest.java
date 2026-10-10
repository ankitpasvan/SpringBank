package com.example.banking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.banking.model.Transaction;
import com.example.banking.model.TransactionType;

// Real persistence tests against an in-memory database: they prove the filters, date ranges,
// sorting and pagination actually work, instead of only checking what query object we built.
@DataJpaTest
@Import(TransactionSearchRepository.class)
class TransactionSearchRepositoryTest {

    private static final String ACCOUNT_ID = "acc-1";
    private static final String OTHER_ACCOUNT_ID = "acc-2";

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionSearchRepository repository;

    private Transaction tx(String accountId, TransactionType type, String createdAt) {
        Transaction transaction = new Transaction(accountId, type, new BigDecimal("10.00"),
                new BigDecimal("10.00"), null);
        ReflectionTestUtils.setField(transaction, "createdAt", Instant.parse(createdAt));
        return transactionRepository.save(transaction);
    }

    @BeforeEach
    void setUp() {
        tx(ACCOUNT_ID, TransactionType.DEPOSIT, "2026-01-10T10:00:00Z");
        tx(ACCOUNT_ID, TransactionType.WITHDRAWAL, "2026-01-20T10:00:00Z");
        tx(ACCOUNT_ID, TransactionType.DEPOSIT, "2026-02-10T10:00:00Z");
        tx(OTHER_ACCOUNT_ID, TransactionType.DEPOSIT, "2026-01-15T10:00:00Z");
    }

    private Pageable newestFirst(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    @Test
    void search_noFilters_returnsOnlyThisAccountsTransactionsNewestFirst() {
        Page<Transaction> page = repository.search(ACCOUNT_ID, null, null, null, newestFirst(0, 20));

        assertEquals(3, page.getTotalElements());
        List<Instant> dates = page.getContent().stream().map(Transaction::getCreatedAt).toList();
        assertEquals(List.of(Instant.parse("2026-02-10T10:00:00Z"), Instant.parse("2026-01-20T10:00:00Z"),
                Instant.parse("2026-01-10T10:00:00Z")), dates);
    }

    @Test
    void search_withTypeFilter_returnsOnlyMatchingType() {
        Page<Transaction> page =
                repository.search(ACCOUNT_ID, TransactionType.DEPOSIT, null, null, newestFirst(0, 20));

        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(t -> t.getType() == TransactionType.DEPOSIT));
    }

    @Test
    void search_withDateRange_appliesInclusiveFromAndExclusiveTo() {
        Instant from = Instant.parse("2026-01-15T00:00:00Z");
        Instant toExclusive = Instant.parse("2026-02-01T00:00:00Z");

        Page<Transaction> page = repository.search(ACCOUNT_ID, null, from, toExclusive, newestFirst(0, 20));

        assertEquals(1, page.getTotalElements());
        assertEquals(TransactionType.WITHDRAWAL, page.getContent().get(0).getType());
    }

    @Test
    void search_pagination_returnsCorrectPageAndTotals() {
        Page<Transaction> first = repository.search(ACCOUNT_ID, null, null, null, newestFirst(0, 2));
        Page<Transaction> second = repository.search(ACCOUNT_ID, null, null, null, newestFirst(1, 2));

        assertEquals(3, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertEquals(2, first.getContent().size());
        assertEquals(1, second.getContent().size());
        // no overlap between pages
        assertTrue(first.getContent().stream()
                .noneMatch(t -> t.getId().equals(second.getContent().get(0).getId())));
    }
}
