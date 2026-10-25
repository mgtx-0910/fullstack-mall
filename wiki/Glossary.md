# Glossary：前端 ↔ 后端术语对照

| 前端概念 | 后端对应 | 说明 |
| --- | --- | --- |
| `package.json` | `pom.xml` | Maven 还负责编译、测试、打包、多模块 |
| `main.ts` | Spring Boot 启动类 | 启动后是常驻进程，监听端口 |
| Vue Router | `@RequestMapping` / `@GetMapping` | Controller 面对的是网络请求，不是页面跳转 |
| Axios 请求体类型 | Request DTO | 运行时真实对象，参与绑定与校验 |
| Axios 响应类型 | Response DTO | 会真实参与 JSON 序列化 |
| `api/product.ts` | `I*Facade` 接口 | 定义"后端能提供什么能力" |
| composable / service | `*Facade` 实现 | 权限、编排、转换、事务边界 |
| Pinia store | MySQL / Redis | 服务所有用户，可持久化 |
| mock data | seed SQL | 必须符合数据库约束 |
| 浏览器 DevTools | 日志 + traceId + curl + SQL | 后端问题常需跨 HTTP / Java / DB / 缓存定位 |
| HTTP 状态码 | `ApiCode` 业务码 | 状态码表达大类，业务码表达具体原因 |
| 前端表单校验 | Bean Validation + 数据库约束 | 前端校验只是体验，后端必须自己校验 |

## 后端独有名词

| 名词 | 一句话解释 | 本仓库落点 |
| --- | --- | --- |
| Bean | 被 Spring 容器管理的对象 | `@RestController`、`@Service`、`@Configuration` |
| 依赖注入 | 不自己 `new`，由容器注入 | `@Resource` |
| DTO | 面向接口的数据载体 | `contract/**/Request`、`Response` |
| Entity | 面向数据库表的 Java 对象 | `service/**/entity/Entity.java` |
| Mapper | SQL / CRUD 执行入口 | `service/**/mapper/*Mapper.java` |
| DbService | 按业务语义封装的数据访问 | `service/**/service/*DbService.java` |
| Facade | 业务用例编排层 | `service/**/*Facade.java` |
| TransactionService | 跨表事务边界 | `service/order/*TransactionService.java` |
| Cache Aside | 先查缓存，未命中回源再回填 | `service/product/cache/ProductDetailCacheService.java` |
| fail-open | 依赖故障时降级而不是整体失败 | 商品详情在 Redis 故障回源 MySQL |
| 写后失效 | 改库后立刻删缓存 | `ProductDetailCacheService.evict()` |
| 缓存穿透 | 反复请求不存在的 key | 短 TTL 空值缓存 |
| 幂等 | 同一请求重复到达只生效一次 | `Idempotency-Key`、`trade_no` 唯一索引 |
| 状态机 | 状态只能按规则流转 | `ProductStatus`、`OrderStatus` |
| 条件更新 | `UPDATE ... WHERE status = ?` | 并发下的乐观控制 |
| traceId | 一次请求在日志里的唯一标识 | `TraceIdFilter` / `TraceIdContext` |
| 逻辑删除 | 用 `deleted` 标记而不是真删 | MyBatis-Plus 逻辑删除配置 |

## 状态码速查

| HTTP | 含义 | 常见 code |
| --- | --- | --- |
| 400 | 参数或业务规则不通过 | `VALIDATION_ERROR`、`MALFORMED_JSON`、`INSUFFICIENT_STOCK` |
| 401 | 未登录 / Token 失效 | `UNAUTHORIZED` |
| 403 | 已登录但没权限 | `FORBIDDEN`、`ACCOUNT_DISABLED` |
| 404 | 资源不存在 | `PRODUCT_NOT_FOUND`、`ORDER_NOT_FOUND`、`NOT_FOUND` |
| 500 | 未知异常 | `INTERNAL_ERROR` |
