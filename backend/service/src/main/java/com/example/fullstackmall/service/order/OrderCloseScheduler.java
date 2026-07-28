package com.example.fullstackmall.service.order;

import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时触发超时订单扫描。Scheduler 本身不持有数据库事务。
 */
@Component
public class OrderCloseScheduler {

    @Resource
    private OrderCloseBatchService orderCloseBatchService;

    /** fixedDelay 表示上一次扫描结束后再等待指定时间，避免扫描任务无间隔重叠执行。 */
    @Scheduled(
            fixedDelayString = "${mall.order.close-scan-fixed-delay-ms:60000}",
            initialDelayString = "${mall.order.close-scan-fixed-delay-ms:60000}"
    )
    public void closeExpiredOrders() {
        orderCloseBatchService.closeExpiredOrders();
    }
}
