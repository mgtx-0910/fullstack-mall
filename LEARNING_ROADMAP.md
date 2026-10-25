# 学习路线：前端工程师转后端（基于 fullstack-mall）

这份路线对应掘金专栏《（前端转全栈）》第 01～09 篇，并把每一篇落到本仓库**真实存在的文件**上。

核心原则只有一句：**永远从"用户动作"开始读代码，不要从"框架名词"开始。**
比如学商品详情，不要先问"MyBatis-Plus 的 Wrapper 有几种写法"，而是先还原动作：
用户打开商品详情页 → 前端 `GET /api/products/{id}` → `ProductController.detail()` → `IProductFacade.getPublishedProduct()` → 缓存 / 数据库 → JSON。

阅读顺序固定为：

```text
Controller（HTTP 入口）
  → I*Facade 接口（业务能力清单，在 contract 模块）
  → *Facade 实现（业务编排，在 service 模块）
  → CacheService / DbService（数据从哪来）
  → Mapper / Entity（怎么落到表）
  → sql/01_schema.sql（字段和约束到底是什么）
```

---

## 阶段总览

| 阶段 | 主题 | 对应文章 | 核心文件 | 学完你能回答 |
| --- | --- | --- | --- | --- |
| 0 | 环境与本地基础设施 | — | `docker-compose.dev.yml`、`sql/` | 后端为什么不能只靠 `pnpm dev` 跑起来 |
| 1 | 工程地图 | 01 | `backend/pom.xml`、`backend/contract`、`backend/service` | `contract` 和 `service` 为什么分开 |
| 2 | Java / Maven / Spring Boot 启动 | 02 | `FullstackMallApplication.java`、`application.yml` | 后端服务是怎么"跑起来并常驻"的 |
| 3 | Controller / Request / Response | 03 | `ProductController.java`、`contract/common/*` | 一个 Axios 请求进后端之后被谁接住 |
| 4 | MySQL 持久化 | 04 | `sql/01_schema.sql`、`*_Entity.java` | 为什么前端状态不够用，必须有数据库 |
| 5 | MyBatis-Plus | 05 | `*Mapper.java`、`*DbService.java`、`MybatisPlusConfig.java` | 后端不手写一堆 SQL 怎么操作数据库 |
| 6 | JWT / Spring Security | 06 | `auth/`、`security/`、`SecurityConfig.java` | 401 和 403 到底差在哪 |
| 7 | Redis 缓存 | 07 | `cache/RedisOperatorClient.java`、`product/cache/*` | 为什么改完商品要删缓存，Redis 挂了怎么办 |
| 8 | 商品详情完整链路 | 08 | `ProductFacade.java` + 上述各层 | 一次请求从 HTTP 到 JSON 经过了什么 |
| 9 | 状态机 / 事务 / 幂等 | 09 | `ProductStatus`、`order/*TransactionService`、`payment/*` | 商品为什么不能随便上下架，支付回调重复怎么办 |
| 10 | 进阶：购物车、订单、支付、超时关单 | — | `cart/`、`order/OrderCloseScheduler.java` | 并发、重复请求、外部回调怎么兜住 |

---

## 阶段 0：环境与本地基础设施

**目标**：理解后端项目为什么需要 Docker。

前端 `pnpm dev` 就能跑，因为页面不一定依赖本地数据库；后端必须连 MySQL 和 Redis。

```bash
docker compose -f docker-compose.dev.yml up -d mysql redis
```

- MySQL 8.4 → 本机 `3308`（避免和你已有的 3306 冲突）
- Redis 7.4 → 本机 `6379`
- 首次启动会自动执行 `sql/01_schema.sql`、`sql/02_seed.sql`

**自查**：MySQL 没启动时，商品接口会怎样？Redis 没启动时，商品详情接口应该失败还是降级？（答案见阶段 7 的 fail-open）

---

## 阶段 1：工程地图（第 01 篇）

**目标**：建立整体地图，不急着写业务。

```text
fullstack-mall/
├── backend/
│   ├── pom.xml            # 父工程：Java 17、Spring Boot 版本、模块关系（≈ package.json，但还管编译/测试/打包）
│   ├── contract/          # 对外契约：Request / Response / enum / I*Facade（前端最该先看这里）
│   └── service/           # 可运行服务：Controller / Facade / DbService / Mapper / Cache / Security
├── sql/                   # 01_schema.sql 建表、02_seed.sql 演示数据
└── docker-compose.dev.yml # 本地 MySQL + Redis
```

