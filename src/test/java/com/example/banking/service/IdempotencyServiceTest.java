package com.example.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.banking.exception.IdempotencyInProgressException;
import com.example.banking.model.IdempotencyRecord;
import com.example.banking.repository.IdempotencyRecordRepository;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final String USER_ID = "user-1";
    private static final String KEY = "key-abc";
    private static final String FINGERPRINT = "fp-abc";

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(idempotencyRecordRepository);
    }

    private IdempotencyRecord recordWithId(String id, String userId, String key, String fingerprint) {
        IdempotencyRecord record = new IdempotencyRecord(userId, key, fingerprint);
        ReflectionTestUtils.setField(record, "id", id);
        return record;
    }

    @Test
    void claim_firstTime_succeeds_returnsInProgressRecord() {
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(inv -> recordWithId("rec-1", USER_ID, KEY, FINGERPRINT));

        IdempotencyRecord result = idempotencyService.claim(USER_ID, KEY, FINGERPRINT);

        assertNotNull(result.getId());
        assertEquals(USER_ID, result.getUserId());
        assertEquals(KEY, result.getIdempotencyKey());
        assertEquals(FINGERPRINT, result.getRequestFingerprint());
        assertEquals(com.example.banking.model.IdempotencyStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    void claim_firstTime_storesRequestFingerprint() {
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        idempotencyService.claim(USER_ID, KEY, "fp-123");

        ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotencyRecordRepository).save(captor.capture());
        assertEquals("fp-123", captor.getValue().getRequestFingerprint());
    }

    @Test
    void claim_sameUserSameKey_whileInProgress_throwsIdempotencyInProgress() {
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenThrow(new DuplicateKeyException("dup"));
        IdempotencyRecord existingInProgress = recordWithId("rec-1", USER_ID, KEY, FINGERPRINT); // still IN_PROGRESS
        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(USER_ID, KEY))
                .thenReturn(Optional.of(existingInProgress));

        assertThrows(IdempotencyInProgressException.class,
                () -> idempotencyService.claim(USER_ID, KEY, FINGERPRINT));
    }

    @Test
    void claim_sameUserSameKey_afterCompleted_returnsCompletedRecordForReplay() {
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenThrow(new DuplicateKeyException("dup"));
        IdempotencyRecord completed = recordWithId("rec-1", USER_ID, KEY, FINGERPRINT);
        completed.markCompleted("tx-99", "acc-receiver");
        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(USER_ID, KEY))
                .thenReturn(Optional.of(completed));

        IdempotencyRecord result = idempotencyService.claim(USER_ID, KEY, FINGERPRINT);

        assertEquals(com.example.banking.model.IdempotencyStatus.COMPLETED, result.getStatus());
        assertEquals("tx-99", result.getResultTransactionId());
        assertEquals("acc-receiver", result.getToAccountId());
        assertEquals(FINGERPRINT, result.getRequestFingerprint());
    }

    @Test
    void claim_differentUser_sameKey_isAllowed() {
        // Each user's save succeeds independently - the unique index is scoped per user,
        // so this never throws DuplicateKeyException in the first place.
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(inv -> {
                    IdempotencyRecord arg = inv.getArgument(0);
                    return recordWithId("rec-" + arg.getUserId(), arg.getUserId(), arg.getIdempotencyKey(),
                            arg.getRequestFingerprint());
                });

        IdempotencyRecord first = idempotencyService.claim("user-1", KEY, FINGERPRINT);
        IdempotencyRecord second = idempotencyService.claim("user-2", KEY, FINGERPRINT);

        assertEquals("user-1", first.getUserId());
        assertEquals("user-2", second.getUserId());
        assertEquals(KEY, first.getIdempotencyKey());
        assertEquals(KEY, second.getIdempotencyKey());
    }

    @Test
    void claim_sameUser_differentKey_isAllowed() {
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(inv -> {
                    IdempotencyRecord arg = inv.getArgument(0);
                    return recordWithId("rec-" + arg.getIdempotencyKey(), arg.getUserId(), arg.getIdempotencyKey(),
                            arg.getRequestFingerprint());
                });

        IdempotencyRecord first = idempotencyService.claim(USER_ID, "key-a", FINGERPRINT);
        IdempotencyRecord second = idempotencyService.claim(USER_ID, "key-b", FINGERPRINT);

        assertEquals("key-a", first.getIdempotencyKey());
        assertEquals("key-b", second.getIdempotencyKey());
    }

    @Test
    void ensureFingerprint_setsFingerprintWhenAbsent() {
        IdempotencyRecord record = recordWithId("rec-1", USER_ID, KEY, null);
        when(idempotencyRecordRepository.findById("rec-1")).thenReturn(Optional.of(record));
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = idempotencyService.ensureFingerprint("rec-1", "fp-new");

        assertEquals("fp-new", result);
        assertEquals("fp-new", record.getRequestFingerprint());
        verify(idempotencyRecordRepository).save(record);
    }

    @Test
    void ensureFingerprint_keepsExistingFingerprint_neverOverwrites() {
        IdempotencyRecord record = recordWithId("rec-1", USER_ID, KEY, "fp-old");
        when(idempotencyRecordRepository.findById("rec-1")).thenReturn(Optional.of(record));

        String result = idempotencyService.ensureFingerprint("rec-1", "fp-new");

        assertEquals("fp-old", result);
        assertEquals("fp-old", record.getRequestFingerprint());
        verify(idempotencyRecordRepository, never()).save(any(IdempotencyRecord.class));
    }

    @Test
    void ensureFingerprint_recordNotFound_throwsIllegalState() {
        when(idempotencyRecordRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> idempotencyService.ensureFingerprint("missing", "fp-new"));

        verify(idempotencyRecordRepository, never()).save(any(IdempotencyRecord.class));
    }

    @Test
    void markCompleted_updatesStatusAndFields() {
        IdempotencyRecord record = recordWithId("rec-1", USER_ID, KEY, FINGERPRINT);
        when(idempotencyRecordRepository.findById("rec-1")).thenReturn(Optional.of(record));
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        idempotencyService.markCompleted("rec-1", "tx-1", "acc-2");

        assertEquals(com.example.banking.model.IdempotencyStatus.COMPLETED, record.getStatus());
        assertEquals("tx-1", record.getResultTransactionId());
        assertEquals("acc-2", record.getToAccountId());
        verify(idempotencyRecordRepository).save(record);
    }

    @Test
    void markCompleted_recordNotFound_throwsIllegalState() {
        when(idempotencyRecordRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> idempotencyService.markCompleted("missing", "tx-1", null));

        verify(idempotencyRecordRepository, never()).save(any(IdempotencyRecord.class));
    }

    @Test
    void release_deletesRecordById() {
        idempotencyService.release("rec-1");

        verify(idempotencyRecordRepository).deleteById("rec-1");
    }
}
