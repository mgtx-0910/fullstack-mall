package com.example.fullstackmall.service.order;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.inventory.service.SkuDbService;
import com.example.fullstackmall.service.order.entity.OrderItemEntity;
import com.example.fullstackmall.service.order.service.OrderDbService;
import com.example.fullstackmall.service.order.service.OrderItemDbService;
import com.example.fullstackmall.service.payment.service.PaymentDbService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 单张订单超时关闭事务。每张订单独立提交，避免整批大事务。
 */
@Service
public class OrderCloseTransactionService {

    @Resource
    private OrderDbService orderDbService;

    @Resource
    private OrderItemDbService orderItemDbService;

    @Resource
    private SkuDbService skuDbService;

    @Resource
    private PaymentDbService paymentDbService;

    /**
     * 每张超时订单使用独立事务关闭，避免一张订单失败导致整个扫描批次回滚。
     * 返回 false 表示订单已被其他并发操作处理，不属于系统异常。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean closeExpiredOrder(Long orderId, LocalDateTime now) {
        // 与支付回调保持统一锁顺序：支付行 → 订单行 → SKU 行，降低交叉死锁风险。
        paymentDbService.selectByOrderIdForUpdate(orderId);
        // 条件 UPDATE 只关闭仍为待支付且已过期的订单；返回 0 表示无需处理或已被其他线程抢先处理。
        if (orderDbService.closeExpiredOrder(orderId, now) != 1) {
            return false;
        }
        List<OrderItemEntity> items = orderItemDbService.listByOrderId(orderId)
                .stream().sorted(Comparator.comparing(OrderItemEntity::getSkuId)).toList();
        if (items.isEmpty()) {
            throw conflict();
        }
        for (OrderItemEntity item : items) {
            if (skuDbService.releaseLockedStock(item.getSkuId(), item.getQuantity()) != 1) {
                throw conflict();
            }
        }
        paymentDbService.closePendingByOrderId(orderId);
        return true;
    }

    private BusinessException conflict() {
        return new BusinessException(ApiCode.ORDER_CLOSE_STOCK_RELEASE_FAILED,
                ApiCode.ORDER_CLOSE_STOCK_RELEASE_FAILED.defaultMessage(), HttpStatus.CONFLICT);
    }
}
