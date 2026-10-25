# 04 MySQL 持久化：为什么前端状态不够用

## 这一页解决什么

前端的 store 刷新就没了，后端的数据要服务所有用户、要能持久化、要经得起并发。
这一页讲：表结构在哪里、约束为什么重要、Entity 和表是什么关系。

## 表结构在哪

- `sql/01_schema.sql`：建表
- `sql/02_seed.sql`：演示数据（首次启动 MySQL 容器时自动执行）

主要表：

| 表 | 作用 | 值得注意的约束 |
| --- | --- | --- |
| `mall_user` | 用户 | `username` 唯一索引 |
| `mall_category` | 商品分类 | 启用 / 停用 |
| `mall_product` | 商品 | 状态字段 `status` |
| `mall_sku` | SKU（规格级价格与库存） | 库存真正扣减在这里 |
| `mall_cart_item` | 购物车 | `(user_id, sku_id)` 唯一索引 |
| `mall_order` | 订单 | `order_no` 唯一索引 |
| `mall_order_item` | 订单明细 | 下单时的商品快照 |
| `mall_payment` | 支付流水 | `trade_no` 唯一索引 |

## 数据库是业务规则的最终保障

前端表单可以限制必填、长度、格式，但**任何人都可以绕过页面直接调接口**。
真正可靠的约束要在数据库里落地：

- 唯一索引保证"不能有两个同名用户""同一笔支付不能重复处理"——
  光靠"先查一遍再插入"在并发下一定会出问题
- 金额用 `DECIMAL`，不要用 `FLOAT / DOUBLE`
- 状态字段 + 非空约束，决定"这个数据现在是合法的吗"

## Entity：数据库一行在 Java 里的样子

例如 `service/product/entity/ProductEntity.java`（配合 MyBatis-Plus 注解与 `map-underscore-to-camel-case`）：

```text
mall_product.status        → ProductEntity.status
mall_product.created_at    → ProductEntity.createdAt
```

Entity 面向表，可以包含 `createdBy`、`deleted` 等内部字段；
Response 面向接口，只暴露前端需要的字段。

## 本地怎么连

```text
host: localhost
port: 3308        # 注意不是 3306
database: fullstack_mall
username: mall
password: mall_dev_password
```

重置本地数据：

```bash
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up -d mysql redis
```

> `down -v` 会删除本项目 Docker volume 中的数据，不要对非本项目环境执行。

## 自查题

- 为什么"存在但未上架"的商品，公开详情接口也返回 404？
- 为什么金额不能用 `double`？
- 唯一索引解决了哪些"代码层面兜不住"的问题？
