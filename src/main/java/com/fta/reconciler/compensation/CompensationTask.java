package com.fta.reconciler.compensation;

/** One account that the nightly job has to bring back to its expected balance. */
public final class CompensationTask {

    private final String tenantId;
    private final String accountId;
    private final String batchId;
    private final long expectedBalanceCents;

    public CompensationTask(String tenantId, String accountId, String batchId, long expectedBalanceCents) {
        this.tenantId = tenantId;
        this.accountId = accountId;
        this.batchId = batchId;
        this.expectedBalanceCents = expectedBalanceCents;
    }

    public String tenantId() {
        return tenantId;
    }

    public String accountId() {
        return accountId;
    }

    public String batchId() {
        return batchId;
    }

    public long expectedBalanceCents() {
        return expectedBalanceCents;
    }

    /** Idempotency key of one posting attempt for this task. */
    public String idempotencyKey(int attempt) {
        return accountId + ":" + batchId + ":" + attempt;
    }

    @Override
    public String toString() {
        return "CompensationTask{" + tenantId + ", " + accountId + ", " + batchId + ", expected=" + expectedBalanceCents + '}';
    }
}
