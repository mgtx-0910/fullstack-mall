# 学习路线（阶段 0 ～ 10）

每一阶段都对应仓库里真实的文件，建议按顺序走，不要跳到订单 / 支付 / 缓存。

| 阶段 | 主题 | 核心文件 | 学完你能回答 |
| --- | --- | --- | --- |
| 0 | 环境与基础设施 | `docker-compose.dev.yml`、`sql/` | 后端为什么不能只靠一条命令跑起来 |
| 1 | 工程地图 | `backend/pom.xml`、`backend/contract`、`backend/service` | `contract` 和 `service` 为什么分开 |
| 2 | Maven / Spring Boot 启动 | `FullstackMallApplication.java`、`application.yml` | 服务是怎么跑起来并常驻的 |
| 3 | Controller / Request / Response | `ProductController.java`、`contract/common/*` | Axios 请求进后端后被谁接住 |
| 4 | MySQL 持久化 | `sql/01_schema.sql`、`entity/*Entity.java` | 为什么前端状态不够用 |
| 5 | MyBatis-Plus | `*Mapper.java`、`*DbService.java`、`MybatisPlusConfig.java` | 不手写一堆 SQL 怎么操作数据库 |
| 6 | JWT / Spring Security | `auth/`、`security/`、`SecurityConfig.java` | 401 和 403 差在哪 |
| 7 | Redis 缓存 | `cache/RedisOperatorClient.java`、`product/cache/*` | 改完商品为什么要删缓存 |
| 8 | 商品详情完整链路 | `ProductFacade.java` + 上述各层 | 一次请求从 HTTP 到 JSON 经过了什么 |
| 9 | 状态机 / 事务 / 幂等 | `ProductStatus`、`order/*TransactionService`、`payment/*` | 商品为什么不能随便上下架 |
| 10 | 购物车 / 订单 / 支付 / 超时关单 | `cart/`、`order/OrderCloseScheduler.java` | 并发、重复请求、外部回调怎么兜住 |

## 通用自查清单

每学完一章，用这四个问题验收：

1. 这个能力解决什么问题？
2. 它在本仓库哪个文件里？
3. 它出错时，前端会看到什么（哪个 `code` / 哪个 HTTP 状态码）？
4. 我只验证了成功流程，还是把失败流程也定义了？

## 固定阅读顺序

```text
Controller（HTTP 入口）
  → I*Facade 接口（业务能力清单，在 backend/contract）
  → *Facade 实现（业务编排，在 backend/service）
  → CacheService / DbService（数据从哪来）
  → Mapper / Entity（怎么落到表）
  → sql/01_schema.sql（字段和约束到底是什么）
```

## 常见坑

- 一上来就读订单 / 支付 / 缓存，越看越乱 → 从 `GET /api/products/{id}` 开始
- 把 Controller 当万能文件，业务和 SQL 都往里塞
- 把 DTO、Entity、数据库表混为一谈
- 以为前端校验足够安全
- 以为 Redis 是数据库
- 只看 200 成功，不定义失败
- 不看 README / 文档，直接全局搜索
