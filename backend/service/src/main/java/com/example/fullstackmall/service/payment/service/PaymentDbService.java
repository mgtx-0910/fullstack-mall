package com.example.fullstackmall.service.payment.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fullstackmall.service.payment.entity.PaymentEntity;
import com.example.fullstackmall.service.payment.mapper.PaymentMapper;
import org.springframework.stereotype.Service;

/**
 * 支付流水数据库访问 Service。
 */
@Service
public class PaymentDbService extends ServiceImpl<PaymentMapper, PaymentEntity> {

    public PaymentEntity getByOrderId(Long orderId) {
        return lambdaQuery().eq(PaymentEntity::getOrderId, orderId).one();
    }

    public PaymentEntity getByOrderIdAndUserId(Long orderId, Long userId) {
        return lambdaQuery()
                .eq(PaymentEntity::getOrderId, orderId)
                .eq(PaymentEntity::getUserId, userId)
                .one();
    }

    public PaymentEntity getByCallbackId(String callbackId) {
        return lambdaQuery().eq(PaymentEntity::getCallbackId, callbackId).one();
    }

    /**
     * 回调处理前锁定支付流水，使同一 paymentNo 的重复或并发回调排队执行。
     */
    public PaymentEntity selectByPaymentNoForUpdate(String paymentNo) {
        return baseMapper.selectByPaymentNoForUpdate(paymentNo);
    }

    /**
     * 按订单锁定支付流水，供取消和超时关单遵守“支付行 → 订单行 → SKU 行”的统一锁顺序。
     */
    public PaymentEntity selectByOrderIdForUpdate(Long orderId) {
        return baseMapper.selectByOrderIdForUpdate(orderId);
    }

    /**
     * 仅把 PENDING 支付更新为 SUCCESS，并同时保存渠道流水号和回调 ID。
     * 返回 0 表示状态已被其他事务修改，不能继续重复确认订单与库存。
     */
    public int markSuccess(Long paymentId, String providerTransactionNo, String callbackId) {
        return baseMapper.markSuccess(paymentId, providerTransactionNo, callbackId);
    }

    /**
     * 订单取消或超时关闭时，只关闭仍为 PENDING 的支付流水，避免覆盖已成功支付状态。
     */
    public int closePendingByOrderId(Long orderId) {
        return baseMapper.closePendingByOrderId(orderId);
    }
}
