package com.example.fullstackmall.contract.order;

/**
 * 订单状态。状态只能从待支付进入一个终态。
 */
public enum OrderStatus {
    /** 待支付：库存已锁定，仍可支付、取消或被超时关闭。 */
    PENDING_PAYMENT,
    /** 已支付终态：支付回调已经确认锁定库存售出。 */
    PAID,
    /** 用户主动取消终态：锁定库存已经释放。 */
    CANCELLED,
    /** 系统超时关闭终态：锁定库存已经释放。 */
    CLOSED
}
