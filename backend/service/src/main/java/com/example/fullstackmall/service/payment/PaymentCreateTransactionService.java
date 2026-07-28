package com.example.fullstackmall.service.payment;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.order.OrderStatus;
import com.example.fullstackmall.contract.payment.PaymentStatus;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.order.entity.OrderEntity;
import com.example.fullstackmall.service.order.service.OrderDbService;
import com.example.fullstackmall.service.payment.entity.PaymentEntity;
import com.example.fullstackmall.service.payment.service.PaymentDbService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 创建支付流水的事务边界。订单行锁避免取消/关单与创建支付交叉。
 */
@Service
public class PaymentCreateTransactionService {

    @Resource
    private OrderDbService orderDbService;

    @Resource
    private PaymentDbService paymentDbService;

    /**
     * 锁定订单并在同一事务中创建唯一支付流水，防止创建支付与取消、关单并发冲突。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createPayment(Long orderId, Long userId) {
        // FOR UPDATE 锁住订单，防止创建支付与取消、超时关单同时修改同一订单。
        OrderEntity order = orderDbService.selectByIdForUpdate(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ApiCode.ORDER_NOT_FOUND,
                    ApiCode.ORDER_NOT_FOUND.defaultMessage(), HttpStatus.NOT_FOUND);
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatusCode())) {
            throw conflict(ApiCode.PAYMENT_ORDER_NOT_PAYABLE);
        }
        if (order.getExpiresAt() != null && !order.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw conflict(ApiCode.PAYMENT_ORDER_NOT_PAYABLE);
        }

        // 一张订单只保留一条支付流水，重复调用直接返回已有记录，实现顺序幂等。
        PaymentEntity existing = paymentDbService.getByOrderId(orderId);
        if (existing != null) {
            return existing.getId();
        }

        LocalDateTime now = LocalDateTime.now();
        PaymentEntity payment = new PaymentEntity();
        payment.setPaymentNo("PAY-" + UUID.randomUUID().toString().replace("-", ""));
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(order.getTotalAmount());
        payment.setStatusCode(PaymentStatus.PENDING.name());
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        paymentDbService.save(payment);
        return payment.getId();
    }

    private BusinessException conflict(ApiCode apiCode) {
        return new BusinessException(apiCode, apiCode.defaultMessage(), HttpStatus.CONFLICT);
    }
}
