package com.example.fullstackmall.service.category.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * mall_category 表对应的持久化对象。
 */
@Data
@TableName("mall_category")
public class CategoryEntity {

    /** 分类主键，由 MySQL 自增生成，创建分类时前端不传。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 分类名称，会展示在商品详情中，因此分类改名后需要让相关商品缓存失效。 */
    private String name;
    /** 分类启用状态：1 表示启用，0 表示停用；公开商品只能使用启用分类。 */
    private Integer status;

    /** 分类排序值，数值越小越靠前，属于后台配置字段。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 分类创建时间，由服务端生成并持久化。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 分类最后更新时间，可用于排查后台修改记录。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
