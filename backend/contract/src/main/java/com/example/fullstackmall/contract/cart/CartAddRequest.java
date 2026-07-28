package com.example.fullstackmall.contract.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加入购物车请求。userId 必须从当前登录上下文获取，不能由前端提交。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartAddRequest {

    /** 前端选择的 SKU 主键；服务端仍会查询数据库确认 SKU、商品和库存真实可售。 */
    @NotNull(message = "SKU ID 不能为空")
    private Long skuId;

    /** 本次希望加入的数量；最终是否可加入还要与数据库当前可售库存比较。 */
    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量不能小于 1")
    @Max(value = 99, message = "单个 SKU 最多加入 99 件")
    private Integer quantity;
}
