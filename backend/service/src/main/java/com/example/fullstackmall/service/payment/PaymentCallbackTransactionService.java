package com.example.fullstackmall.service.payment;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.order.OrderStatus;
import com.example.fullstackmall.contract.payment.MockPaymentCallbackRequest;
import com.example.fullstackmall.contract.payment.PaymentStatus;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.inventory.service.SkuDbService;
import com.example.fullstackmall.service.order.entity.OrderEntity;
import com.example.fullstackmall.service.order.entity.OrderItemEntity;
import com.example.fullstackmall.service.order.service.OrderDbService;
import com.example.fullstackmall.service.order.service.OrderItemDbService;
import com.example.fullstackmall.service.payment.entity.PaymentEntity;
import com.example.fullstackmall.service.payment.service.PaymentDbService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 支付成功回调事务：支付、订单和库存必须同时成功或同时回滚。
 */
@Service
public class PaymentCallbackTransactionService {

    @Resource
    private PaymentDbService paymentDbService;

    @Resource
    private OrderDbService orderDbService;

    @Resource
    private OrderItemDbService orderItemDbService;

    @Resource
    private SkuDbService skuDbService;

    /**
     * 在同一事务中处理成功回调、更新支付与订单状态并确认锁定库存。
     * 任一状态条件或库存确认失败都会整体回滚，不能出现“支付成功但订单未支付”。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long handleSuccess(MockPaymentCallbackRequest request) {
        // 先悲观锁定支付行，使同一支付的重复回调和关单竞争串行化。
        PaymentEntity payment = paymentDbService.selectByPaymentNoForUpdate(request.getPaymentNo());
        if (payment == null) {
            throw new BusinessException(ApiCode.PAYMENT_NOT_FOUND,
                    ApiCode.PAYMENT_NOT_FOUND.defaultMessage(), HttpStatus.NOT_FOUND);
        }
        if (PaymentStatus.SUCCESS.name().equals(payment.getStatusCode())) {
            // 支付成功后重复收到同一业务结果，直接返回原记录，不再次确认库存。
            return payment.getId();
        }
        if (!PaymentStatus.PENDING.name().equals(payment.getStatusCode())) {
            throw conflict(ApiCode.PAYMENT_STATUS_CONFLICT);
        }

        OrderEntity order = orderDbService.getById(payment.getOrderId());
        if (order == null) {
            throw new BusinessException(ApiCode.ORDER_NOT_FOUND,
                    ApiCode.ORDER_NOT_FOUND.defaultMessage(), HttpStatus.NOT_FOUND);
        }
        if (payment.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw conflict(ApiCode.PAYMENT_AMOUNT_MISMATCH);
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatusCode())) {
            throw conflict(ApiCode.PAYMENT_STATUS_CONFLICT);
        }
        // 状态条件 UPDATE 是第二道并发保护：只有 PENDING_PAYMENT 才能改成 PAID。
        if (orderDbService.markPaid(order.getId()) != 1) {
            throw conflict(ApiCode.PAYMENT_STATUS_CONFLICT);
        }
        if (paymentDbService.markSuccess(payment.getId(),
                request.getProviderTransactionNo(), request.getCallbackId()) != 1) {
            throw conflict(ApiCode.PAYMENT_STATUS_CONFLICT);
        }

        // 支付成功后把 locked_stock 真正消费掉；按 skuId 排序保持固定加锁顺序。
        List<OrderItemEntity> items = orderItemDbService.listByOrderId(order.getId())
                .stream().sorted(Comparator.comparing(OrderItemEntity::getSkuId)).toList();
        if (items.isEmpty()) {
            throw conflict(ApiCode.PAYMENT_STOCK_CONFIRM_FAILED);
        }
        for (OrderItemEntity item : items) {
            if (skuDbService.confirmLockedStock(item.getSkuId(), item.getQuantity()) != 1) {
                throw conflict(ApiCode.PAYMENT_STOCK_CONFIRM_FAILED);
            }
        }
        return payment.getId();
    }

    private BusinessException conflict(ApiCode apiCode) {
        return new BusinessException(apiCode, apiCode.defaultMessage(), HttpStatus.CONFLICT);
    }
}
