package com.example.fullstackmall.contract.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员编辑商品基础信息时允许提交的字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {

    /** 前端选择的分类主键；服务端会确认分类存在且启用。 */
    @NotNull(message = "商品分类不能为空")
    @Positive(message = "商品分类 ID 必须大于 0")
    private Long categoryId;

    /** 前端提交的商品标题；服务端会 trim 后保存。 */
    @NotBlank(message = "商品标题不能为空")
    @Size(max = 100, message = "商品标题不能超过 100 个字符")
    private String title;

    /** 可选商品描述；空白内容会被规范化为 null。 */
    @Size(max = 2000, message = "商品描述不能超过 2000 个字符")
    private String description;
}
