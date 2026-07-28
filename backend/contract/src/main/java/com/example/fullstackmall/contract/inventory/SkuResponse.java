package com.example.fullstackmall.contract.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SKU 对外响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuResponse {

    /** SKU 主键，由数据库生成。 */
    private Long id;
    /** 该 SKU 所属商品主键。 */
    private Long productId;
    /** SKU 唯一业务编码。 */
    private String skuCode;
    /** 用于前端展示的规格组合文本。 */
    private String specText;
    /** 当前销售价；金额使用 BigDecimal 保证十进制精度。 */
    private BigDecimal salePrice;
    /** 当前可售库存，下单锁定时会减少。 */
    private Integer availableStock;
    /** 已经被待支付订单占用的库存，不能再次出售。 */
    private Integer lockedStock;
    /** 乐观锁版本号；价格或后台库存调整成功后会递增。 */
    private Integer version;
    /** SKU 创建时间，由服务端生成。 */
    private LocalDateTime createdAt;
    /** SKU 最近更新时间，由服务端维护。 */
    private LocalDateTime updatedAt;
}
