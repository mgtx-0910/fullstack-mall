# 05 MyBatis-Plus：不手写一堆 SQL 怎么操作数据库

## 这一页解决什么

后端不是"直接拿 JSON 当数据库"，而是把数据库行映射成 Entity，再组装成 Response。
这一页讲清 Mapper / DbService / Entity 三层各干什么。

## 三层职责

```text
Entity     mall_product 一行在 Java 里长什么样
Mapper     SQL / CRUD 的执行入口（继承 BaseMapper 就有单表增删改查）
DbService  按业务语义封装（"只查已上架商品"这种条件只写一次）
```

## Mapper：零 SQL 的单表 CRUD

`service/product/mapper/ProductMapper.java`：

```java
@Mapper
public interface ProductMapper extends BaseMapper<ProductEntity> {
}
```

继承 `BaseMapper<ProductEntity>` 就拥有了 `selectById`、`insert`、`updateById`、`delete` 等能力。
复杂查询再写自定义方法或 XML，不要一上来就手写一堆 SQL。

## DbService：把底层操作包装成业务语义

`service/product/service/ProductDbService.java` 里会有类似这些方法：

- `findPublishedById(...)`：只查已上架商品
- `pagePublished(...)`：公开分页
- `pageAdmin(...)`：管理端分页（可按状态过滤）

为什么必须这样封一层？因为"公开接口只能看到已上架商品"这条规则，
如果散落在各个 Controller 里，迟早有人漏掉 `status` 条件，把草稿商品暴露出去。

## 分页拦截器（很容易踩的坑）

`service/config/MybatisPlusConfig.java` 注册了分页插件。
**没有它，`selectPage()` 会退化成"查全表 + 内存分页"**，本地数据量小看不出来，上线就出事。

相关配置在 `application.yml`：

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    banner: false
    db-config:
      id-type: auto
```

## 连表与复杂查询

本仓库的商品详情会同时用到商品、分类、SKU：
简单关联查询走 DbService 组合调用，统计 / 复杂条件再考虑自定义 SQL。
先看 `service/product/ProductFacade.java` 里是怎么把这几份数据组装成 `ProductDetailResponse` 的。

## 自查题

- Entity、Mapper、DbService 分别解决什么问题？
- 为什么公开查询要统一走 `findPublishedById` 这类方法？
- 忘了配分页拦截器会发生什么？
