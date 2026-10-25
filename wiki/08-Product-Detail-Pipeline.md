# 08 商品详情完整链路：HTTP、Redis、MySQL 与 JSON

## 这一页解决什么

把一个你熟悉的动作——"用户打开商品详情页"——从头到尾走一遍，
把前几页的 Controller、Facade、缓存、数据库、统一响应串成一条线。

## 前端那一侧（示意）

```ts
const res = await axios.get(`/api/products/${productId}`)
if (res.data.code === 'SUCCESS') render(res.data.data)
else showToast(res.data.message)
```

前端关心：路径、参数、返回字段、loading、错误提示。

## 后端那一侧：完整链路

```text
浏览器 / curl  GET /api/products/1
   ↓
TraceIdFilter                生成 traceId，写进日志 MDC 与响应头 X-Trace-Id
   ↓
Spring Security 过滤器链     公开 GET 放行；管理端校验 Token 与角色
   ↓
ProductController.detail()   @PathVariable 绑定 id，不写业务逻辑
   ↓
IProductFacade.getPublishedProduct()
   ↓
ProductFacade                业务编排：
                              ├─ ProductDetailCacheService.lookup()  → HIT 直接返回
                              ├─ MISS / DEGRADED → 回源 MySQL
                              └─ 回填缓存
   ↓
ProductDbService → ProductMapper → MySQL(mall_product / mall_category / mall_sku)
   ↓
Entity → ProductDetailResponse → ApiResponse(code/message/data/traceId) → JSON
   ↓
出错 → GlobalExceptionHandler → 统一 ApiResponse（如 PRODUCT_NOT_FOUND / 404）
```

## 后端看同一个需求，会多问这些问题

- 这个接口允许匿名访问吗？（公开 GET 可以，管理端不行）
- 商品不存在返回什么业务码？（`PRODUCT_NOT_FOUND`）
- 商品未上架能不能被公开访问？（不能，和不存在返回同一个结果）
- 分类停用后商品还能展示吗？（不能，见 `CATEGORY_NOT_FOUND`）
- 查 MySQL 慢的时候要不要走缓存？（要，见 [07 Redis](07-Redis-Cache.md)）
- Redis 挂了是失败还是降级？（降级回源）
- 返回字段能直接把数据库所有字段暴露出去吗？（不能）
- 一次请求怎么在日志里定位？（traceId）

这些问题，就是后端要分层的全部理由。

## 动手

```bash
# 看完整响应头（含 X-Trace-Id）
curl -i http://localhost:8080/api/products/1

# 不存在的商品
curl -i http://localhost:8080/api/products/999999

# 分页列表
curl -sS 'http://localhost:8080/api/products?pageNo=1&pageSize=10'
```

## 自查题

- 画出自己的调用链：HTTP → Controller → Facade → 缓存 / DbService → Mapper → MySQL → Response → JSON
- 缓存命中和未命中，日志里分别是什么事件？（HIT / MISS / PUT）
- Redis 停掉后，这个接口的行为是什么？为什么？
