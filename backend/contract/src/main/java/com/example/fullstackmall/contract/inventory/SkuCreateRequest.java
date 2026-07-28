package com.example.fullstackmall.contract.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建 SKU 请求。productId 来自 URL，version 和 lockedStock 由服务端维护。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuCreateRequest {

    /** 前端提交的 SKU 业务编码；服务端会 trim，并由数据库唯一约束防止重复。 */
    @NotBlank(message = "SKU 编码不能为空")
    @Size(max = 64, message = "SKU 编码不能超过 64 个字符")
    private String skuCode;

    /** 前端提交的规格组合描述，例如“红色 / 256GB”。 */
    @NotBlank(message = "规格描述不能为空")
    @Size(max = 200, message = "规格描述不能超过 200 个字符")
    private String specText;

    /** SKU 初始销售价，金额必须使用 BigDecimal，避免浮点精度问题。 */
    @NotNull(message = "销售价不能为空")
    @DecimalMin(value = "0.01", message = "销售价必须大于 0")
    @Digits(integer = 10, fraction = 2, message = "销售价最多 10 位整数和 2 位小数")
    private BigDecimal salePrice;

    /** SKU 创建时的初始可售库存；锁定库存由服务端维护，不能由前端提交。 */
    @NotNull(message = "初始库存不能为空")
    @Min(value = 0, message = "初始库存不能小于 0")
    private Integer initialStock;
}
