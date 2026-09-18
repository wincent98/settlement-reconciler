package com.fta.reconciler.lock;

import java.util.concurrent.locks.ReentrantLock;

/** Auto-closable handle released when the guarded block exits. */
public final class LockHandle implements AutoCloseable {

    private final String key;
    private final ReentrantLock lock;

    LockHandle(String key, ReentrantLock lock) {
        this.key = key;
        this.lock = lock;
    }

    public String key() {
        return key;
    }

    @Override
    public void close() {
        lock.unlock();
    }
}