前端 ↔ 后端对照：

| 前端熟悉的概念 | 后端对应 | 本仓库位置 | 关键区别 |
| --- | --- | --- | --- |
| `package.json` | `pom.xml` | `backend/pom.xml` | Maven 还负责编译、测试、打包、多模块 |
| `main.ts` | Spring Boot 启动类 | `service/FullstackMallApplication.java` | 启动后常驻进程，监听 8080 |
| Vue Router | Controller URL 映射 | `product/ProductController.java` | 面对的是不可信的网络输入 |
| Axios 请求类型 | Request DTO | `contract/product/ProductQueryRequest.java` | 运行时真实对象，参与绑定和校验 |
| Axios 响应类型 | Response DTO | `contract/product/ProductDetailResponse.java` | 会真实参与 JSON 序列化 |
| `api/product.ts` | Facade 接口 | `contract/product/IProductFacade.java` | 定义"后端能提供什么能力" |
| 状态管理 store | MySQL / Redis | — | 服务所有用户，可持久化 |

**自查**：`backend/contract` 和 `backend/service` 的区别是什么？为什么不能把业务逻辑写在 Controller 里？

---

## 阶段 2：Java / Maven / Spring Boot 启动（第 02 篇）

**核心文件**

- `backend/service/src/main/java/com/example/fullstackmall/service/FullstackMallApplication.java`：启动类
- `backend/service/src/main/resources/application.yml`：配置（`${DB_URL:默认值}` 这种写法就是"环境变量覆盖"）
- `backend/service/src/main/resources/application-local.yml`、`application-debug.yml`：本地 / 调试 profile

**动手**

```bash
cd backend
mvn clean package                      # 编译 + 跑测试 + 打包
mvn -pl service -am spring-boot:run    # 只跑 service，并自动构建它依赖的 contract
```

启动后：

```bash
curl -sS http://localhost:8080/api/health
```

**自查**：`@SpringBootApplication` 做了什么？为什么业务对象都用 `@Resource` 注入而不是自己 `new`？
（因为对象可能涉及事务代理、安全代理、配置注入、生命周期，交给 Spring 容器统一管理。）

---

## 阶段 3：Controller / Request / Response（第 03 篇）

**核心文件**

- `service/product/ProductController.java`：`GET /api/products`、`GET /api/products/{id}`
- `service/product/ProductAdminController.java`：`/api/admin/products`
- `contract/product/ProductQueryRequest.java` / `ProductDetailResponse.java` / `ProductSummaryResponse.java`
- `contract/common/ApiResponse.java`、`ApiCode.java`、`PageResponse.java`
- `service/common/exception/GlobalExceptionHandler.java`：统一异常 → 统一响应
- `service/common/trace/TraceIdFilter.java`、`TraceIdContext.java`：traceId

