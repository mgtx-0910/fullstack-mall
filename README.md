# fullstack-mall

一个用于学习后端电商业务开发的本地全栈示例项目。当前公开仓库主要包含 Java 后端服务、数据库初始化 SQL 和本地开发用 Docker Compose 配置。

## 仓库内容

```text
fullstack-mall/
├── backend/                 # Spring Boot 后端工程
│   ├── contract/             # 对外请求、响应、枚举和 Facade 接口定义
│   └── service/              # Web Controller、业务实现、MyBatis-Plus、Redis 缓存等
├── sql/                     # MySQL 初始化建表和演示数据
├── wiki/                    # GitHub Wiki 内容源（可直接同步到仓库 Wiki）
├── docker-compose.dev.yml   # 本地 MySQL + Redis 开发环境
├── LEARNING_ROADMAP.md      # 学习路线（前端转全栈，对应专栏第 01～09 篇）
├── .gitignore
└── README.md
```

> 代码来源于原仓库 <https://github.com/2530622506/fullstack-mall>（掘金专栏《（前端转全栈）》示例项目）。
> 本仓库在此基础上新增了 `LEARNING_ROADMAP.md` 学习路线和 `wiki/` 目录，作为学习过程中的笔记与导航。
> 公开仓库中没有包含个人开发记录、AI 工作区、IDE 配置等本地内容。

## 技术栈

- Java 17
- Spring Boot
- Spring Web
- Spring Security
- JWT
- MyBatis-Plus
- MySQL 8.4
- Redis 7.4
- Maven
- Docker Compose
- JUnit / Spring Boot Test

## 已包含的主要业务能力

- 健康检查接口
- 用户注册、登录、当前用户查询
- JWT 鉴权
- 用户角色区分：普通用户 / 管理员
- 商品分类草稿管理
- 商品创建、上下架、查询、详情
- SKU 库存与价格维护
- 购物车添加、查询、移除
- 下单、订单查询、订单状态流转
- 模拟支付创建、支付回调、订单支付成功处理
- 商品详情 Redis Cache Aside 缓存
- 基于 MySQL 的初始化表结构和演示数据

## 快速启动

### 1. 启动本地依赖

项目依赖 MySQL 和 Redis。可以直接使用仓库内的 Docker Compose 文件启动：

```bash
docker compose -f docker-compose.dev.yml up -d mysql redis
```

如果需要重置本项目自己的本地数据库数据，可以执行：

```bash
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up -d mysql redis
```

> `down -v` 会删除本项目 Docker volume 中的 MySQL / Redis 数据，不要对非本项目环境执行。

### 2. 编译后端

```bash
cd backend
mvn clean package
```

### 3. 启动后端服务

公开仓库不内置 JWT 密钥。本地启动前需要设置 `JWT_SECRET`，可以临时生成一个：

```bash
export JWT_SECRET="$(openssl rand -base64 32)"
java -jar service/target/fullstack-mall-service-0.0.1-SNAPSHOT.jar
```

服务默认启动端口：

```text
http://localhost:8080
```

Swagger UI：

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON：

```text
http://localhost:8080/v3/api-docs
```

## 本地配置

默认 MySQL 配置：

```text
host: localhost
port: 3308
database: fullstack_mall
username: mall
password: mall_dev_password
```

默认 Redis 配置：

```text
host: localhost
port: 6379
```

可通过环境变量覆盖：

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

## 演示账号

初始化 SQL 中包含演示账号，方便本地调试：

```text
普通用户：xiaoming / Mall123456
管理员：admin / Admin123456
```

这些账号仅用于本地学习和演示，不要用于真实环境。

## 常用接口示例

### 健康检查

```bash
curl -sS http://localhost:8080/api/health
```

### 管理员登录

```bash
curl -sS -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123456"}'
```

登录后复制响应中的 `accessToken`：

```bash
ADMIN_TOKEN='粘贴 accessToken'
```

### 查询商品列表

```bash
curl -sS 'http://localhost:8080/api/products?pageNo=1&pageSize=10'
```

### 查询商品详情

```bash
curl -sS http://localhost:8080/api/products/1
```

### 创建商品

```bash
curl -sS -X POST http://localhost:8080/api/admin/products \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"示例商品",
    "subtitle":"本地演示商品",
    "categoryId":1,
    "mainImage":"https://example.com/product.png",
    "price":1999
  }'
```

### 普通用户登录

```bash
curl -sS -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"xiaoming","password":"Mall123456"}'
```

登录后复制响应中的 `accessToken`：

