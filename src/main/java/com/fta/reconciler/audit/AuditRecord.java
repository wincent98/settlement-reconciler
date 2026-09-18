package com.fta.reconciler.audit;

/** One immutable audit line written for every ledger append. */
public final class AuditRecord {

    private final String accountId;
    private final long sequence;
    private final String idempotencyKey;

    public AuditRecord(String accountId, long sequence, String idempotencyKey) {
        this.accountId = accountId;
        this.sequence = sequence;
        this.idempotencyKey = idempotencyKey;
    }

    public String accountId() {
        return accountId;
    }

    public long sequence() {
        return sequence;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    @Override
    public String toString() {
        return "AuditRecord{" + accountId + ", seq=" + sequence + ", key=" + idempotencyKey + '}';
    }
}
