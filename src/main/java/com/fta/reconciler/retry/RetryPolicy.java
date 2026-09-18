package com.fta.reconciler.retry;

import com.fta.reconciler.ledger.DuplicateEntryException;

/** Decides how often and on which failures an operation is retried. */
public class RetryPolicy {

    private final int maxAttempts;

    public RetryPolicy(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        this.maxAttempts = maxAttempts;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    /** Storage level write conflicts are transient and safe to replay. */
    public boolean isRetryable(RuntimeException exception) {
        return exception instanceof DuplicateEntryException
                || exception instanceof IllegalStateException;
    }
}
