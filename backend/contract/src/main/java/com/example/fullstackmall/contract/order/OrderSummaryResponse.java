package com.example.fullstackmall.contract.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员订单列表摘要。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {

    /** 订单数据库主键。 */
    private Long id;
    /** 服务端生成的对外订单号。 */
    private String orderNo;
    /** 下单用户 ID。 */
    private Long userId;
    /** 订单当前状态。 */
    private OrderStatus status;
    /** 订单总金额快照。 */
    private BigDecimal totalAmount;
    /** 订单创建时间。 */
    private LocalDateTime createdAt;
    /** 订单最近更新时间。 */
    private LocalDateTime updatedAt;
    /** 支付截止时间。 */
    private LocalDateTime expiresAt;
    /** 支付成功时间。 */
    private LocalDateTime paidAt;
    /** 取消时间。 */
    private LocalDateTime cancelledAt;
    /** 超时关闭时间。 */
    private LocalDateTime closedAt;
}
