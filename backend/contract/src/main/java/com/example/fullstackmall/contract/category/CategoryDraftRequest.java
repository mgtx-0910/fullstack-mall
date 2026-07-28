package com.example.fullstackmall.contract.category;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类草稿请求。Request 表示客户端允许提交什么，不是数据库 Entity。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDraftRequest {

    /** 前端提交的分类名称；服务端会 trim，并检查长度和保留名称。 */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 30, message = "分类名称不能超过 30 个字符")
    private String name;

    /** 可选分类描述；空白文本会被规范化为空字符串。 */
    @Size(max = 200, message = "分类描述不能超过 200 个字符")
    private String description;

    /** 前端提交的展示排序值，数值越小通常越靠前。 */
    @NotNull(message = "排序值不能为空")
    @Min(value = 0, message = "排序值不能小于 0")
    @Max(value = 9999, message = "排序值不能大于 9999")
    private Integer sortOrder;
}
