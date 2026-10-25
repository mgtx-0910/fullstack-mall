# 09 状态机、事务与幂等：商品为什么不能随便上下架

## 这一页解决什么

后端不只处理"请求成功"，还要处理**并发、失败、重复请求、外部回调**。
这一页讲状态机、事务、幂等这三件事在本仓库里的落点。

## 状态机：状态不能随便跳

`contract/product/ProductStatus.java`：

```text
DRAFT（草稿）     管理员维护，匿名用户不可见
ON_SALE（已上架） 匿名用户可查，且上架时必须存在可售 SKU
OFF_SHELF（已下架）保留历史数据，匿名用户不可见
```

`contract/order/OrderStatus.java`：

```text
PENDING_PAYMENT → PAID / CANCELLED / CLOSED（待支付进入终态后不能再变）
```

为什么商品不能随便上下架？状态一旦随意流转，就会出现：
"下架商品还能被下单""草稿商品出现在公开列表""已支付订单被改成待支付"。

上架还有前置条件：**至少有一个有价格且有库存的 SKU**，
否则返回 `PRODUCT_NOT_READY_FOR_SALE`；非法流转返回 `INVALID_PRODUCT_STATUS_TRANSITION`。

## 事务：多张表要么一起成功，要么一起失败

| 文件 | 事务内容 |
| --- | --- |
| `service/order/OrderCreateTransactionService.java` | 写订单 + 写明细 + 锁库存 |
| `service/order/OrderCancelTransactionService.java` | 取消订单 + 释放库存 |
| `service/order/OrderCloseTransactionService.java` | 超时关单 + 释放库存 |
| `service/payment/PaymentCreateTransactionService.java` | 创建支付流水 |
| `service/payment/PaymentCallbackTransactionService.java` | 回调处理 + 确认库存售出 |

把事务单独拆成 `*TransactionService`，是为了让 Facade 保持"编排"职责，
同时避免事务边界被随意扩大（比如在事务里做 Redis 写入、远程调用）。

## 幂等：同一个请求来两次，结果必须一样

- 下单：`Idempotency-Key` 请求头（格式不合法 → `ORDER_IDEMPOTENCY_KEY_INVALID`）
- 支付回调：`mall_payment.trade_no` 唯一索引 + 回调事件 ID 去重（`PAYMENT_CALLBACK_DUPLICATE`）
- 状态条件更新：`UPDATE ... WHERE status = PENDING_PAYMENT`，
  并发下只有真正改到行的请求算成功——最朴素的乐观并发控制

## 并发库存

- SKU 库存有版本号，冲突返回 `INVENTORY_VERSION_CONFLICT`
- 库存不足返回 `INSUFFICIENT_STOCK` / `ORDER_INSUFFICIENT_STOCK`
- 释放失败也有明确业务码：`ORDER_STOCK_RELEASE_FAILED`、`ORDER_CLOSE_STOCK_RELEASE_FAILED`

## 超时关单

- `service/order/OrderCloseScheduler.java`：定时扫描
- `service/order/OrderCloseBatchService.java`：分批扫描，避免一次捞出太多数据
- 可调配置：`ORDER_PAYMENT_TIMEOUT_MINUTES`（默认 15）、`ORDER_CLOSE_SCAN_FIXED_DELAY_MS`、`ORDER_CLOSE_SCAN_BATCH_SIZE`

## 自查题

- 已支付订单还能被取消吗？靠什么保证？
- 支付回调重复到达两次，系统怎么保证只处理一次？
- 下单失败时，已锁定的库存怎么办？
- 为什么状态更新要写成 `WHERE status = ?` 的条件更新？
