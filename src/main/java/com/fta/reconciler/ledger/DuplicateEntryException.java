package com.fta.reconciler.ledger;

/** Raised when the unique constraint on {@code idempotency_key} rejects an append. */
public class DuplicateEntryException extends RuntimeException {

    private final String idempotencyKey;

    public DuplicateEntryException(String idempotencyKey) {
        super("duplicate ledger entry for idempotency key " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }
}
