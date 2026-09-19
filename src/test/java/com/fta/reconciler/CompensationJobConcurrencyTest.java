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
 * Regression test for the nightly batch being scheduled on two boxes: the same
 * task may run concurrently, and the per-account lock must still serialise them.
 */
class CompensationJobConcurrencyTest {

    @Test
    void concurrentRunsOfTheSameBatchPostOnlyOnce() throws Exception {
        AuditLog auditLog = new AuditLog();
        ShardRouter router = new ShardRouter(auditLog, 4);
        CompensationJob job = new CompensationJob(router, new TransactionManager(), new LockManager(),
                new RetryTemplate(new RetryPolicy(3)));

        int accounts = 16;
        int runners = 4;
        List<CompensationTask> tasks = new ArrayList<>();
        for (int i = 0; i < accounts; i++) {
            String tenantId = "T-" + (i % 4);
            String accountId = tenantId + "-ACC-" + i;
            router.storeFor(tenantId).append("seed:" + accountId, accountId, 400L);
            tasks.add(new CompensationTask(tenantId, accountId, "B-1", 1000L));
        }

        ExecutorService pool = Executors.newFixedThreadPool(runners * 2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (CompensationTask task : tasks) {
            for (int r = 0; r < runners; r++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    job.runOnce(task);
                    return null;
                }));
            }
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        for (CompensationTask task : tasks) {
            LedgerStore store = router.storeFor(task.tenantId());
            assertEquals(1000L, store.balanceOf(task.accountId()),
                    "balance drifted for " + task.accountId());
            assertEquals(2, store.entriesOf(task.accountId()).size(),
                    "duplicate compensation entry for " + task.accountId());
        }
        assertTrue(auditLog.isMonotonicPerAccount(), "audit sequence went backwards");
    }
}
