package com.example.fullstackmall.contract.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员修改商品副标题请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSubtitleUpdateRequest {
    /** 商品副标题；服务端会 trim 后保存。 */
    @NotBlank(message = "商品副标题不能为空")
    @Size(max=100,message = "商品副标题不能超过100个字符")
    private String subtitle;
}
