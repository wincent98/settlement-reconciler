package com.fta.reconciler;

import com.fta.reconciler.lock.LockHandle;
import com.fta.reconciler.lock.LockManager;
import com.fta.reconciler.tx.Propagation;
import com.fta.reconciler.tx.TransactionManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LockManagerTest {

    private final LockManager lockManager = new LockManager();
    private final TransactionManager txManager = new TransactionManager();

    @Test
    void acquiresAndReleasesTheLock() {
        try (LockHandle handle = lockManager.acquire("shard-0", "ACC-1")) {
            assertTrue(handle.key().contains("ACC-1"));
        }
        assertEquals(1, lockManager.trackedKeys());
    }

    @Test
    void differentAccountsUseDifferentKeys() {
        String first;
        String second;
        try (LockHandle handle = lockManager.acquire("shard-0", "ACC-1")) {
            first = handle.key();
        }
        try (LockHandle handle = lockManager.acquire("shard-0", "ACC-2")) {
            second = handle.key();
        }
        assertNotEquals(first, second);
        assertEquals(2, lockManager.trackedKeys());
    }

    @Test
    void differentShardsUseDifferentKeys() {
        String first;
        String second;
        try (LockHandle handle = lockManager.acquire("shard-0", "ACC-1")) {
            first = handle.key();
        }
        try (LockHandle handle = lockManager.acquire("shard-1", "ACC-1")) {
            second = handle.key();
        }
        assertNotEquals(first, second);
    }

    @Test
    void isReentrantWithinTheSameThread() {
        txManager.execute(Propagation.REQUIRED, txId -> {
            try (LockHandle outer = lockManager.acquire("shard-0", "ACC-1");
                 LockHandle inner = lockManager.acquire("shard-0", "ACC-1")) {
                assertEquals(outer.key(), inner.key());
            }
            return null;
        });
    }
}
