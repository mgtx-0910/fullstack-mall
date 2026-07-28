package com.example.fullstackmall.contract.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单项快照响应。这里的标题、规格和价格都是下单时保存的历史值。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    /** 订单项主键，由数据库生成。 */
    private Long id;
    /** 成交时关联的 SKU 主键，用于后续库存和履约追踪。 */
    private Long skuId;
    /** 成交时关联的商品主键。 */
    private Long productId;
    /** 下单时保存的商品标题快照，后续商品改名不会改变它。 */
    private String productTitle;
    /** 下单时保存的 SKU 编码快照。 */
    private String skuCode;
    /** 下单时保存的规格描述快照。 */
    private String specText;
    /** 下单时确认的成交单价快照，不再跟随商品当前售价变化。 */
    private BigDecimal unitPrice;
    /** 该订单项购买数量。 */
    private Integer quantity;
    /** 成交单价乘以数量得到的订单项金额快照。 */
    private BigDecimal lineAmount;
}
