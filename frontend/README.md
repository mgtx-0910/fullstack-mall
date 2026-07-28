# fullstack-mall 管理后台

精简版管理后台前端工程，基于 `Umi Max`、`React`、`Ant Design Pro`、`Ant Design` 和 `Zustand`。

## 功能范围

本期只做后台管理，不包含用户端商城页面：

- 登录 / 退出登录，并限制只有 `ADMIN` 角色可进入后台。
- 后台布局和导航。
- 商品管理：列表、新建、编辑、上架、下架、修改副标题。
- SKU 管理：列表、创建、改价、改库存，改价和改库存会携带后端版本号做乐观锁校验。
- 用户管理：用户列表查询。
- 订单管理：订单列表、详情、取消待支付订单。
- 分类管理：分类基础展示、分类草稿校验。

## 本地运行

### 1. 启动后端

在项目根目录执行：

```bash
cd backend
mvn -Dmaven.repo.local=$PWD/.m2-repository -pl service -am spring-boot:run
```

后端默认监听 `http://localhost:8080`。

### 2. 安装前端依赖

```bash
cd frontend
pnpm install
```

### 3. 启动前端

```bash
pnpm dev
```

Umi 默认监听 `http://localhost:8000`。前端开发代理已配置：

```ts
'/api' -> 'http://localhost:8080'
```

因此页面代码统一请求 `/api/...`，不要在业务代码中写死后端地址。

## 管理员账号

如果已执行项目初始化 SQL / seed 数据，可使用示例管理员账号登录：

- 用户名：`admin`
- 密码：`Admin123456`

如果本地数据库没有该账号，请先按后端文档完成数据库初始化。

## 常用命令

```bash
pnpm dev        # 启动开发服务器
pnpm typecheck  # 生成 Umi 临时类型并执行 TypeScript 类型检查
pnpm build      # 生产构建
pnpm lint       # Umi lint
```
