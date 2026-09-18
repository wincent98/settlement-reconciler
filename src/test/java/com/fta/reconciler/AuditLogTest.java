package com.fta.reconciler;

import com.fta.reconciler.audit.AuditLog;
import com.fta.reconciler.audit.AuditRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogTest {

    private final AuditLog auditLog = new AuditLog();

    @Test
    void keepsRecordsInAppendOrder() {
        auditLog.record("ACC-1", 1L, "k1");
        auditLog.record("ACC-1", 2L, "k2");
        List<AuditRecord> records = auditLog.records();
        assertEquals(2, records.size());
        assertEquals("k1", records.get(0).idempotencyKey());
        assertEquals(2L, records.get(1).sequence());
    }

    @Test
    void acceptsInterleavedAccounts() {
        auditLog.record("ACC-1", 1L, "k1");
        auditLog.record("ACC-2", 2L, "k2");
        auditLog.record("ACC-1", 3L, "k3");
        assertTrue(auditLog.isMonotonicPerAccount());
    }

    @Test
    void detectsOutOfOrderSequencesForOneAccount() {
        auditLog.record("ACC-1", 5L, "k5");
        auditLog.record("ACC-1", 4L, "k4");
        assertFalse(auditLog.isMonotonicPerAccount());
    }

    @Test
    void returnsADefensiveCopy() {
        auditLog.record("ACC-1", 1L, "k1");
        auditLog.records().clear();
        assertEquals(1, auditLog.size());
    }
}
