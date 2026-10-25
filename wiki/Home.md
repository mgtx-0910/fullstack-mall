# fullstack-mall Wiki

一个用于学习后端电商业务开发的本地全栈示例项目：Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL 8.4 + Redis 7.4。

这个 Wiki 面向**前端转后端**的同学：所有内容都落到仓库里真实存在的文件，你可以边读边打开对应源码对照。

## 从哪开始

1. 还没跑起来？先看 [02-Maven-SpringBoot](https://github.com/mgtx-0910/fullstack-mall/wiki/02-Maven-SpringBoot) 把服务跑起来，再回来读。
2. 第一次打开后端项目？先看 [01-Project-Map](https://github.com/mgtx-0910/fullstack-mall/wiki/01-Project-Map)，建立工程地图。
3. 想按篇学？按 [Roadmap](https://github.com/mgtx-0910/fullstack-mall/wiki/Roadmap) 的阶段 0 → 10 顺序走。
4. 名词看不懂？查 [Glossary](https://github.com/mgtx-0910/fullstack-mall/wiki/Glossary)（前端 ↔ 后端术语对照）。
5. 卡住了？看 [FAQ](https://github.com/mgtx-0910/fullstack-mall/wiki/FAQ)。

## 一条最重要的阅读规则

**永远从"用户动作"开始读代码，不要从"框架名词"开始。**

比如学商品详情：不要先问"MyBatis-Plus 的 Wrapper 有几种写法"，
而是从"用户打开商品详情页 → `GET /api/products/{id}`"出发，沿着调用链走：

```text
Controller → I*Facade → *Facade → CacheService / DbService → Mapper → MySQL → Response → JSON
```

每碰到一个不懂的概念，只问三个问题：

1. 它解决什么问题？
2. 它在本仓库哪个文件里？
3. 它出错时，前端会看到什么？

## 目录

- [Roadmap](https://github.com/mgtx-0910/fullstack-mall/wiki/Roadmap) —— 阶段 0～10 学习路线总表
- [01 工程地图](https://github.com/mgtx-0910/fullstack-mall/wiki/01-Project-Map)
- [02 Maven 与 Spring Boot 启动](https://github.com/mgtx-0910/fullstack-mall/wiki/02-Maven-SpringBoot)
- [03 Controller / Request / Response](https://github.com/mgtx-0910/fullstack-mall/wiki/03-Controller-Request-Response)
- [04 MySQL 持久化](https://github.com/mgtx-0910/fullstack-mall/wiki/04-MySQL-Persistence)
- [05 MyBatis-Plus](https://github.com/mgtx-0910/fullstack-mall/wiki/05-MyBatis-Plus)
- [06 JWT 与 Spring Security](https://github.com/mgtx-0910/fullstack-mall/wiki/06-JWT-Security)
- [07 Redis 缓存](https://github.com/mgtx-0910/fullstack-mall/wiki/07-Redis-Cache)
- [08 商品详情完整链路](https://github.com/mgtx-0910/fullstack-mall/wiki/08-Product-Detail-Pipeline)
- [09 状态机 / 事务 / 幂等](https://github.com/mgtx-0910/fullstack-mall/wiki/09-State-Machine-Transaction)
- [10 购物车 / 订单 / 支付](https://github.com/mgtx-0910/fullstack-mall/wiki/10-Cart-Order-Payment)
- [Glossary 术语对照](https://github.com/mgtx-0910/fullstack-mall/wiki/Glossary)
- [FAQ 常见问题](https://github.com/mgtx-0910/fullstack-mall/wiki/FAQ)

仓库根目录还有一份 **[LEARNING_ROADMAP.md](https://github.com/mgtx-0910/fullstack-mall/blob/main/LEARNING_ROADMAP.md)**，内容与该路线一致，方便离线阅读。
