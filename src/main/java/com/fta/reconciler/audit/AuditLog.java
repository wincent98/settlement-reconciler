package com.fta.reconciler.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Append-only audit trail.
 *
 * Downstream regulatory export replays this list in insertion order, so for a given
 * account the recorded sequence numbers must never go backwards.
 */
public class AuditLog {

    private final List<AuditRecord> records = Collections.synchronizedList(new ArrayList<>());

    public void record(String accountId, long sequence, String idempotencyKey) {
        records.add(new AuditRecord(accountId, sequence, idempotencyKey));
    }

    public List<AuditRecord> records() {
        synchronized (records) {
            return new ArrayList<>(records);
        }
    }

    public int size() {
        return records.size();
    }

    /** Verifies the append order matches the sequence order for every account. */
    public boolean isMonotonicPerAccount() {
        Map<String, Long> last = new HashMap<>();
        for (AuditRecord record : records()) {
            Long previous = last.get(record.accountId());
            if (previous != null && record.sequence() <= previous) {
                return false;
            }
            last.put(record.accountId(), record.sequence());
        }
        return true;
    }
}
