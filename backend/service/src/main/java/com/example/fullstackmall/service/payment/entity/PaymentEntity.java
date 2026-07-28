package com.example.fullstackmall.service.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * mall_payment 表对应的支付流水持久化对象。
 */
@Data
@TableName("mall_payment")
public class PaymentEntity {

    /** 支付流水主键，由 MySQL 自增生成。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 服务端生成的支付单号，用于模拟支付和回调时定位支付流水。 */
    @TableField("payment_no")
    private String paymentNo;

    /** 支付流水关联的订单主键，一个订单在本阶段只创建一条有效支付流水。 */
    @TableField("order_id")
    private Long orderId;

    /** 支付流水所属用户 ID，创建和查询时必须校验，防止替他人订单发起支付。 */
    @TableField("user_id")
    private Long userId;

    /** 支付金额快照，来自订单总金额，不能信任前端自行提交的金额。 */
    private BigDecimal amount;

    /** 支付状态数据库编码，使用 String 保存并转换为 PaymentStatus 枚举。 */
    @TableField("status_code")
    private String statusCode;

    /** 支付平台交易号；回调成功前通常为 null。 */
    @TableField("provider_transaction_no")
    private String providerTransactionNo;

    /** 支付平台回调事件的幂等键，数据库唯一约束防止跨流水重复消费。 */
    @TableField("callback_id")
    private String callbackId;

    /** 支付流水创建时间，由服务端写入。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 支付流水最近一次状态更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 支付成功时间；待支付或已关闭时为 null。 */
    @TableField("paid_at")
    private LocalDateTime paidAt;

    /** 支付流水关闭时间；只有订单取消或超时关闭时才有值。 */
    @TableField("closed_at")
    private LocalDateTime closedAt;
}
