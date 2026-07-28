package com.example.fullstackmall.service.order;

import com.example.fullstackmall.service.order.service.OrderDbService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 扫描一批过期订单，并逐张调用独立事务 Bean。
 */
@Service
public class OrderCloseBatchService {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseBatchService.class);

    @Resource
    private OrderDbService orderDbService;

    @Resource
    private OrderCloseTransactionService orderCloseTransactionService;

    @Value("${mall.order.close-scan-batch-size:100}")
    private int batchSize;

    public int closeExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> orderIds = orderDbService.listExpiredPendingOrderIds(now, batchSize);
        int closedCount = 0;
        // 批次本身不包大事务，每个订单调用独立 Bean 的事务方法，单笔失败不会回滚整批。
        for (Long orderId : orderIds) {
            try {
                if (orderCloseTransactionService.closeExpiredOrder(orderId, now)) {
                    closedCount++;
                }
            } catch (RuntimeException exception) {
                log.error("关闭超时订单失败，orderId={}", orderId, exception);
            }
        }
        return closedCount;
    }
}
