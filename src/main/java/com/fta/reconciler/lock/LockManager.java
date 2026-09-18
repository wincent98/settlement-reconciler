package com.fta.reconciler.lock;

import com.fta.reconciler.tx.TransactionContext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/** Pessimistic per-account lock used to serialise writes to the same balance. */
public class LockManager {

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Acquires the lock guarding {@code accountId} on {@code shard}.
     * The key is scoped to the active transaction so that nested calls inside one
     * transaction reuse the same monitor instead of deadlocking on themselves.
     */
    public LockHandle acquire(String shard, String accountId) {
        String key = TransactionContext.currentTxId() + "|" + shard + "|" + accountId;
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        return new LockHandle(key, lock);
    }

    public int trackedKeys() {
        return locks.size();
    }
}
