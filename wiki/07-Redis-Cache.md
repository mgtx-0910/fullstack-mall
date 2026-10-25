# 07 Redis 缓存：为什么后端也要缓存，以及 Redis 挂了怎么办

## 这一页解决什么

前端有内存缓存、localStorage；后端有 Redis。
关键区别是：**Redis 是服务端共享的性能层，不是业务真相来源。**

## 涉及的文件

| 文件 | 作用 |
| --- | --- |
| `service/cache/RedisOperatorClient.java` | 对 Redis 操作统一封装，异常集中兜底 |
| `service/product/cache/ProductDetailCacheService.java` | 商品详情 Cache Aside：key / JSON / TTL / 空值 / 失效 / 降级 |
| `product/cache/ProductDetailCacheLookup.java` | 一次查询的结果：HIT / MISS / NULL_HIT / DEGRADED / BYPASS |
| `product/cache/ProductDetailCacheStatus.java` | 缓存状态枚举 |
| `product/cache/ProductDetailCacheProperties.java` | 开关与 TTL 配置 |

## Cache Aside 的标准姿势

```text
读：先查缓存
    命中        → 直接返回
    未命中/降级 → 查 MySQL → 回填缓存 → 返回
写：先更新数据库 → 再删除缓存（写后失效）
```

## 这个实现里值得学的 5 个点

1. **key 带版本前缀**：`mall:product:detail:v1:{id}`，将来改缓存结构可以平滑切 v2
2. **写后失效**：改完商品立刻 `evict()`，否则前端会一直看到旧数据
3. **空值缓存**：缓存 `__NULL__:PRODUCT_NOT_FOUND` 并给一个很短的 TTL（默认 30s），
   缓解"同一个不存在的 ID 被反复请求"造成的缓存穿透
4. **坏 JSON 自愈**：反序列化失败会删掉脏 key 再回源，
   否则在 TTL 到期前每次请求都会重复失败
5. **fail-open**：Redis 不可用时返回 `DEGRADED`，Facade 继续查 MySQL ——
   缓存故障不能让原本可用的商品详情接口一起不可用

## 相关配置

```yaml
mall:
  cache:
    product-detail:
      enabled: ${MALL_PRODUCT_CACHE_ENABLED:true}
      value-ttl: ${MALL_PRODUCT_CACHE_VALUE_TTL:10m}
      null-ttl: ${MALL_PRODUCT_CACHE_NULL_TTL:30s}
```

本地 Redis 端口 `6379`（见 `docker-compose.dev.yml`）。

## 动手验证

```bash
# 1) 正常读两次，观察日志里的 HIT / MISS / PUT
curl -sS http://localhost:8080/api/products/1
curl -sS http://localhost:8080/api/products/1

# 2) 停掉 Redis，验证 fail-open（接口仍应返回数据，而不是 500）
docker compose -f docker-compose.dev.yml stop redis
curl -i http://localhost:8080/api/products/1
docker compose -f docker-compose.dev.yml start redis

# 3) 用 redis-cli 直接看 key
docker exec -it fullstack-mall-redis redis-cli keys 'mall:product:detail:v1:*'
```

## 自查题

- Redis 是数据库吗？如果 Redis 里的数据和 MySQL 不一致，以谁为准？
- 改完商品不删缓存会发生什么？
- 为什么"不存在的商品"也要缓存，而且还只缓存很短时间？
