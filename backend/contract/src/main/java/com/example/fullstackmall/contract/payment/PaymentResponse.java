package com.example.fullstackmall.contract.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 当前用户可见的支付流水。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /** 支付流水数据库主键。 */
    private Long id;
    /** 服务端生成的对外支付流水号。 */
    private String paymentNo;
    /** 该支付流水关联的订单主键。 */
    private Long orderId;
    /** 创建支付流水时保存的订单金额快照，回调时必须与订单金额一致。 */
    private BigDecimal amount;
    /** 支付流水当前状态，由后端状态机维护。 */
    private PaymentStatus status;
    /** 支付成功后记录的支付平台交易号；尚未成功时为 null。 */
    private String providerTransactionNo;
    /** 支付流水创建时间。 */
    private LocalDateTime createdAt;
    /** 支付流水最近更新时间。 */
    private LocalDateTime updatedAt;
    /** 支付成功时间；未成功时为 null。 */
    private LocalDateTime paidAt;
    /** 订单取消或超时导致支付关闭的时间；未关闭时为 null。 */
    private LocalDateTime closedAt;
}