```bash
USER_TOKEN='粘贴 accessToken'
```

### 添加购物车

```bash
curl -sS -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"skuId":1,"quantity":1}'
```

### 查询购物车

```bash
curl -sS http://localhost:8080/api/cart \
  -H "Authorization: Bearer $USER_TOKEN"
```

### 创建订单

```bash
curl -sS -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Idempotency-Key: demo-order-001'
```

### 创建支付单

```bash
curl -sS -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"orderId":1}'
```

### 模拟支付回调

```bash
curl -sS -X POST http://localhost:8080/api/payments/mock-callback \
  -H 'X-Mock-Payment-Secret: your-local-mock-payment-secret' \
  -H 'Content-Type: application/json' \
  -d '{
    "paymentNo":"PAYMENT_NO_FROM_CREATE_PAYMENT_RESPONSE",
    "success":true,
    "callbackRequestNo":"callback-demo-001"
  }'
```

## 测试

运行单元测试和 H2 集成测试：

```bash
cd backend
mvn clean test
```

部分测试需要本机 Docker 环境。需要显式开启对应环境变量后再运行：

```bash
RUN_MYSQL_CONTAINER_TESTS=true \
TESTCONTAINERS_RYUK_DISABLED=true \
mvn -pl service -am \
  -Dtest=OrderMySqlContainerTest,PaymentMySqlContainerTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Redis 容器测试：

```bash
RUN_REDIS_CONTAINER_TESTS=true \
TESTCONTAINERS_RYUK_DISABLED=true \
mvn -pl service -am \
  -Dtest=RedisContainerTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

## 学习路线（前端转全栈）

如果你是前端工程师、第一次打开一个 Spring Boot 项目，建议按下面的顺序读，而不是从最复杂的订单 / 支付开始：

```text
Controller（HTTP 入口）
  → I*Facade 接口（业务能力清单，在 backend/contract）
  → *Facade 实现（业务编排，在 backend/service）
  → CacheService / DbService（数据从哪来）
  → Mapper / Entity（怎么落到表）
  → sql/01_schema.sql（字段和约束到底是什么）
```

| 阶段 | 主题 | 核心文件 | 学完你能回答 |
| --- | --- | --- | --- |
| 0 | 环境与基础设施 | `docker-compose.dev.yml`、`sql/` | 后端为什么不能只靠一条命令跑起来 |
| 1 | 工程地图 | `backend/pom.xml`、`contract/`、`service/` | `contract` 和 `service` 为什么分开 |
| 2 | Maven / Spring Boot 启动 | `FullstackMallApplication.java`、`application.yml` | 服务是怎么跑起来并常驻的 |
| 3 | Controller / Request / Response | `product/ProductController.java`、`contract/common/*` | Axios 请求进后端后被谁接住 |
| 4 | MySQL 持久化 | `sql/01_schema.sql`、`entity/*Entity.java` | 为什么前端状态不够用 |
| 5 | MyBatis-Plus | `*Mapper.java`、`*DbService.java` | 不手写一堆 SQL 怎么操作数据库 |
| 6 | JWT / Spring Security | `auth/`、`security/`、`SecurityConfig.java` | 401 和 403 差在哪 |
| 7 | Redis 缓存 | `cache/RedisOperatorClient.java`、`product/cache/*` | 改完商品为什么要删缓存 |
| 8 | 商品详情完整链路 | `ProductFacade.java` + 上述各层 | 一次请求从 HTTP 到 JSON 经过了什么 |
| 9 | 状态机 / 事务 / 幂等 | `ProductStatus`、`order/*TransactionService`、`payment/*` | 商品为什么不能随便上下架 |
| 10 | 进阶：购物车 / 订单 / 支付 / 超时关单 | `cart/`、`order/OrderCloseScheduler.java` | 并发、重复请求、外部回调怎么兜住 |

完整版见 **[LEARNING_ROADMAP.md](LEARNING_ROADMAP.md)**（对应掘金专栏《前端转全栈》第 01～09 篇，每篇都落到本仓库真实文件上）。

配套 Wiki 内容在 **[wiki/](wiki/)** 目录，可直接作为仓库 Wiki 的内容源。

## 安全说明

- 本仓库中的数据库账号、演示用户和 mock 支付密钥均为本地开发占位值。
- 公开配置不包含真实 JWT 密钥；启动时必须通过 `JWT_SECRET` 环境变量注入。
- 真实环境不要使用 README 或 SQL 中的演示账号密码。
- 真实环境建议使用独立密钥管理服务或部署平台 Secret 管理能力。

