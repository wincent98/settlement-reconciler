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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Runs one nightly sweep. Operations schedules the sweep on two boxes, so the same
 * batch can be picked up twice within the same window.
 */
public final class ReconcilerApp {

    private static final String BATCH_ID = "B-20260918";
    private static final int TENANTS = 4;
    private static final int ACCOUNTS_PER_TENANT = 8;
    private static final long EXPECTED_CENTS = 1_000_00L;
    private static final long SEEDED_CENTS = 400_00L;

    public static void main(String[] args) throws Exception {
        int sweeps = args.length > 0 ? Integer.parseInt(args[0]) : 2;

        AuditLog auditLog = new AuditLog();
        ShardRouter router = new ShardRouter(auditLog, 4);
        TransactionManager txManager = new TransactionManager();
        LockManager lockManager = new LockManager();
        RetryTemplate retryTemplate = new RetryTemplate(new RetryPolicy(3));
        CompensationJob job = new CompensationJob(router, txManager, lockManager, retryTemplate);

        List<CompensationTask> tasks = seed(router);

        System.out.println("settlement-reconciler 1.4.0");
        System.out.println("tenants=" + TENANTS + " accounts=" + tasks.size()
                + " shards=" + router.shardCount() + " sweeps=" + sweeps);
        System.out.println("running nightly compensation ...");

        ExecutorService pool = Executors.newFixedThreadPool(sweeps * 4);
        CountDownLatch start = new CountDownLatch(1);
        for (CompensationTask task : tasks) {
            for (int sweep = 0; sweep < sweeps; sweep++) {
                pool.submit(() -> {
                    start.await();
                    job.runOnce(task);
                    return null;
                });
            }
        }
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(60, TimeUnit.SECONDS);

        report(router, tasks, auditLog);
    }

    private static List<CompensationTask> seed(ShardRouter router) {
        List<CompensationTask> tasks = new ArrayList<>();
        for (int t = 0; t < TENANTS; t++) {
            String tenantId = "T-" + t;
            LedgerStore store = router.storeFor(tenantId);
            for (int a = 0; a < ACCOUNTS_PER_TENANT; a++) {
                String accountId = tenantId + "-ACC-" + a;
                store.append("seed:" + accountId, accountId, SEEDED_CENTS);
                tasks.add(new CompensationTask(tenantId, accountId, BATCH_ID, EXPECTED_CENTS));
            }
        }
        return tasks;
    }

    private static void report(ShardRouter router, List<CompensationTask> tasks, AuditLog auditLog) {
        System.out.println();
        System.out.println("=== reconciliation report ===");
        int drifted = 0;
        for (CompensationTask task : tasks) {
            LedgerStore store = router.storeFor(task.tenantId());
            long actual = store.balanceOf(task.accountId());
            long expected = task.expectedBalanceCents();
            if (actual != expected) {
                drifted++;
                System.out.printf("  %-14s expected=%-9d actual=%-9d entries=%d  [DRIFT]%n",
                        task.accountId(), expected, actual, store.entriesOf(task.accountId()).size());
            }
        }
        System.out.println();
        System.out.println("accounts checked : " + tasks.size());
        System.out.println("accounts drifted : " + drifted);
        System.out.println("audit records    : " + auditLog.size());
        System.out.println("audit monotonic  : " + auditLog.isMonotonicPerAccount());

        if (drifted > 0) {
            System.out.println();
            System.out.println("FAILED: ledger does not match the expected balances");
            System.exit(1);
        }
        System.out.println();
        System.out.println("OK: ledger matches the expected balances");
    }

    private ReconcilerApp() {
    }
}