统一响应长这样：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": { "id": 1, "name": "示例商品" },
  "traceId": "b3f1c0..."
}
```

**前端视角的重点**：`ApiCode` 是稳定业务码，前端要按 `code` 分支，不要解析 `message` 文案。

**失败契约**（后端必须定义的部分）：

| 场景 | code | HTTP |
| --- | --- | --- |
| 参数不合法 | `VALIDATION_ERROR` | 400 |
| 没登录 / Token 失效 | `UNAUTHORIZED` | 401 |
| 登录了但没权限 | `FORBIDDEN` | 403 |
| 商品不存在 | `PRODUCT_NOT_FOUND` | 404 |
| 库存不足 | `INSUFFICIENT_STOCK` | 409/400 |
| 未知异常 | `INTERNAL_ERROR` | 500 |

**自查**：`Request`、`Response`、`Entity` 三者字段很像，为什么不能合并成一个？
（Request/Response 面向接口，Entity 面向数据库表；比如 `createdBy`、`deleted` 就不该出现在公开 Response 里。）

---

## 阶段 4：MySQL 持久化（第 04 篇）

**核心文件**：`sql/01_schema.sql`、`sql/02_seed.sql`、各模块 `entity/*Entity.java`

前端状态刷新就没了，后端数据要服务所有用户且可持久化。**数据库是业务规则的最终保障**：

- `mall_user.username` 唯一索引 → 不能有两个同名用户（光靠代码查一遍在并发下不可靠）
- `mall_order.order_no` 唯一索引 → 支付回调幂等的基础
- `mall_payment.trade_no` 唯一索引 → 同一笔支付重复回调只处理一次
- `mall_cart_item (user_id, sku_id)` 唯一索引 → 不会重复加购
- 金额字段用 `DECIMAL`，不要用 `FLOAT/DOUBLE`

**铁律**：前端表单校验只是体验优化，真正可靠的校验必须在后端 + 数据库约束里落地，
因为任何人都可以绕过页面直接调接口。

**自查**：为什么"存在但未上架"的商品，公开详情接口也返回 404？

---

## 阶段 5：MyBatis-Plus（第 05 篇）

**核心文件**

- `service/product/mapper/ProductMapper.java`：继承 `BaseMapper<ProductEntity>`，单表 CRUD 零 SQL
- `service/product/service/ProductDbService.java`：把底层操作包装成业务语义方法
- `service/config/MybatisPlusConfig.java`：分页拦截器（没有它，`selectPage` 会退化成全表查再内存分页）

三层职责：

```text
Entity   → 数据库一行在 Java 里长什么样
Mapper   → SQL / CRUD 的执行入口
DbService → 按业务语义读写（比如"只查已上架商品"这件事只写一次）
```

**自查**：为什么公开查询要统一走 `findPublishedById` 这类方法，而不是每个 Controller 自己拼 `status` 条件？

---

## 阶段 6：JWT 与 Spring Security（第 06 篇）

**核心文件**

- `service/auth/AuthController.java`、`AuthFacade.java`、`AuthService.java`、`JwtTokenService.java`
- `service/security/JwtAuthenticationFilter.java`：从 `Authorization: Bearer <token>` 解出登录人
- `service/security/SecurityConfig.java`：谁能访问哪个接口
- `service/security/CurrentUserService.java`、`CurrentUserPrincipal.java`：取当前登录人
- `service/security/RestAuthenticationEntryPoint.java`（401）、`RestAccessDeniedHandler.java`（403）
- `contract/auth/UserRole.java`：普通用户 / 管理员

一句话分工：**前端负责携带 Token，后端负责验证 Token 并决定能不能访问。**

```bash
# 登录拿 accessToken
curl -sS -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123456"}'

# 带 Token 访问管理端接口
curl -sS -X POST http://localhost:8080/api/admin/products \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' -d '{...}'
```

**401 还是 403**：401 = 你是谁？403 = 我知道你是谁，但你没权限。

**铁律**："谁在操作"从 Token 里取，不从请求体里取，否则前端改一个字段就能伪造操作人。

---

## 阶段 7：Redis 缓存（第 07 篇）

**核心文件**

- `service/cache/RedisOperatorClient.java`：对 Redis 操作做统一封装 + 异常兜底
- `service/product/cache/ProductDetailCacheService.java`：Cache Aside 的实现（key / JSON / TTL / 空值 / 失效 / 降级）
- `ProductDetailCacheLookup.java`、`ProductDetailCacheStatus.java`、`ProductDetailCacheProperties.java`

关键设计，逐个都能在源码里找到：

1. **key 有版本前缀**：`mall:product:detail:v1:{id}`，将来改缓存结构可以平滑切 v2
2. **写后失效**：改完商品立刻 `evict`，否则前端一直看到旧数据
3. **空值缓存**：`__NULL__:PRODUCT_NOT_FOUND` + 短 TTL（默认 30s），缓解缓存穿透
4. **坏 JSON 自愈**：反序列化失败会删掉脏 key 再回源，避免 TTL 内一直失败
5. **fail-open**：Redis 挂了返回 `DEGRADED`，Facade 继续查 MySQL，接口不跟着挂

相关配置（`application.yml`）：

```yaml
mall.cache.product-detail:
  enabled: ${MALL_PRODUCT_CACHE_ENABLED:true}
  value-ttl: ${MALL_PRODUCT_CACHE_VALUE_TTL:10m}
  null-ttl: ${MALL_PRODUCT_CACHE_NULL_TTL:30s}
```

**自查**：Redis 是数据库吗？（不是。它是性能层，业务真相永远在 MySQL。）

---

## 阶段 8：商品详情完整链路（第 08 篇）

把前几个阶段串成一条线：

```text
浏览器 / curl  GET /api/products/1
   ↓
TraceIdFilter                    生成 traceId，写进日志 MDC 和响应头
   ↓
Spring Security 过滤器链         公开 GET 放行；管理端校验 Token 和角色
   ↓
ProductController.detail()       接住 HTTP 参数，不写业务
   ↓
IProductFacade.getPublishedProduct()
   ↓
ProductFacade                    业务编排：先查缓存 → 未命中/降级再查库 → 回填缓存
   ↓
ProductDetailCacheService  ←→  Redis
   ↓
ProductDbService → ProductMapper → MySQL(mall_product / mall_category / mall_sku)
   ↓
Entity → ProductDetailResponse → ApiResponse(code/message/data/traceId) → JSON
   ↓
出错则由 GlobalExceptionHandler 转成统一 ApiResponse
```

**动手验证**

```bash
curl -i http://localhost:8080/api/products/1          # 看响应头里的 X-Trace-Id
curl -i http://localhost:8080/api/products/999999     # 看 404 + PRODUCT_NOT_FOUND
docker compose -f docker-compose.dev.yml stop redis   # 停掉 Redis 再请求，验证 fail-open
```

---

## 阶段 9：状态机、事务与幂等（第 09 篇）

**核心文件**

- `contract/product/ProductStatus.java`：`DRAFT` / `ON_SALE` / `OFF_SHELF`
- `contract/product/ProductStatusChangeRequest.java`、`ProductAdminController.java`
- `contract/order/OrderStatus.java`：`PENDING_PAYMENT` → `PAID` / `CANCELLED` / `CLOSED`
- `service/order/OrderCreateTransactionService.java`：下单 = 写订单 + 写明细 + 锁库存，一起成功或一起失败
- `service/order/OrderCancelTransactionService.java`、`OrderCloseTransactionService.java`：取消 / 超时关单要释放库存
- `service/order/OrderCloseScheduler.java`、`OrderCloseBatchService.java`：定时扫描超时未支付订单
- `service/payment/PaymentCallbackTransactionService.java`：回调幂等
- `contract/common/ApiCode.java`：`INVALID_PRODUCT_STATUS_TRANSITION`、`ORDER_STATUS_CONFLICT`、`PAYMENT_CALLBACK_DUPLICATE`、`INVENTORY_VERSION_CONFLICT` …

**为什么商品不能随便上下架**：状态一旦随意流转，就会出现"下架商品还能被下单""草稿商品出现在公开列表"。
上架还有前置条件：至少有一个**有价格且有库存**的 SKU（`PRODUCT_NOT_READY_FOR_SALE`）。

**为什么订单要事务**：写订单、写明细、锁库存必须原子，任何一步失败都要回滚。

**为什么支付回调要幂等**：网络重试、用户重复点击都会导致同一笔回调到达多次，
靠 `trade_no` 唯一索引 + 回调事件 ID 去重（`PAYMENT_CALLBACK_DUPLICATE`）。

**为什么要用状态条件更新**：`UPDATE ... WHERE status = PENDING_PAYMENT`，
并发下只有真正改到行的那个请求算成功，这就是最朴素的乐观并发控制。

---

## 阶段 10：进阶 —— 购物车、订单、支付

- `service/cart/`：`CartController` / `CartFacade` / `CartItemDbService`；`CART_ITEM_ALREADY_EXISTS`、`CART_QUANTITY_EXCEEDS_STOCK`
- `service/inventory/`：`SkuAdminController` / `SkuFacade` / `SkuDbService`；库存版本号 `INVENTORY_VERSION_CONFLICT`
- `service/order/` + `service/payment/`：下单幂等（`Idempotency-Key` 请求头）、创建支付单、模拟回调、超时关单

建议练习顺序：加购物车 → 下单（观察库存锁定）→ 创建支付单 → 模拟回调（重复发两次，观察幂等）→ 等 15 分钟看超时关单（或调小 `ORDER_PAYMENT_TIMEOUT_MINUTES`）。

---

## 每章的自查清单（通用）

1. 这个能力解决什么问题？
2. 它在本仓库哪个文件里？
3. 它出错时，前端会看到什么现象（哪个 code / 哪个状态码）？
4. 如果只测成功流程，失败流程我定义了吗？

## 常见坑

- 一上来就读订单 / 支付 / 缓存，越看越乱 → 从 `GET /api/products/{id}` 开始
- 把 Controller 当万能文件，业务和 SQL 都往里塞
- 把 DTO、Entity、数据库表混为一谈
- 以为前端校验足够安全
- 以为 Redis 是数据库
- 只看 200 成功，不定义失败
- 不看 README / 文档，直接全局搜索

## 相关资源

- 掘金专栏：前端转全栈（第 01～09 篇）
- GitHub Wiki：见 `wiki/` 目录（可直接作为仓库 Wiki 的内容源）
- 接口文档：启动后访问 `http://localhost:8080/swagger-ui.html`
