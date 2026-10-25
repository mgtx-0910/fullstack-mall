# FAQ：常见问题

## 环境类

**Q：没有 Docker 能学吗？**
能。先读代码和文档（不跑接口也行）；真正联调时再装 Docker。MySQL 和 Redis 你用本机已装好的实例也可以，改 `DB_URL` / `REDIS_HOST` / `REDIS_PORT` 即可。

**Q：MySQL 为什么是 3308 不是 3306？**
`docker-compose.dev.yml` 故意映射成 3308，避免和你本机已有的 MySQL 冲突。

**Q：启动报 JWT 相关错误？**
`application.yml` 里写的是 `${JWT_SECRET}`，没有默认值。启动前必须注入：
`export JWT_SECRET="$(openssl rand -base64 32)"`。

**Q：想重置本地数据？**
```bash
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up -d mysql redis
```
注意 `down -v` 会删除本项目 volume 中的数据。

## 代码阅读类

**Q：应该从哪个文件开始读？**
从 `service/product/ProductController.java` 的 `detail()` 开始，然后按
Controller → `IProductFacade` → `ProductFacade` → 缓存 / DbService → Mapper → SQL 的顺序走。

**Q：为什么一个接口要拆这么多层？**
因为不同问题要放在不同地方解决：HTTP 参数在 Controller，业务规则在 Facade，
缓存策略在 CacheService，SQL 在 Mapper，约束在数据库。全塞进 Controller，几百行之后就没人敢改了。

**Q：`contract` 和 `service` 到底有什么区别？**
`contract` 是对外契约（Request / Response / enum / Facade 接口），前端最该先看这里；
`service` 是可运行实现（Controller / Facade 实现 / DbService / Mapper / Security）。

**Q：Request、Response、Entity 字段都差不多，能合并吗？**
不能。Request/Response 面向接口（不该暴露 `createdBy`、`deleted`），Entity 面向数据库表。

## 运行与调试类

**Q：接口返回 401 怎么办？**
先确认带了 `Authorization: Bearer <token>`，再确认 Token 没过期（默认 1800 秒，`JWT_EXPIRATION_SECONDS`），
最后确认访问路径是否需要管理员角色。

**Q：接口返回 403 但我是管理员？**
确认账号的 role 是管理员（演示账号 `admin`），并确认 Token 是登录后重新取的。

**Q：改了商品，详情接口还是旧数据？**
看是否有缓存未失效。正常链路是"更新数据库 + `evict` 缓存"；
排查可用 `redis-cli keys 'mall:product:detail:v1:*'`。也可以临时把 `MALL_PRODUCT_CACHE_ENABLED` 设为 `false` 对比验证。

**Q：Redis 挂了，商品详情会 500 吗？**
不应该。缓存走 fail-open，Redis 不可用时回源 MySQL。详见 [07 Redis](https://github.com/mgtx-0910/fullstack-mall/wiki/07-Redis-Cache)。

**Q：怎么快速定位一次请求？**
看响应头 `X-Trace-Id`，然后按这个 traceId 过滤服务日志。

## 测试类

**Q：怎么跑测试？**
```bash
cd backend && mvn clean test
```
需要 Docker 的容器测试默认关闭，要显式开启：
```bash
RUN_MYSQL_CONTAINER_TESTS=true TESTCONTAINERS_RYUK_DISABLED=true \
  mvn -pl service -am -Dtest=OrderMySqlContainerTest,PaymentMySqlContainerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```
Redis 容器测试：`RUN_REDIS_CONTAINER_TESTS=true ... -Dtest=RedisContainerTest`。

## 安全类

**Q：README 里的演示账号能用在生产吗？**
不能。这些账号密码、数据库连接信息、mock 支付密钥都是本地开发占位值。
真实环境请用密钥管理服务或部署平台的 Secret 能力。
