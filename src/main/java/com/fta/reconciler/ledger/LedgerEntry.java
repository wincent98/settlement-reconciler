package com.fta.reconciler.ledger;

import java.util.Objects;

public final class LedgerEntry {

    private final String idempotencyKey;
    private final String accountId;
    private final long amountCents;
    private final long sequence;

    public LedgerEntry(String idempotencyKey, String accountId, long amountCents, long sequence) {
        this.idempotencyKey = idempotencyKey;
        this.accountId = accountId;
        this.amountCents = amountCents;
        this.sequence = sequence;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String accountId() {
        return accountId;
    }

    public long amountCents() {
        return amountCents;
    }

    public long sequence() {
        return sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LedgerEntry)) {
            return false;
        }
        return idempotencyKey.equals(((LedgerEntry) o).idempotencyKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idempotencyKey);
    }

    @Override
    public String toString() {
        return "LedgerEntry{" + idempotencyKey + ", " + accountId + ", " + amountCents + ", seq=" + sequence + '}';
    }
}
