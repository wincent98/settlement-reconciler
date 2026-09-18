package com.fta.reconciler.compensation;

import com.fta.reconciler.ledger.LedgerEntry;

public final class CompensationResult {

    public enum Status {
        POSTED,
        SKIPPED
    }

    private final Status status;
    private final CompensationTask task;
    private final LedgerEntry entry;

    private CompensationResult(Status status, CompensationTask task, LedgerEntry entry) {
        this.status = status;
        this.task = task;
        this.entry = entry;
    }

    public static CompensationResult posted(CompensationTask task, LedgerEntry entry) {
        return new CompensationResult(Status.POSTED, task, entry);
    }

    public static CompensationResult skipped(CompensationTask task) {
        return new CompensationResult(Status.SKIPPED, task, null);
    }

    public Status status() {
        return status;
    }

    public CompensationTask task() {
        return task;
    }

    public LedgerEntry entry() {
        return entry;
    }

    @Override
    public String toString() {
        return "CompensationResult{" + status + ", " + task.accountId() + '}';
    }
}
