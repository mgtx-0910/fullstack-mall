package com.example.fullstackmall.contract.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 当前登录用户的购物车及展示汇总。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    /** 当前登录用户的购物车明细；服务端已经按 userId 做数据隔离。 */
    private List<CartItemResponse> items;
    /** 全部返回明细的数量合计，便于前端显示购物车角标。 */
    private Integer totalQuantity;
    /** 按当前价格计算的预估总额；真正下单金额由订单服务重新计算。 */
    private BigDecimal estimatedTotal;
}
