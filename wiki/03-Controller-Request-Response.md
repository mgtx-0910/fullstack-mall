# 03 Controller / Request / Response：Axios 请求进后端后被谁接住

## 这一页解决什么

前端 `axios.get('/api/products/1')` 的另一端发生了什么？
请求被谁接住、参数怎么绑定、为什么响应要统一包一层、失败时前端会收到什么。

## Controller 是 HTTP 入口，不是业务逻辑层

`service/product/ProductController.java`：

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Resource
    private IProductFacade productFacade;

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> detail(
            @PathVariable Long id,
            HttpServletRequest servletRequest
    ) {
        ProductDetailResponse response = productFacade.getPublishedProduct(id);
        return ApiResponse.success(response, TraceIdContext.get(servletRequest));
    }
}
```

逐行翻译给前端同学：

- `@RestController`：这个类里的方法作为 HTTP 接口暴露，返回值会被序列化成 JSON
- `@RequestMapping("/api/products")`：公共路径前缀，相当于前端 `api/product.ts` 里的 base path
- `@GetMapping("/{id}")` + `@PathVariable Long id`：路径参数绑定
- `@Resource private IProductFacade`：依赖注入，Controller 不自己 `new`
- `productFacade.getPublishedProduct(id)`：业务交给 Facade
- `ApiResponse.success(..., traceId)`：统一响应 + 链路 ID

**Controller 只做三件事**：接住 HTTP 参数、调用 Facade、包一层统一响应。

## 三个"看起来都是类型"的东西

| 类型 | 面向谁 | 位置 | 能否直接暴露给前端 |
| --- | --- | --- | --- |
| Request DTO | 接口入参 | `contract/product/ProductCreateRequest.java` | — |
| Response DTO | 接口出参 | `contract/product/ProductDetailResponse.java` | 是 |
| Entity | 数据库表 | `service/product/entity/ProductEntity.java` | 否 |

Entity 里有 `createdBy`、`deleted` 这类内部字段，公开 Response 不应该带出去。

## 统一响应

`contract/common/ApiResponse.java` + `contract/common/ApiCode.java`：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": { "id": 1, "name": "示例商品" },
  "traceId": "b3f1c0..."
}
```

前端要按 `code` 分支，**不要解析 `message` 文案**（`ApiCode.defaultMessage()` 只是默认提示，调用方可以覆盖）。

分页用 `contract/common/PageResponse.java`，不要每个接口自定义一套 `{ list, count }`。

## 失败契约

`service/common/exception/GlobalExceptionHandler.java` 把异常翻成统一响应：

| 场景 | code | HTTP |
| --- | --- | --- |
| 参数不合法 | `VALIDATION_ERROR` | 400 |
| JSON 格式错 | `MALFORMED_JSON` | 400 |
| 没登录 / Token 失效 | `UNAUTHORIZED` | 401 |
| 登录了但没权限 | `FORBIDDEN` | 403 |
| 商品不存在 | `PRODUCT_NOT_FOUND` | 404 |
| 库存不足 | `INSUFFICIENT_STOCK` | 409/400 |
| 未知异常 | `INTERNAL_ERROR` | 500 |

完整清单见 `contract/common/ApiCode.java`。

## traceId：后端排查问题的那根线

- `service/common/trace/TraceIdFilter.java`：请求进来生成 traceId，放进日志 MDC 和响应头
- `service/common/trace/TraceIdContext.java`：取当前请求的 traceId

前端排查看 Network 面板，后端排查看日志 + traceId。用户反馈"商品详情打不开"时，
拿到响应头里的 `X-Trace-Id` 就能把那一次请求的所有日志串起来。

## 动手

```bash
curl -i http://localhost:8080/api/products/1        # 看响应头 X-Trace-Id
curl -i http://localhost:8080/api/products/999999   # 404 + PRODUCT_NOT_FOUND
curl -i -X POST http://localhost:8080/api/admin/products -d '{}'   # 401（没 Token）
```

## 自查题

- `GET /api/products/{id}` 对应哪个 Java 方法？`{id}` 绑定到哪个参数？
- Request / Response / Entity 字段很像，为什么不合并？
- 前端校验已经做了，后端为什么还要校验？
