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

import java.util.List;

/**
 * 取消订单的事务边界：订单状态修改和锁定库存释放必须同时成功或同时回滚。
 */
@Service
public class OrderCancelTransactionService {

    @Resource
    private OrderDbService orderDbService;

    @Resource
    private OrderItemDbService orderItemDbService;

    @Resource
    private SkuDbService skuDbService;

    @Resource
    private PaymentDbService paymentDbService;

    /**
     * 在同一事务中取消待支付订单、释放全部锁定库存并关闭待支付流水。
     * 条件 UPDATE 影响行数不是 1 时抛异常，使已经执行的库存变更也一起回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId) {
        orderDbService.requireByIdAndUserId(orderId, userId);
        // 与支付回调保持统一锁顺序：支付行 → 订单行 → SKU 行，降低交叉死锁风险。
        paymentDbService.selectByOrderIdForUpdate(orderId);
        int affectedRows = orderDbService.cancelPendingOrder(orderId, userId);
        if (affectedRows != 1) {
            throw conflict(ApiCode.ORDER_STATUS_CONFLICT);
        }

        // 后续任一库存释放失败都会抛异常，使订单状态和之前已释放的库存一起回滚。
        List<OrderItemEntity> orderItems = orderItemDbService.listByOrderId(orderId);
        if (orderItems.isEmpty()) {
            throw conflict(ApiCode.ORDER_STOCK_RELEASE_FAILED);
        }
        for (OrderItemEntity item : orderItems.stream()
                .sorted(java.util.Comparator.comparing(OrderItemEntity::getSkuId)).toList()) {
            int releasedRows = skuDbService.releaseLockedStock(item.getSkuId(), item.getQuantity());
            if (releasedRows != 1) {
                throw conflict(ApiCode.ORDER_STOCK_RELEASE_FAILED);
            }
        }
        paymentDbService.closePendingByOrderId(orderId);
    }

    private BusinessException conflict(ApiCode apiCode) {
        return new BusinessException(apiCode, apiCode.defaultMessage(), HttpStatus.CONFLICT);
    }
}
