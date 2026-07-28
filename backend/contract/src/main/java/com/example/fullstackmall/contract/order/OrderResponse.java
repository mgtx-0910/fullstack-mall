package com.example.fullstackmall.contract.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前登录用户的订单详情。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    /** 订单数据库主键。 */
    private Long id;
    /** 服务端生成的对外订单号，前端不能自行指定。 */
    private String orderNo;
    /** 订单当前状态，由后端状态机控制。 */
    private OrderStatus status;
    /** 所有订单项金额快照之和，是支付金额校验依据。 */
    private BigDecimal totalAmount;
    /** 订单项快照列表，商品后续改名或改价不会改变这里的历史数据。 */
    private List<OrderItemResponse> items;
    /** 订单创建时间。 */
    private LocalDateTime createdAt;
    /** 订单最近一次状态或内容更新时间。 */
    private LocalDateTime updatedAt;
    /** 支付截止时间；超过该时间且仍待支付的订单可被定时任务关闭。 */
    private LocalDateTime expiresAt;
    /** 支付成功时间；订单未支付时为 null。 */
    private LocalDateTime paidAt;
    /** 用户主动取消时间；未取消时为 null。 */
    private LocalDateTime cancelledAt;
    /** 系统超时关闭时间；未被系统关闭时为 null。 */
    private LocalDateTime closedAt;
}
