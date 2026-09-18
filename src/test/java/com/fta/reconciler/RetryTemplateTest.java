package com.fta.reconciler;

import com.fta.reconciler.ledger.DuplicateEntryException;
import com.fta.reconciler.retry.RetryPolicy;
import com.fta.reconciler.retry.RetryTemplate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryTemplateTest {

    private final RetryTemplate template = new RetryTemplate(new RetryPolicy(3));

    @Test
    void rejectsNonPositiveMaxAttempts() {
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(0));
    }

    @Test
    void classifiesWriteConflictsAsRetryable() {
        RetryPolicy policy = new RetryPolicy(3);
        assertTrue(policy.isRetryable(new DuplicateEntryException("k1")));
        assertFalse(policy.isRetryable(new IllegalArgumentException("bad input")));
    }

    @Test
    void returnsTheFirstSuccessfulAttempt() {
        List<Integer> attempts = new ArrayList<>();
        String value = template.execute(attempt -> {
            attempts.add(attempt);
            return "ok-" + attempt;
        });
        assertEquals("ok-0", value);
        assertEquals(1, attempts.size());
    }

    @Test
    void replaysUntilTheActionSucceeds() {
        List<Integer> attempts = new ArrayList<>();
        String value = template.execute(attempt -> {
            attempts.add(attempt);
            if (attempt < 2) {
                throw new DuplicateEntryException("k" + attempt);
            }
            return "ok-" + attempt;
        });
        assertEquals("ok-2", value);
        assertEquals(List.of(0, 1, 2), attempts);
    }

    @Test
    void propagatesNonRetryableFailuresImmediately() {
        List<Integer> attempts = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> template.execute(attempt -> {
            attempts.add(attempt);
            throw new IllegalArgumentException("bad input");
        }));
        assertEquals(1, attempts.size());
    }

    @Test
    void givesUpAfterMaxAttempts() {
        assertThrows(DuplicateEntryException.class, () -> template.execute(attempt -> {
            throw new DuplicateEntryException("k" + attempt);
        }));
    }
}
