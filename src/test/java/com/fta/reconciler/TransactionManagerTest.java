package com.fta.reconciler;

import com.fta.reconciler.tx.Propagation;
import com.fta.reconciler.tx.TransactionContext;
import com.fta.reconciler.tx.TransactionManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionManagerTest {

    private final TransactionManager txManager = new TransactionManager();

    @Test
    void startsATransactionWhenNoneIsActive() {
        assertNull(TransactionContext.currentTxId());
        String id = txManager.execute(Propagation.REQUIRED, txId -> txId);
        assertTrue(id.startsWith("tx-"));
        assertNull(TransactionContext.currentTxId());
    }

    @Test
    void requiredReusesTheOuterTransaction() {
        String[] ids = txManager.execute(Propagation.REQUIRED,
                outer -> new String[]{outer, txManager.execute(Propagation.REQUIRED, inner -> inner)});
        assertEquals(ids[0], ids[1]);
    }

    @Test
    void requiresNewAlwaysStartsAnotherTransaction() {
        String[] ids = txManager.execute(Propagation.REQUIRED,
                outer -> new String[]{outer, txManager.execute(Propagation.REQUIRES_NEW, inner -> inner)});
        assertNotEquals(ids[0], ids[1]);
    }

    @Test
    void restoresTheOuterTransactionAfterANestedOne() {
        txManager.execute(Propagation.REQUIRED, outer -> {
            txManager.execute(Propagation.REQUIRES_NEW, inner -> null);
            assertEquals(outer, TransactionContext.currentTxId());
            assertEquals(1, TransactionContext.depth());
            return null;
        });
        assertFalse(TransactionContext.isActive());
    }

    @Test
    void clearsTheContextWhenTheCallbackThrows() {
        try {
            txManager.execute(Propagation.REQUIRED, txId -> {
                throw new IllegalArgumentException("boom");
            });
        } catch (IllegalArgumentException expected) {
            // ignored
        }
        assertNull(TransactionContext.currentTxId());
    }

    @Test
    void countsAllocatedTransactions() {
        long before = txManager.allocatedTransactions();
        txManager.execute(Propagation.REQUIRED, txId -> null);
        assertEquals(before + 1, txManager.allocatedTransactions());
    }
}
