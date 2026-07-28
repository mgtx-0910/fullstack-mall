package com.example.fullstackmall.contract.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车行响应。商品、价格和库存字段是查询时从其他模块组装的当前值。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    /** 购物车行主键，由数据库生成。 */
    private Long id;
    /** 购物车行关联的 SKU 主键。 */
    private Long skuId;
    /** SKU 所属商品主键，查询时由 SKU 数据组装。 */
    private Long productId;
    /** 商品当前标题，不是下单快照；商品改名后再次查询购物车会看到新标题。 */
    private String productTitle;
    /** SKU 业务编码，来自当前 SKU 数据。 */
    private String skuCode;
    /** 当前规格描述，例如颜色、容量等组合文本。 */
    private String specText;
    /** 查询购物车时的当前单价，仅用于预估，创建订单时服务端会重新校验并保存价格快照。 */
    private BigDecimal currentUnitPrice;
    /** 购物车中保存的购买数量。 */
    private Integer quantity;
    /** 查询时数据库中的当前可售库存，不代表之后下单一定成功。 */
    private Integer availableStock;
    /** 该购物车行是否被选中参与结算；当前新增购物车时由服务端设为选中。 */
    private Boolean checked;
    /** 服务端根据商品状态、价格和库存计算出的当前是否可售。 */
    private Boolean saleable;
    /** 当前单价乘以购物车数量得到的预估小计，不是已承诺的订单金额。 */
    private BigDecimal lineAmount;
    /** 购物车行最后更新时间，来源于购物车表。 */
    private LocalDateTime updatedAt;
}
