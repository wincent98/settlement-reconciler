package com.fta.reconciler.retry;

import java.util.function.IntFunction;

/** Runs an action and replays it while the policy classifies the failure as retryable. */
public class RetryTemplate {

    private final RetryPolicy policy;

    public RetryTemplate(RetryPolicy policy) {
        this.policy = policy;
    }

    public RetryPolicy policy() {
        return policy;
    }

    public <T> T execute(IntFunction<T> action) {
        RuntimeException last = null;
        for (int attempt = 0; attempt < policy.maxAttempts(); attempt++) {
            try {
                return action.apply(attempt);
            } catch (RuntimeException e) {
                if (!policy.isRetryable(e)) {
                    throw e;
                }
                last = e;
            }
        }
        throw last;
    }
}
