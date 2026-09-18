package com.fta.reconciler;

import com.fta.reconciler.audit.AuditLog;
import com.fta.reconciler.ledger.DuplicateEntryException;
import com.fta.reconciler.ledger.LedgerEntry;
import com.fta.reconciler.ledger.LedgerStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerStoreTest {

    private AuditLog auditLog;
    private LedgerStore store;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
        store = new LedgerStore("shard-0", auditLog);
    }

    @Test
    void appendsEntryAndReturnsIt() {
        LedgerEntry entry = store.append("k1", "ACC-1", 500L);
        assertEquals("k1", entry.idempotencyKey());
        assertEquals("ACC-1", entry.accountId());
        assertEquals(500L, entry.amountCents());
        assertEquals(1L, entry.sequence());
    }

    @Test
    void rejectsDuplicateIdempotencyKey() {
        store.append("k1", "ACC-1", 500L);
        DuplicateEntryException e = assertThrows(DuplicateEntryException.class,
                () -> store.append("k1", "ACC-1", 500L));
        assertEquals("k1", e.idempotencyKey());
        assertEquals(1, store.entryCount());
    }

    @Test
    void balanceIsTheSumOfEntries() {
        store.append("k1", "ACC-1", 500L);
        store.append("k2", "ACC-1", -200L);
        store.append("k3", "ACC-2", 700L);
        assertEquals(300L, store.balanceOf("ACC-1"));
        assertEquals(700L, store.balanceOf("ACC-2"));
        assertEquals(0L, store.balanceOf("ACC-unknown"));
    }

    @Test
    void writesOneAuditRecordPerAppend() {
        store.append("k1", "ACC-1", 500L);
        store.append("k2", "ACC-1", 100L);
        assertEquals(2, auditLog.size());
        assertTrue(auditLog.isMonotonicPerAccount());
    }

    @Test
    void entriesOfReturnsACopy() {
        store.append("k1", "ACC-1", 500L);
        assertEquals(1, store.entriesOf("ACC-1").size());
        store.entriesOf("ACC-1").clear();
        assertEquals(1, store.entriesOf("ACC-1").size());
    }

    @Test
    void shardNameIsExposed() {
        assertEquals("shard-0", store.shard());
    }
}
