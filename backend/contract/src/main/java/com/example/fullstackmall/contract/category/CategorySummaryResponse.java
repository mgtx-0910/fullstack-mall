package com.example.fullstackmall.contract.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员分类列表摘要。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummaryResponse {

    /** 分类主键。 */
    private Long id;
    /** 分类名称。 */
    private String name;
    /** 分类状态，1 表示启用，0 表示停用。 */
    private Integer status;
    /** 展示排序值，数值越小越靠前。 */
    private Integer sortOrder;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 最近更新时间。 */
    private LocalDateTime updatedAt;
}
