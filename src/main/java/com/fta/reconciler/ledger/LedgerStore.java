package com.fta.reconciler.ledger;

import com.fta.reconciler.audit.AuditLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory stand-in for one physical shard of the ledger table. */
public class LedgerStore {

    private final String shard;
    private final AuditLog auditLog;
    private final ConcurrentMap<String, LedgerEntry> entriesByKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<LedgerEntry>> entriesByAccount = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public LedgerStore(String shard, AuditLog auditLog) {
        this.shard = shard;
        this.auditLog = auditLog;
    }

    public String shard() {
        return shard;
    }

    /** Appends one entry. The unique constraint on the idempotency key rejects replays. */
    public LedgerEntry append(String idempotencyKey, String accountId, long amountCents) {
        long seq = sequence.incrementAndGet();
        LedgerEntry entry = new LedgerEntry(idempotencyKey, accountId, amountCents, seq);
        LedgerEntry existing = entriesByKey.putIfAbsent(idempotencyKey, entry);
        if (existing != null) {
            throw new DuplicateEntryException(idempotencyKey);
        }
        entriesByAccount.computeIfAbsent(accountId, k -> Collections.synchronizedList(new ArrayList<>())).add(entry);
        auditLog.record(accountId, seq, idempotencyKey);
        return entry;
    }

    public long balanceOf(String accountId) {
        long total = 0L;
        for (LedgerEntry entry : entriesOf(accountId)) {
            total += entry.amountCents();
        }
        return total;
    }

    public List<LedgerEntry> entriesOf(String accountId) {
        List<LedgerEntry> entries = entriesByAccount.get(accountId);
        if (entries == null) {
            return Collections.emptyList();
        }
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    public int entryCount() {
        return entriesByKey.size();
    }
}
