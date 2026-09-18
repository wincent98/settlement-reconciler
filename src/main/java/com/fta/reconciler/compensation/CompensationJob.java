package com.fta.reconciler.compensation;

import com.fta.reconciler.ledger.LedgerEntry;
import com.fta.reconciler.ledger.LedgerStore;
import com.fta.reconciler.lock.LockHandle;
import com.fta.reconciler.lock.LockManager;
import com.fta.reconciler.retry.RetryTemplate;
import com.fta.reconciler.routing.ShardRouter;
import com.fta.reconciler.tx.Propagation;
import com.fta.reconciler.tx.TransactionManager;

/** Nightly job that tops up accounts whose balance drifted away from the expected value. */
public class CompensationJob {

    private final ShardRouter router;
    private final TransactionManager txManager;
    private final LockManager lockManager;
    private final RetryTemplate retryTemplate;

    public CompensationJob(ShardRouter router,
                           TransactionManager txManager,
                           LockManager lockManager,
                           RetryTemplate retryTemplate) {
        this.router = router;
        this.txManager = txManager;
        this.lockManager = lockManager;
        this.retryTemplate = retryTemplate;
    }

    public CompensationResult runOnce(CompensationTask task) {
        return txManager.execute(Propagation.REQUIRED, batchTx ->
                // Each account is settled in its own transaction so one bad account
                // cannot roll back the whole nightly batch.
                txManager.execute(Propagation.REQUIRES_NEW, accountTx -> {
                    LedgerStore store = router.storeFor(task.tenantId());
                    try (LockHandle handle = lockManager.acquire(store.shard(), task.accountId())) {
                        long current = store.balanceOf(task.accountId());
                        long missing = task.expectedBalanceCents() - current;
                        if (missing == 0L) {
                            return CompensationResult.skipped(task);
                        }
                        return retryTemplate.execute(attempt -> {
                            LedgerEntry entry = store.append(task.idempotencyKey(attempt), task.accountId(), missing);
                            return CompensationResult.posted(task, entry);
                        });
                    }
                }));
    }
}
