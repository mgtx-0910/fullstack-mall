package com.example.fullstackmall.service.cart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * mall_cart_item 表对应的持久化对象。
 */
@Data
@TableName("mall_cart_item")
public class CartItemEntity {

    /** 购物车明细主键，由 MySQL 自增生成，前端新增时不能指定。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 购物车所属用户 ID，来自可信登录态，用于隔离不同用户的数据。 */
    @TableField("user_id")
    private Long userId;

    /** 加入购物车的 SKU ID；购物车保存具体规格，而不是只保存商品 ID。 */
    @TableField("sku_id")
    private Long skuId;

    /** 用户计划购买的数量；真正下单时仍需重新校验库存。 */
    private Integer quantity;

    /** 1 表示本次结算选中，0 表示仅保留在购物车。 */
    private Integer checked;

    /** 购物车明细创建时间，由服务端写入，不接收前端时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 最近一次修改数量或勾选状态的时间，由服务端维护。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
