package com.fta.reconciler.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/** Pessimistic per-account lock used to serialise writes to the same balance. */
public class LockManager {

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Acquires the lock guarding {@code accountId} on {@code shard}.
     * The key is scoped only to the shard and account so that concurrent
     * transactions serialise on the same account. Nested calls from one thread
     * still work because {@link ReentrantLock} is reentrant.
     */
    public LockHandle acquire(String shard, String accountId) {
        String key = shard + "|" + accountId;
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        return new LockHandle(key, lock);
    }

    public int trackedKeys() {
        return locks.size();
    }
}
