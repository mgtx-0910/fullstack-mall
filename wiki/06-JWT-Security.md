# 06 JWT 与 Spring Security：登录后端到底在做什么

## 这一页解决什么

前端熟悉的是"登录拿到 Token，存起来，请求时带上"。
这一页讲另一端：**后端怎么签发 Token、怎么验证 Token、怎么判断你能不能访问这个接口。**

## 一句话分工

> 前端负责携带 Token，后端负责验证 Token 是否可信，并决定能不能访问接口。

## 涉及的文件

| 文件 | 作用 |
| --- | --- |
| `service/auth/AuthController.java` | `/api/auth/register`、`/api/auth/login` |
| `service/auth/AuthFacade.java` | 业务编排：校验账号状态、签发 Token |
| `service/auth/AuthService.java` | 用户查询与落库 |
| `service/auth/JwtTokenService.java` | JWT 签发与解析 |
| `service/security/JwtAuthenticationFilter.java` | 从 `Authorization: Bearer <token>` 解出登录人 |
| `service/security/SecurityConfig.java` | 谁可以访问哪个接口 |
| `service/security/CurrentUserService.java` | 取当前登录人 |
| `service/security/CurrentUserPrincipal.java` | 登录人模型 |
| `service/security/RestAuthenticationEntryPoint.java` | 401 的返回 |
| `service/security/RestAccessDeniedHandler.java` | 403 的返回 |
| `contract/auth/UserRole.java` | 普通用户 / 管理员 |

## 401 还是 403

- **401 UNAUTHORIZED**：你是谁？——没带 Token、Token 过期、Token 被篡改
- **403 FORBIDDEN**：我知道你是谁，但你没权限——普通用户访问 `/api/admin/**`

被 Security 拦下的请求也会返回统一 `ApiResponse`，不会给你一个空 body 或 HTML 错误页。

## 动手

```bash
# 管理员登录
curl -sS -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123456"}'

ADMIN_TOKEN='粘贴响应里的 accessToken'

# 带 Token 访问管理端
curl -sS -X POST http://localhost:8080/api/admin/products \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"示例商品","subtitle":"本地演示","categoryId":1,"mainImage":"https://example.com/p.png","price":1999}'

# 不带 Token 再试一次 → 401
curl -i -X POST http://localhost:8080/api/admin/products -d '{}'

# 普通用户 Token 访问管理端 → 403
```

演示账号（仅本地演示）：`xiaoming / Mall123456`、`admin / Admin123456`。

## 几条铁律

1. **密码存哈希，不存明文**；校验用 `PasswordEncoder.matches`
2. **"谁在操作"从 Token 里取，不从请求体里取**，否则前端改一个字段就能伪造操作人
3. 用户不存在和密码错误返回**同一个**提示，避免被枚举出哪些用户名存在
4. 密钥通过 `JWT_SECRET` 环境变量注入，公开仓库不内置；相关业务码：`UNAUTHORIZED`、`FORBIDDEN`、`INVALID_CREDENTIALS`、`ACCOUNT_DISABLED`

## 自查题

- 401 和 403 的区别是什么？各举一个本仓库里的场景。
- 为什么 `operatorId` 不能由前端传？
- Token 过期了，前端会看到什么 code？
