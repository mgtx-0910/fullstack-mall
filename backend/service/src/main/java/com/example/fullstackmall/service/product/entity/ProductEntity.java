package com.example.fullstackmall.service.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * mall_product 表对应的持久化对象。
 */
@Data
@TableName("mall_product")
public class ProductEntity {

    /** 商品主键，由 MySQL 自增生成，缓存 key 也使用这个可信 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商品所属分类 ID；公开详情还会校验该分类当前是否启用。 */
    @TableField("category_id")
    private Long categoryId;

    /** 商品当前标题，属于可变的当前值，不是订单中的历史快照。 */
    private String title;
    /** 商品副标题 */
    private String subtitle;
    /** 商品当前描述，由后台维护并展示在公开详情中。 */
    private String description;

    /** 商品状态数据库编码，以 String 持久化并转换为 ProductStatus 枚举。 */
    @TableField("status_code")
    private String statusCode;

    /** 创建商品的后台用户 ID，来自可信登录态，而不是前端任意指定。 */
    @TableField("created_by")
    private Long createdBy;

    /** 商品创建时间，由服务端生成。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 商品最后更新时间；更新成功后还需要删除对应详情缓存。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
