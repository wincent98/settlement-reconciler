package com.fta.reconciler.tx;

import java.util.concurrent.atomic.AtomicLong;

/** Minimal transaction manager: allocates transaction ids and maintains the context stack. */
public class TransactionManager {

    private final AtomicLong counter = new AtomicLong();

    public <T> T execute(Propagation propagation, TransactionCallback<T> callback) {
        String current = TransactionContext.currentTxId();
        boolean startNew = propagation == Propagation.REQUIRES_NEW || current == null;
        String txId = startNew ? "tx-" + counter.incrementAndGet() : current;
        TransactionContext.push(txId);
        try {
            return callback.doInTransaction(txId);
        } finally {
            TransactionContext.pop();
        }
    }

    public long allocatedTransactions() {
        return counter.get();
    }
}
