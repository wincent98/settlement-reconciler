package com.fta.reconciler;

import com.fta.reconciler.audit.AuditLog;
import com.fta.reconciler.compensation.CompensationJob;
import com.fta.reconciler.compensation.CompensationTask;
import com.fta.reconciler.ledger.LedgerStore;
import com.fta.reconciler.lock.LockManager;
import com.fta.reconciler.retry.RetryPolicy;
import com.fta.reconciler.retry.RetryTemplate;
import com.fta.reconciler.routing.ShardRouter;
import com.fta.reconciler.tx.TransactionManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the production incident where the same nightly batch was
 * executed twice concurrently (two scheduler boxes) and produced duplicate
 * ledger entries whose idempotency keys differed only in the attempt suffix.
 */
class ConcurrentCompensationTest {

    private static final int TENANTS = 4;
    private static final int ACCOUNTS_PER_TENANT = 8;
    private static final long EXPECTED_CENTS = 1_000_00L;
    private static final long SEEDED_CENTS = 400_00L;

    @Test
    void concurrentSweepsOfTheSameBatchPostEachCompensationExactlyOnce() throws Exception {
        AuditLog auditLog = new AuditLog();
        ShardRouter router = new ShardRouter(auditLog, 4);
        CompensationJob job = new CompensationJob(router, new TransactionManager(),
                new LockManager(), new RetryTemplate(new RetryPolicy(3)));

        List<CompensationTask> tasks = new ArrayList<>();
        for (int t = 0; t < TENANTS; t++) {
            String tenantId = "T-" + t;
            LedgerStore store = router.storeFor(tenantId);
            for (int a = 0; a < ACCOUNTS_PER_TENANT; a++) {
                String accountId = tenantId + "-ACC-" + a;
                store.append("seed:" + accountId, accountId, SEEDED_CENTS);
                tasks.add(new CompensationTask(tenantId, accountId, "B-1", EXPECTED_CENTS));
            }
        }

        int sweeps = 2;
        ExecutorService pool = Executors.newFixedThreadPool(sweeps * 4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (CompensationTask task : tasks) {
            for (int sweep = 0; sweep < sweeps; sweep++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    job.runOnce(task);
                    return null;
                }));
            }
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        for (Future<?> future : futures) {
            future.get();
        }

        for (CompensationTask task : tasks) {
            LedgerStore store = router.storeFor(task.tenantId());
            assertEquals(EXPECTED_CENTS, store.balanceOf(task.accountId()),
                    "balance drifted for " + task.accountId());
            assertEquals(2, store.entriesOf(task.accountId()).size(),
                    "expected seed + exactly one compensation entry for " + task.accountId());
        }
        assertTrue(auditLog.isMonotonicPerAccount());
    }
}
