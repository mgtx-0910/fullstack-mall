# 10 购物车 / 订单 / 支付：把完整业务闭环跑一遍

## 这一页解决什么

把前面学到的分层、鉴权、事务、幂等用到一条完整业务链路上：
加购 → 下单 → 支付 → 回调 → 超时关单。

## 涉及的文件

| 模块 | 文件 |
| --- | --- |
| 购物车 | `service/cart/CartController.java`、`CartFacade.java`、`service/CartItemDbService.java`、`entity/CartItemEntity.java`、`mapper/CartItemMapper.java` |
| 库存 | `service/inventory/SkuController.java`、`SkuAdminController.java`、`SkuFacade.java`、`service/SkuDbService.java` |
| 订单 | `service/order/OrderController.java`、`OrderAdminController.java`、`OrderFacade.java` + 各 `*TransactionService` + `OrderCloseScheduler.java` |
| 支付 | `service/payment/PaymentController.java`、`MockPaymentCallbackController.java`、`PaymentFacade.java` + `PaymentCallbackTransactionService.java` |
| 契约 | `contract/cart/*`、`contract/inventory/*`、`contract/order/*`、`contract/payment/*` |

## 完整流程

```bash
# 0) 起服务
docker compose -f docker-compose.dev.yml up -d mysql redis
cd backend && mvn -pl service -am spring-boot:run

# 1) 用户登录
curl -sS -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"xiaoming","password":"Mall123456"}'
USER_TOKEN='粘贴 accessToken'

# 2) 加购物车
curl -sS -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"skuId":1,"quantity":1}'

# 3) 查购物车
curl -sS http://localhost:8080/api/cart -H "Authorization: Bearer $USER_TOKEN"

# 4) 下单（Idempotency-Key 保证重复点击只生成一笔）
curl -sS -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Idempotency-Key: demo-order-001'

# 5) 创建支付单
curl -sS -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"orderId":1}'

# 6) 模拟支付回调（重复发第二次，观察幂等）
curl -sS -X POST http://localhost:8080/api/payments/mock-callback \
  -H 'X-Mock-Payment-Secret: your-local-mock-payment-secret' \
  -H 'Content-Type: application/json' \
  -d '{"paymentNo":"上一步返回的 paymentNo","success":true,"callbackRequestNo":"callback-demo-001"}'
```

## 每一步背后的后端问题

- **加购**：SKU 是否可售？数量是否超过库存？（`CART_SKU_NOT_SALEABLE`、`CART_QUANTITY_EXCEEDS_STOCK`）
- **下单**：购物车是否有变化？（`ORDER_CART_CHANGED`）库存够吗？（`ORDER_INSUFFICIENT_STOCK`）需要事务锁库存
- **支付**：订单状态必须是待支付（`PAYMENT_ORDER_NOT_PAYABLE`），金额要一致（`PAYMENT_AMOUNT_MISMATCH`）
- **回调**：验签（`PAYMENT_CALLBACK_SECRET_INVALID`）、去重（`PAYMENT_CALLBACK_DUPLICATE`）、确认库存售出（事务）
- **超时关单**：定时扫描待支付超期订单，释放锁定库存（`ORDER_CLOSE_STOCK_RELEASE_FAILED`）

想快点看到超时关单，把 `ORDER_PAYMENT_TIMEOUT_MINUTES` 调小后重启服务。

## 自查题

- 下单时哪些写操作必须在同一个事务里？
- 支付回调重复到达，靠什么保证只处理一次？
- 库存释放失败时，系统应该怎么办？
