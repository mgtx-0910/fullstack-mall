package com.example.fullstackmall.service.order.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fullstackmall.service.order.entity.OrderItemEntity;
import com.example.fullstackmall.service.order.mapper.OrderItemMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单项快照数据库访问 Service。
 */
@Service
public class OrderItemDbService extends ServiceImpl<OrderItemMapper, OrderItemEntity> {

    public List<OrderItemEntity> listByOrderId(Long orderId) {
        return lambdaQuery()
                .eq(OrderItemEntity::getOrderId, orderId)
                .orderByAsc(OrderItemEntity::getId)
                .list();
    }
}
