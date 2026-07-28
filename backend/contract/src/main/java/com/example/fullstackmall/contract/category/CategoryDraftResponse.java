package com.example.fullstackmall.contract.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规范化后的分类草稿响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDraftResponse {

    /** 经过服务端 trim 和规则校验后的分类名称。 */
    private String name;
    /** 经过服务端规范化后的描述。 */
    private String description;
    /** 校验通过的排序值；阶段 1 只返回结果，不写入数据库。 */
    private Integer sortOrder;
}
