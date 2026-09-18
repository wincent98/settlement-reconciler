package com.fta.reconciler.tx;

@FunctionalInterface
public interface TransactionCallback<T> {
    T doInTransaction(String txId);
}
