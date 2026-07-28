package com.example.fullstackmall.contract.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商品列表摘要。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {
    /** 商品主键。 */
    private Long id;
    /** 商品所属分类主键。 */
    private Long categoryId;
    /** 批量查询分类后组装的当前分类名称，避免逐条查询造成 N+1。 */
    private String categoryName;
    /** 商品当前标题。 */
    private String title;
    /** 商品当前生命周期状态。 */
    private ProductStatus status;
    /** 商品创建时间。 */
    private LocalDateTime createdAt;
}
