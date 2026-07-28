package com.example.fullstackmall.service.payment;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.payment.IPaymentFacade;
import com.example.fullstackmall.contract.payment.MockPaymentCallbackRequest;
import com.example.fullstackmall.contract.payment.PaymentResponse;
import com.example.fullstackmall.contract.payment.PaymentStatus;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.payment.entity.PaymentEntity;
import com.example.fullstackmall.service.payment.service.PaymentDbService;
import com.example.fullstackmall.service.security.CurrentUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 支付用例实现：身份、共享密钥、并发重试和 Response 组装放在事务外层。
 */
@Service
public class PaymentFacade implements IPaymentFacade {

    @Resource
    private CurrentUserService currentUserService;

    @Resource
    private PaymentDbService paymentDbService;

    @Resource
    private PaymentCreateTransactionService paymentCreateTransactionService;

    @Resource
    private PaymentCallbackTransactionService paymentCallbackTransactionService;

    @Value("${mall.payment.mock-callback-secret}")
    private String configuredCallbackSecret;

    @Override
    public PaymentResponse createMyPayment(Long orderId) {
        Long userId = currentUserService.requireCurrentUserId();
        // 事务外先快速查询，常见的重复请求无需再次获取订单行锁。
        PaymentEntity existing = paymentDbService.getByOrderIdAndUserId(orderId, userId);
        if (existing != null) {
            return toResponse(existing);
        }
        try {
            Long paymentId = paymentCreateTransactionService.createPayment(orderId, userId);
            return toResponse(paymentDbService.getById(paymentId));
        } catch (DataIntegrityViolationException exception) {
            // 并发创建时由 order_id 唯一约束决定唯一胜者，失败请求再读取已有流水。
            PaymentEntity concurrent = paymentDbService.getByOrderIdAndUserId(orderId, userId);
            if (concurrent != null) {
                return toResponse(concurrent);
            }
            throw exception;
        }
    }

    @Override
    public PaymentResponse queryMyPayment(Long orderId) {
        Long userId = currentUserService.requireCurrentUserId();
        PaymentEntity payment = paymentDbService.getByOrderIdAndUserId(orderId, userId);
        if (payment == null) {
            throw notFound();
        }
        return toResponse(payment);
    }

    @Override
    public PaymentResponse handleMockCallback(String callbackSecret, MockPaymentCallbackRequest request) {
        requireValidCallbackSecret(callbackSecret);
        try {
            Long paymentId = paymentCallbackTransactionService.handleSuccess(request);
            return toResponse(paymentDbService.getById(paymentId));
        } catch (DataIntegrityViolationException exception) {
            // callback_id 唯一约束防止同一回调事件被记到不同支付流水上。
            PaymentEntity callbackPayment = paymentDbService.getByCallbackId(request.getCallbackId());
            if (callbackPayment != null && request.getPaymentNo().equals(callbackPayment.getPaymentNo())) {
                return toResponse(callbackPayment);
            }
            throw new BusinessException(ApiCode.PAYMENT_CALLBACK_DUPLICATE,
                    ApiCode.PAYMENT_CALLBACK_DUPLICATE.defaultMessage(), HttpStatus.CONFLICT);
        }
    }

    private void requireValidCallbackSecret(String actualSecret) {
        byte[] expected = configuredCallbackSecret.getBytes(StandardCharsets.UTF_8);
        byte[] actual = actualSecret == null ? new byte[0] : actualSecret.getBytes(StandardCharsets.UTF_8);
        // 使用常量时间比较，避免普通字符串比较因提前结束暴露匹配长度信息。
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new BusinessException(ApiCode.PAYMENT_CALLBACK_SECRET_INVALID,
                    ApiCode.PAYMENT_CALLBACK_SECRET_INVALID.defaultMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private BusinessException notFound() {
        return new BusinessException(ApiCode.PAYMENT_NOT_FOUND,
                ApiCode.PAYMENT_NOT_FOUND.defaultMessage(), HttpStatus.NOT_FOUND);
    }

    private PaymentResponse toResponse(PaymentEntity payment) {
        if (payment == null) {
            throw notFound();
        }
        return PaymentResponse.builder()
                .id(payment.getId()).paymentNo(payment.getPaymentNo())
                .orderId(payment.getOrderId()).amount(payment.getAmount())
                .status(PaymentStatus.valueOf(payment.getStatusCode()))
                .providerTransactionNo(payment.getProviderTransactionNo())
                .createdAt(payment.getCreatedAt()).updatedAt(payment.getUpdatedAt())
                .paidAt(payment.getPaidAt()).closedAt(payment.getClosedAt()).build();
    }
}
