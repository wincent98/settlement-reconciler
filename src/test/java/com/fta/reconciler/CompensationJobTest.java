package com.fta.reconciler;

import com.fta.reconciler.audit.AuditLog;
import com.fta.reconciler.compensation.CompensationJob;
import com.fta.reconciler.compensation.CompensationResult;
import com.fta.reconciler.compensation.CompensationTask;
import com.fta.reconciler.ledger.LedgerStore;
import com.fta.reconciler.lock.LockManager;
import com.fta.reconciler.retry.RetryPolicy;
import com.fta.reconciler.retry.RetryTemplate;
import com.fta.reconciler.routing.ShardRouter;
import com.fta.reconciler.tx.TransactionContext;
import com.fta.reconciler.tx.TransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompensationJobTest {

    private AuditLog auditLog;
    private ShardRouter router;
    private CompensationJob job;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
        router = new ShardRouter(auditLog, 4);
        job = new CompensationJob(router, new TransactionManager(), new LockManager(),
                new RetryTemplate(new RetryPolicy(3)));
    }

    @Test
    void postsTheMissingAmount() {
        LedgerStore store = router.storeFor("T-1");
        store.append("seed:ACC-1", "ACC-1", 400L);

        CompensationResult result = job.runOnce(new CompensationTask("T-1", "ACC-1", "B-1", 1000L));

        assertEquals(CompensationResult.Status.POSTED, result.status());
        assertEquals(600L, result.entry().amountCents());
        assertEquals(1000L, store.balanceOf("ACC-1"));
    }

    @Test
    void skipsAnAccountThatAlreadyMatches() {
        LedgerStore store = router.storeFor("T-1");
        store.append("seed:ACC-2", "ACC-2", 1000L);

        CompensationResult result = job.runOnce(new CompensationTask("T-1", "ACC-2", "B-1", 1000L));

        assertEquals(CompensationResult.Status.SKIPPED, result.status());
        assertEquals(1, store.entriesOf("ACC-2").size());
    }

    @Test
    void isIdempotentWhenTheSameBatchRunsTwiceInSequence() {
        LedgerStore store = router.storeFor("T-3");
        store.append("seed:ACC-3", "ACC-3", 400L);
        CompensationTask task = new CompensationTask("T-3", "ACC-3", "B-1", 1000L);

        job.runOnce(task);
        CompensationResult second = job.runOnce(task);

        assertEquals(CompensationResult.Status.SKIPPED, second.status());
        assertEquals(1000L, store.balanceOf("ACC-3"));
        assertEquals(2, store.entriesOf("ACC-3").size());
    }

    @Test
    void handlesANegativeDrift() {
        LedgerStore store = router.storeFor("T-4");
        store.append("seed:ACC-4", "ACC-4", 1500L);

        job.runOnce(new CompensationTask("T-4", "ACC-4", "B-1", 1000L));

        assertEquals(1000L, store.balanceOf("ACC-4"));
    }

    @Test
    void leavesNoOpenTransactionBehind() {
        router.storeFor("T-5").append("seed:ACC-5", "ACC-5", 400L);
        job.runOnce(new CompensationTask("T-5", "ACC-5", "B-1", 1000L));
        assertFalse(TransactionContext.isActive());
    }

    @Test
    void writesAMonotonicAuditTrail() {
        router.storeFor("T-6").append("seed:ACC-6", "ACC-6", 400L);
        job.runOnce(new CompensationTask("T-6", "ACC-6", "B-1", 1000L));
        assertTrue(auditLog.isMonotonicPerAccount());
        assertEquals(2, auditLog.size());
    }
}
