package com.example.fullstackmall.contract.payment;

/**
 * 支付流水状态。
 */
public enum PaymentStatus {
    /** 待支付：支付流水已创建，尚未收到成功回调。 */
    PENDING,
    /** 支付成功终态：平台交易号和支付时间已经保存。 */
    SUCCESS,
    /** 已关闭终态：对应订单已取消或超时，不能再成功支付。 */
    CLOSED
}
