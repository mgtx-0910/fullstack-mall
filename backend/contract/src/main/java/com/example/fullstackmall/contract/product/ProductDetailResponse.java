package com.example.fullstackmall.contract.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商品详情。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {
    /** 商品主键，由数据库生成。 */
    private Long id;
    /** 商品所属分类主键。 */
    private Long categoryId;
    /** 查询时由分类表组装的名称，不在商品表中重复保存。 */
    private String categoryName;
    /** 商品当前标题。 */
    private String title;
    /** 商品副标题。 */
    private String subtitle;
    /** 商品当前描述，未填写时可能为 null。 */
    private String description;
    /** 商品当前生命周期状态。 */
    private ProductStatus status;
    /** 创建商品的管理员用户 ID，来源于可信登录上下文。 */
    private Long createdBy;
    /** 商品创建时间，由服务端生成。 */
    private LocalDateTime createdAt;
    /** 商品最近更新时间。 */
    private LocalDateTime updatedAt;
}
