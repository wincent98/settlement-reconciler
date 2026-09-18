# settlement-reconciler

多租户结算对账服务的核心引擎。夜间批次扫描所有账户，把实际账本余额补偿回清算系统给出的期望余额。

## 模块

| 包 | 职责 |
|----|------|
| `ledger` | 账本存储。`LedgerStore.append` 在 `idempotency_key` 上有唯一约束，重复写入抛 `DuplicateEntryException` |
| `routing` | 多租户分片路由。`ShardRouter` 按 tenantId 把请求路由到对应分片的 `LedgerStore` |
| `tx` | 事务管理。支持 `REQUIRED` / `REQUIRES_NEW` 两种传播行为，事务 id 维护在线程上下文里 |
| `lock` | 账户粒度的悲观锁，用于串行化同一账户上的写入 |
| `retry` | 重试策略。存储层写冲突被视为可重试 |
| `compensation` | 夜间补偿任务 `CompensationJob` |
| `audit` | 追加型审计流水。下游监管导出按插入顺序回放，同一账户的 sequence 不允许回退 |

## 构建与运行

```bash
mvn -o package
java -jar target/settlement-reconciler-1.4.0.jar
```

可选参数：一次运行的 sweep 次数（默认 2，对应调度器在两台机器上都配置了同一批次）。

```bash
java -jar target/settlement-reconciler-1.4.0.jar 2
```

退出码 0 表示账本与期望余额一致，1 表示存在漂移。

## 测试

```bash
mvn -o test
```

## 运维记录

- 2026-09-12 起，夜间批次结束后偶发发现少量账户余额高于期望值，账本里出现金额相同、
  `idempotency_key` 仅后缀不同的重复分录。无异常堆栈。
- 2026-09-15 调度侧确认：同一夜间批次在两台机器上都有配置，存在同一时间窗口内被执行两次的可能。
