# 01 工程地图：第一次打开后端项目先看哪里

## 这一页解决什么

打开一个 Java 后端项目，`pom.xml`、`contract`、`service`、`Controller`、`Facade`、`DbService`、`Mapper`、`Entity`……
一堆新名词。这一页只做一件事：**建立地图**，让你知道每个目录解决什么问题，将来往哪条路深入。

不需要今天弄懂 Redis、事务和支付回调。

## 目录地图

```text
fullstack-mall/
├── backend/
│   ├── pom.xml            # 父工程：Java 17、Spring Boot 版本、模块关系
│   ├── contract/          # 对外契约：Request / Response / enum / I*Facade
│   └── service/           # 可运行服务：Controller / Facade / DbService / Mapper / Cache / Security
├── sql/
│   ├── 01_schema.sql      # 建表：字段、索引、唯一约束
│   └── 02_seed.sql        # 演示数据
├── docker-compose.dev.yml # 本地 MySQL 8.4(3308) + Redis 7.4(6379)
├── README.md
└── LEARNING_ROADMAP.md
```

## 前端 ↔ 后端对照表

| 前端熟悉的概念 | 后端对应 | 本仓库位置 | 关键区别 |
| --- | --- | --- | --- |
| `package.json` | `pom.xml` | `backend/pom.xml` | Maven 还负责编译、测试、打包、多模块 |
| `main.ts` | Spring Boot 启动类 | `service/FullstackMallApplication.java` | 启动后是常驻进程，监听 8080 |
| Vue Router | Controller URL 映射 | `product/ProductController.java` | 面对不可信的网络输入，要校验、鉴权 |
| Axios 请求类型 | Request DTO | `contract/product/ProductQueryRequest.java` | 运行时真实对象，参与绑定和校验 |
| Axios 响应类型 | Response DTO | `contract/product/ProductDetailResponse.java` | 会真实参与 JSON 序列化 |
| `api/product.ts` | Facade 接口 | `contract/product/IProductFacade.java` | 定义"后端能提供什么能力" |
| composable / service | Facade 实现 | `service/product/ProductFacade.java` | 做权限、编排、转换、事务边界 |
| 状态管理 store | MySQL / Redis | — | 服务所有用户，可持久化 |
| mock data | seed SQL | `sql/02_seed.sql` | 必须符合数据库约束 |

## 分层职责，一句话记住一层

```text
Controller     我接到了什么 HTTP 请求？
Request/Response 前端能传什么，后端返回什么？
Facade         这个业务用例要怎么完成？
CacheService   能不能先从缓存拿，什么时候失效？
DbService      我要按业务语义读写哪些数据？
Mapper         底层 SQL / CRUD 怎么执行？
Entity         数据库一行在 Java 里长什么样？
SQL            表结构和约束最终是什么？
*TransactionService 多个表要不要一起成功或一起失败？
```

## 建议的第一天阅读清单

1. `README.md`：项目是什么、已实现到哪个阶段
2. `backend/pom.xml`：Java 版本、Spring Boot 版本、模块拆分
3. `backend/service/pom.xml`：这个服务具备哪些后端能力（Web / Security / Redis / MyBatis-Plus / JWT …）
4. `service/product/ProductController.java`：最简单的公开 GET 入口
5. `contract/product/IProductFacade.java`：商品模块对外能力清单
6. `service/product/ProductFacade.java`：业务编排
7. 需要查库时再看 `service/product/service/ProductDbService.java`、`mapper/ProductMapper.java`、`entity/ProductEntity.java`
8. 需要确认字段时再看 `sql/01_schema.sql`

不要第一天就从 `01_schema.sql` 头背到尾，也不要第一天就深挖 Redis 异常分支。

## 自查题

- `backend/contract` 和 `backend/service` 的区别是什么？
- `pom.xml`、`application.yml`、`docker-compose.dev.yml` 分别解决什么问题？
- 顺着 `GET /api/products/{id}` 能找到 `ProductController.detail()` 和 `IProductFacade.getPublishedProduct()` 吗？
- 为什么业务逻辑不能堆在 Controller 里？
