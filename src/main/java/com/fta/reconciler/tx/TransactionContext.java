package com.fta.reconciler.tx;

import java.util.ArrayDeque;
import java.util.Deque;

/** Holds the transaction id stack of the current thread. */
public final class TransactionContext {

    private static final ThreadLocal<Deque<String>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private TransactionContext() {
    }

    static void push(String txId) {
        STACK.get().push(txId);
    }

    static void pop() {
        Deque<String> stack = STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            STACK.remove();
        }
    }

    public static String currentTxId() {
        Deque<String> stack = STACK.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    public static boolean isActive() {
        return currentTxId() != null;
    }

    public static int depth() {
        return STACK.get().size();
    }
}
