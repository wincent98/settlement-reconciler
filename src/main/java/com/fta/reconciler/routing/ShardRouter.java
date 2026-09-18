package com.fta.reconciler.routing;

import com.fta.reconciler.audit.AuditLog;
import com.fta.reconciler.ledger.LedgerStore;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Routes a tenant to the shard that owns its ledger rows. */
public class ShardRouter {

    private final Map<String, LedgerStore> shards = new LinkedHashMap<>();
    private final int shardCount;

    public ShardRouter(AuditLog auditLog, int shardCount) {
        if (shardCount <= 0) {
            throw new IllegalArgumentException("shardCount must be positive");
        }
        this.shardCount = shardCount;
        for (int i = 0; i < shardCount; i++) {
            String name = "shard-" + i;
            shards.put(name, new LedgerStore(name, auditLog));
        }
    }

    public String shardFor(String tenantId) {
        int index = Math.floorMod(tenantId.hashCode(), shardCount);
        return "shard-" + index;
    }

    public LedgerStore storeFor(String tenantId) {
        return shards.get(shardFor(tenantId));
    }

    public Collection<LedgerStore> stores() {
        return Collections.unmodifiableCollection(shards.values());
    }

    public int shardCount() {
        return shardCount;
    }
}
