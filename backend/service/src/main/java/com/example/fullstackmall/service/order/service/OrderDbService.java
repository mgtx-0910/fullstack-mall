package com.example.fullstackmall.service.order.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.order.entity.OrderEntity;
import com.example.fullstackmall.service.order.mapper.OrderMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单主表数据库访问 Service。用户订单查询必须同时带 orderId 和 userId。
 */
@Service
public class OrderDbService extends ServiceImpl<OrderMapper, OrderEntity> {

    public OrderEntity getByUserIdAndIdempotencyKey(Long userId, String idempotencyKey) {
        return lambdaQuery().eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getIdempotencyKey, idempotencyKey).one();
    }

    public OrderEntity getByIdAndUserId(Long orderId, Long userId) {
        return lambdaQuery().eq(OrderEntity::getId, orderId)
                .eq(OrderEntity::getUserId, userId).one();
    }

    public OrderEntity requireByIdAndUserId(Long orderId, Long userId) {
        OrderEntity order = getByIdAndUserId(orderId, userId);
        if (order == null) {
            throw new BusinessException(ApiCode.ORDER_NOT_FOUND,
                    ApiCode.ORDER_NOT_FOUND.defaultMessage(), HttpStatus.NOT_FOUND);
        }
        return order;
    }

    /**
     * 在当前事务内使用 SELECT ... FOR UPDATE 锁定订单行。
     * 锁会一直持有到事务提交或回滚，用来串行化同一订单的支付、取消和关单操作。
     */
    public OrderEntity selectByIdForUpdate(Long orderId) {
        return baseMapper.selectByIdForUpdate(orderId);
    }

    /**
     * 只有属于当前用户且仍为待支付的订单才能取消；影响行数为 0 表示状态已变化或无权操作。
     */
    public int cancelPendingOrder(Long orderId, Long userId) {
        return baseMapper.cancelPendingOrder(orderId, userId);
    }

    /**
     * 仅允许 PENDING_PAYMENT → PAID，条件 UPDATE 可防止重复回调覆盖终态。
     */
    public int markPaid(Long orderId) {
        return baseMapper.markPaid(orderId);
    }

    /**
     * 只有仍待支付且 expiresAt 已到期的订单才能关闭，调用方必须检查影响行数。
     */
    public int closeExpiredOrder(Long orderId, LocalDateTime now) {
        return baseMapper.closeExpiredOrder(orderId, now);
    }

    /**
     * 定时任务只批量扫描到期订单 ID，不在一个大事务里处理所有订单。
     * 后续每个 ID 进入独立事务，可缩短锁持有时间，也避免单个失败回滚整批数据。
     */
    public List<Long> listExpiredPendingOrderIds(LocalDateTime now, int batchSize) {
        return baseMapper.selectExpiredPendingOrderIds(now, batchSize);
    }
}
