package com.fta.reconciler;

import com.fta.reconciler.audit.AuditLog;
import com.fta.reconciler.ledger.LedgerStore;
import com.fta.reconciler.routing.ShardRouter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShardRouterTest {

    private final ShardRouter router = new ShardRouter(new AuditLog(), 4);

    @Test
    void rejectsANonPositiveShardCount() {
        assertThrows(IllegalArgumentException.class, () -> new ShardRouter(new AuditLog(), 0));
    }

    @Test
    void createsOneStorePerShard() {
        assertEquals(4, router.shardCount());
        assertEquals(4, router.stores().size());
    }

    @Test
    void routesATenantToAStableShard() {
        String first = router.shardFor("T-1");
        assertEquals(first, router.shardFor("T-1"));
        assertSame(router.storeFor("T-1"), router.storeFor("T-1"));
    }

    @Test
    void storeBelongsToTheRoutedShard() {
        LedgerStore store = router.storeFor("T-2");
        assertEquals(router.shardFor("T-2"), store.shard());
    }
}
