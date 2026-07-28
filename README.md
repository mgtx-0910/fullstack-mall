# 二手电商全栈学习项目

这是一个以后端学习为重点的本地全栈项目。当前代码已完成阶段 0～9。阶段 9 为公开商品详情新增了 Redis Cache Aside、正常/空值 TTL、写后失效、坏 JSON 自愈、fail-open 降级和可观察日志。

> 文档同步状态（2026-07-12）：已按当前正式源码重新核对 contract、service、SQL、测试和阶段 9 Redis 调用链。示例继续统一使用普通 Java class、Lombok `@Data`、`jakarta.annotation.Resource` 字段注入、`I...Facade + ...Facade`、MyBatis-Plus `BaseMapper + ServiceImpl`；不使用 Java `record`。当前商品详情链路为 `Controller → ProductFacade → ProductDetailCacheService → RedisOperatorClient → StringRedisTemplate/Lettuce/Redis`，MISS/DEGRADED/BYPASS 才回源 `DbService → Mapper → MySQL`。商品创建和状态变化均在数据库成功后删除详情缓存；分类持久化修改接口尚未实现，因此文档明确了未来按分类批量失效的边界。阶段 0～8 的事务、锁、状态机和库存行为保持不变。最新默认全量测试结果为 `Tests run: 99, Failures: 0, Errors: 0, Skipped: 8`；8 个跳过项包含 7 个真实 MySQL 容器测试和 1 个真实 Redis 容器测试。显式开启后，真实 MySQL Testcontainers 测试结果为 `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`，真实 Redis Testcontainers 测试结果为 `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。Docker Compose 中 MySQL 与 Redis 均已通过健康检查，`redis-cli PING` 返回 `PONG`。当前路线仍暂不引入微服务、RPC、MQ 或真实支付平台。

## 文档阅读入口

如果你想按“先懂语法，再照顺序写接口”的方式学习，建议按下面顺序阅读：

1. [`java-spring-mybatis-syntax-guide.md`](docs/backend-basics/java-spring-mybatis-syntax-guide.md)：Spring Boot、Lombok、`@Resource`、MyBatis-Plus、事务等基础语法；
2. [`company-style-alignment.md`](docs/backend-basics/company-style-alignment.md)：本项目需要保持的企业常见代码风格；
3. [`add-field-guide.md`](docs/backend-basics/add-field-guide.md)：新增数据库字段应该改哪些位置；
4. [`add-api-guide.md`](docs/backend-basics/add-api-guide.md)：新增普通后端接口的推荐代码顺序；
5. [`add-secured-api-guide.md`](docs/backend-basics/add-secured-api-guide.md)：新增登录态或管理员接口时，认证与权限代码放在哪里；
6. `phase-0`～`phase-9`：按业务阶段阅读完整案例。

> 注意：阶段文档会刻意站在“当时正在学习的阶段”描述边界。例如阶段 0 会解释为什么阶段 0 暂不接数据库，这不代表当前完整工程没有数据库。判断当前工程已经实现到哪里，以本 README 顶部的“文档同步状态”为准。

## 项目位置

`fullstack-mall/`

## 当前目录

```text
fullstack-mall/
├── backend/
│   ├── contract/
│   └── service/
├── docs/backend-basics/
├── sql/
└── docker-compose.dev.yml
```


## 当前代码与文档怎么对应

阅读阶段文档时要区分两个概念：

1. **阶段学习边界**：例如阶段 4 重点讲商品，阶段 7 重点讲订单；
2. **当前正式源码**：后续阶段可能会继续扩展前面阶段的同一个类和同一条业务链路。

当前以源码为准的主要边界如下：

| 位置 | 当前职责 | 阅读重点 |
|---|---|---|
| `backend/contract/src/main` | Request、Response、enum、`I...Facade` 接口 | 前端能提交什么、后端返回什么、对外用例是什么 |
| `backend/service/src/main/.../*Controller.java` | URL、HTTP 参数、`@Valid`、统一响应 | 不在这里写跨表事务和库存规则 |
| `backend/service/src/main/.../*Facade.java` | 当前用户、业务编排、Entity 到 Response 转换 | 连接 contract 与内部实现 |
| `*TransactionService.java` | 跨表写操作的事务边界 | 订单、支付、库存必须一起成功或一起回滚 |
| `*DbService.java` | 面向业务语义封装 MyBatis-Plus | 简单查询复用 `ServiceImpl`，复杂并发语义调用 Mapper |
| `product/cache/*` | 商品详情 key、JSON、TTL、空值、失效和降级 | Redis 只是 MySQL 前面的性能层 |
| `*Mapper.java` | SQL 与影响行数 | 状态条件 UPDATE、库存原子更新、`SELECT ... FOR UPDATE` |
| `sql/01_schema.sql` | 表、字段、索引、唯一约束 | 数据库是持久化约束的最终落点 |

如果阶段文档中的历史学习流程与当前源码发生差异，先看该文档顶部的“后续阶段扩展”说明，再以当前源码和本 README 的同步状态为准。

## 阶段 9 快速启动

阶段 5～8 新增或扩展了 `mall_product_sku`、`mall_cart_item`、`mall_order`、`mall_order_item` 和 `mall_payment` 表。如果数据库来自较早阶段，缺少支付表或订单支付时间字段，需要重建**本学习项目自己的** MySQL volume，让初始化 SQL 重新执行：

```bash
cd fullstack-mall
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up -d mysql redis
```

> `down -v` 会删除本学习项目 MySQL volume 中的数据。不要对非本项目数据库执行。

运行测试并启动后端：

```bash
cd backend
mvn -Dmaven.repo.local=$PWD/.m2-repository clean test
mvn -Dmaven.repo.local=$PWD/.m2-repository clean package
export JWT_SECRET="$(openssl rand -base64 32)"
java -jar service/target/fullstack-mall-service-0.0.1-SNAPSHOT.jar
```

真实 MySQL 并发测试需要本机 Docker 正常运行，并显式开启环境变量：

```bash
RUN_MYSQL_CONTAINER_TESTS=true \
TESTCONTAINERS_RYUK_DISABLED=true \
mvn -Dmaven.repo.local=$PWD/.m2-repository \
  -pl service -am \
  -Dtest=OrderMySqlContainerTest,PaymentMySqlContainerTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```


Redis 默认地址：

```text
host: localhost
port: 6379
```

连续请求两次商品详情后观察缓存：

```bash
curl -sS http://localhost:8080/api/products/1
curl -sS http://localhost:8080/api/products/1
docker exec fullstack-mall-redis redis-cli GET mall:product:detail:v1:1
docker exec fullstack-mall-redis redis-cli TTL mall:product:detail:v1:1
```

只想运行 MySQL、不连接 Redis 时：

```bash
export JWT_SECRET="$(openssl rand -base64 32)"
MALL_PRODUCT_CACHE_ENABLED=false java -jar service/target/fullstack-mall-service-0.0.1-SNAPSHOT.jar
```

如果 Docker Desktop 或本机 MySQL 暂时不可用，只想先把后端服务启动起来，可使用内存 H2 的 `local` profile。该模式会自动初始化一份演示数据，并默认关闭 Redis 商品详情缓存；数据会在应用重启后重置：

```bash
cd backend
mvn -Dmaven.repo.local=$PWD/.m2-repository -pl service -am -DskipTests package
export JWT_SECRET="$(openssl rand -base64 32)"
SERVER_PORT=18080 java -jar service/target/fullstack-mall-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```


真实 Redis 容器测试需要显式开启：

```bash
RUN_REDIS_CONTAINER_TESTS=true \
TESTCONTAINERS_RYUK_DISABLED=true \
mvn -Dmaven.repo.local=$PWD/.m2-repository \
  -pl service -am \
  -Dtest=RedisContainerTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

默认后端端口为 `8080`。如果被占用：

```bash
SERVER_PORT=18080 java -jar service/target/fullstack-mall-service-0.0.1-SNAPSHOT.jar
```

## 认证接口

注册：

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"new_user","nickname":"新用户","password":"Mall123456"}'
```

管理员演示账号：

```text
username: admin
password: Admin123456
```

登录并复制响应中的 `accessToken`：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123456"}'

ADMIN_TOKEN='粘贴 accessToken'
```

当前用户：

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 阶段 4 商品接口

管理员创建商品草稿：

```bash
curl -X POST http://localhost:8080/api/admin/products \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "categoryId": 1,
    "title": "九成新机械键盘",
    "description": "功能正常，轻微使用痕迹"
  }'
```

上架商品（阶段 5 必须先执行下方“创建 SKU”，再运行此命令）：

```bash
PRODUCT_ID='创建响应中的商品 id'

curl -X PATCH "http://localhost:8080/api/admin/products/$PRODUCT_ID/status" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"targetStatus":"ON_SALE"}'
```

管理员查询全部状态商品：

```bash
curl 'http://localhost:8080/api/admin/products?pageNum=1&pageSize=10&status=DRAFT' \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

匿名查询已上架商品：

```bash
curl 'http://localhost:8080/api/products?pageNum=1&pageSize=10'
curl "http://localhost:8080/api/products/$PRODUCT_ID"
```

公开接口会由服务端强制只查询 `ON_SALE`，不会因为前端传入其他状态而泄露草稿或下架商品。

## 阶段 5 SKU、价格与库存接口

商品现在必须存在至少一个价格大于 0、可售库存大于 0 的 SKU 才能上架。因此新建商品后的推荐顺序是：先创建 SKU，再上架商品。

创建 SKU：

```bash
PRODUCT_ID='创建响应中的商品 id'

curl -X POST "http://localhost:8080/api/admin/products/$PRODUCT_ID/skus" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "skuCode":"KEYBOARD-BLACK-87",
    "specText":"黑色 / 87 键",
    "salePrice":299.00,
    "initialStock":10
  }'
```

查询管理员 SKU：

```bash
curl "http://localhost:8080/api/admin/products/$PRODUCT_ID/skus" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

修改价格和库存时必须使用查询响应中的最新 `version`：

```bash
SKU_ID='创建响应中的 SKU id'

curl -X PATCH "http://localhost:8080/api/admin/skus/$SKU_ID/price" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"salePrice":289.00,"expectedVersion":0}'

curl -X PATCH "http://localhost:8080/api/admin/skus/$SKU_ID/stock" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"delta":-1,"expectedVersion":1}'
```

公开查询已上架商品 SKU：

```bash
curl "http://localhost:8080/api/products/$PRODUCT_ID/skus"
```

## 阶段 6 购物车接口

普通用户登录并复制响应中的 `accessToken`：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"xiaoming","password":"Mall123456"}'

USER_TOKEN='粘贴 accessToken'
```

加入购物车。种子数据中用户 1 已有 SKU 1，因此这里使用 SKU 4 演示新增：

```bash
curl -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"skuId":4,"quantity":1}'
```

查询当前登录用户的购物车：

```bash
curl http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $USER_TOKEN"
```

`CartAddRequest` 不接收 `userId`。数据归属由服务端从 JWT 对应的当前用户中获取；查询也会在 SQL 条件中限制 `user_id`，不能依赖前端过滤。

## 阶段 7 订单、事务与库存锁定接口

创建订单会结算当前用户 `checked=1` 的购物车行。服务端会重新查询商品、SKU、价格和库存，不接收前端提交的金额。

```bash
IDEMPOTENCY_KEY="order-$(date +%s)"

curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY"
```

重复使用相同 `Idempotency-Key`，应返回同一张订单，不重复锁库存。

```bash
ORDER_ID='创建响应中的订单 id'

curl "http://localhost:8080/api/orders/$ORDER_ID" \
  -H "Authorization: Bearer $USER_TOKEN"

curl -X PATCH "http://localhost:8080/api/orders/$ORDER_ID/cancel" \
  -H "Authorization: Bearer $USER_TOKEN"
```

取消仅允许从 `PENDING_PAYMENT` 进入 `CANCELLED`，并在同一事务中释放锁定库存。完整后端讲解见 `docs/backend-basics/phase-7-order-transaction-stock-lock.md`。

## 阶段 8 模拟支付、回调与超时关单接口

为当前用户的待支付订单创建支付流水。重复调用会返回同一条支付流水，不会为同一订单重复创建：

```bash
ORDER_ID='创建订单响应中的 id'

curl -X POST "http://localhost:8080/api/orders/$ORDER_ID/payments" \
  -H "Authorization: Bearer $USER_TOKEN"
```

查询当前用户订单的支付流水：

```bash
curl "http://localhost:8080/api/orders/$ORDER_ID/payment" \
  -H "Authorization: Bearer $USER_TOKEN"
```

保存创建支付响应中的 `paymentNo`，再发送本地模拟支付成功回调：

```bash
PAYMENT_NO='创建支付响应中的 paymentNo'

curl -X POST http://localhost:8080/api/mock-payments/callback \
  -H 'Content-Type: application/json' \
  -H 'X-Mock-Payment-Secret: your-local-mock-payment-secret' \
  -d "{
    \"paymentNo\": \"$PAYMENT_NO\",
    \"callbackId\": \"callback-success-001\",
    \"providerTransactionNo\": \"provider-txn-001\",
    \"result\": \"SUCCESS\"
  }"
```

回调接口不使用用户 JWT，因为调用方模拟的是外部支付平台；它会校验 `X-Mock-Payment-Secret`。重复发送同一成功回调时，订单、支付和库存不会重复处理。

默认订单支付有效期为 15 分钟，后台任务默认在上一次扫描结束 60 秒后再次扫描过期订单。完整字段、分层、悲观锁、幂等和定时任务讲解见 `docs/backend-basics/phase-8-payment-callback-timeout-close.md`。

## 其他接口

健康检查：

```bash
curl http://localhost:8080/api/health
```

阶段 1 分类草稿校验：

```bash
curl -X POST http://localhost:8080/api/categories/validate \
  -H 'Content-Type: application/json' \
  -d '{"name":"  手机数码  ","description":"二手数码设备","sortOrder":10}'
```

管理员用户列表：

```bash
curl 'http://localhost:8080/api/users?pageNum=1&pageSize=10' \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

OpenAPI：

```text
http://localhost:8080/v3/api-docs
http://localhost:8080/swagger-ui.html
```

## 本地配置

MySQL：

```text
host: localhost
port: 3308
database: fullstack_mall
username: mall
password: mall_dev_password
```

可覆盖环境变量：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION_SECONDS
MOCK_PAYMENT_CALLBACK_SECRET
ORDER_PAYMENT_TIMEOUT_MINUTES
ORDER_CLOSE_SCAN_FIXED_DELAY_MS
ORDER_CLOSE_SCAN_BATCH_SIZE
REDIS_HOST
REDIS_PORT
REDIS_TIMEOUT
MALL_PRODUCT_CACHE_ENABLED
MALL_PRODUCT_CACHE_VALUE_TTL
MALL_PRODUCT_CACHE_NULL_TTL
SERVER_PORT
```

`application.yml` 不再内置 JWT 密钥；本地启动请通过环境变量注入 `JWT_SECRET`，例如 `export JWT_SECRET="$(openssl rand -base64 32)"`。真实环境必须通过安全的环境变量或密钥服务注入。

## 学习文档

建议先按下面顺序阅读 3 份入口文档：

- `docs/backend-basics/backend-foundation/README.md`：从 Vue 前端到 Java 后端的连续基础教程，按 Java、Spring Boot、MySQL、MyBatis-Plus、Redis、Security、完整请求链路和开发流程拆成 8 篇；
- `docs/backend-basics/java-spring-mybatis-syntax-guide.md`：已经知道要查什么时，用作 Java、Lombok、Spring Boot、MyBatis-Plus 和 Security 语法速查；
- `docs/backend-basics/company-style-alignment.md`：查看当前项目采用的普通 class、`@Data`、`@Resource`、`I...Facade + ...Facade`、`BaseMapper + ServiceImpl` 等企业常见编码约定。

然后按阶段阅读：

- `docs/backend-basics/phase-0-backend-bootstrap.md`
- `docs/backend-basics/phase-1-api-foundation.md`
- `docs/backend-basics/phase-2-mysql-mybatis-plus.md`
- `docs/backend-basics/phase-3-auth-rbac.md`
- `docs/backend-basics/phase-4-product-lifecycle.md`
- `docs/backend-basics/phase-5-sku-inventory-concurrency.md`
- `docs/backend-basics/phase-6-cart-module-collaboration.md`
- `docs/backend-basics/phase-7-order-transaction-stock-lock.md`
- `docs/backend-basics/phase-8-payment-callback-timeout-close.md`
- `docs/backend-basics/phase-9-redis-cache-performance.md`

最后使用通用操作指南练习：

- `docs/backend-basics/add-api-guide.md`
- `docs/backend-basics/add-field-guide.md`
- `docs/backend-basics/add-secured-api-guide.md`

推荐路线：先顺序完成 `backend-foundation/` 连续教程，建立完整后端地图；再结合阶段 0～9 阅读真实代码；遇到具体语法时查基础语法手册；自己扩展功能时使用 3 个通用操作指南。阶段 5 重点理解 `BigDecimal`、原子条件 UPDATE 和 `@Version`，阶段 6 重点理解可信身份、用户隔离、跨模块批量组装和 N+1，阶段 7 重点理解 `@Transactional`、订单快照、原子库存锁定、幂等键和状态机，阶段 8 重点理解支付回调、`SELECT ... FOR UPDATE`、双状态机和超时关单，阶段 9 重点理解 Redis、Cache Aside、TTL、空值缓存、缓存失效和 fail-open。

# backup-fullstack-mall
