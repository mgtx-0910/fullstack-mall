package com.example.fullstackmall.service.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * mall_order_item 表对应的订单项快照。
 */
@Data
@TableName("mall_order_item")
public class OrderItemEntity {

    /** 订单明细主键，由 MySQL 自增生成。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属订单主键，用于建立一张订单与多条明细的一对多关系。 */
    @TableField("order_id")
    private Long orderId;

    /** 下单时选择的 SKU ID，用于扣减、确认或释放对应库存。 */
    @TableField("sku_id")
    private Long skuId;

    /** SKU 所属商品 ID，作为订单历史检索和展示关联信息保存。 */
    @TableField("product_id")
    private Long productId;

    /** 下单时复制的商品标题快照，后续商品改名不会影响历史订单。 */
    @TableField("product_title")
    private String productTitle;

    /** 下单时保存的 SKU 编码快照，避免后续规格调整影响历史订单。 */
    @TableField("sku_code")
    private String skuCode;

    /** 下单时保存的规格描述快照，用于稳定展示历史订单。 */
    @TableField("spec_text")
    private String specText;

    /** 下单时确认的成交单价快照，不能在查订单时重新读取商品现价。 */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /** 该订单明细购买数量，也是库存锁定、确认和释放的数量。 */
    private Integer quantity;

    /** 明细金额快照，等于下单时 unitPrice × quantity，由服务端计算。 */
    @TableField("line_amount")
    private BigDecimal lineAmount;

    /** 订单明细创建时间，和订单一起在同一事务中写入。 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
