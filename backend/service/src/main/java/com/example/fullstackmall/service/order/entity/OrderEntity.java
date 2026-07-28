package com.example.fullstackmall.service.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * mall_order 表对应的持久化对象。
 */
@Data
@TableName("mall_order")
public class OrderEntity {

    /** 订单主键，由 MySQL 自增生成；对外查询通常使用更安全的订单号。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 服务端生成的业务订单号，供接口返回、日志追踪和支付关联使用。 */
    @TableField("order_no")
    private String orderNo;

    /** 订单所属用户 ID，必须来自可信登录态，查询时要同时校验归属以防越权。 */
    @TableField("user_id")
    private Long userId;

    /** 订单状态的数据库编码，以 String 持久化，读取后再转换为 OrderStatus 枚举。 */
    @TableField("status_code")
    private String statusCode;

    /** 下单时计算并保存的订单总金额快照，不能在查单时按商品现价重算。 */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /** 客户端一次下单意图的唯一标识，与 userId 组成唯一约束防止重复下单。 */
    @TableField("idempotency_key")
    private String idempotencyKey;

    /** 订单创建时间，由服务端生成。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 订单最近一次状态变化时间，由服务端维护。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 待支付截止时间，定时任务只关闭早于当前时间的待支付订单。 */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /** 支付成功时间；未支付时为 null，不能用 createdAt 代替。 */
    @TableField("paid_at")
    private LocalDateTime paidAt;

    /** 用户取消订单的时间；订单未取消时为 null。 */
    @TableField("cancelled_at")
    private LocalDateTime cancelledAt;

    /** 超时关单时间；只有系统超时关闭后才有值。 */
    @TableField("closed_at")
    private LocalDateTime closedAt;
}
