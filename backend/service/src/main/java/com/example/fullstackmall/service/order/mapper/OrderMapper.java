package com.example.fullstackmall.service.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fullstackmall.service.order.entity.OrderEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单主表 Mapper。所有状态流转都要求旧状态仍为 PENDING_PAYMENT。
 */
public interface OrderMapper extends BaseMapper<OrderEntity> {

    /** 在事务中锁定订单行，锁持续到事务结束。 */
    @Select("""
            SELECT *
            FROM mall_order
            WHERE id = #{orderId}
            FOR UPDATE
            """)
    OrderEntity selectByIdForUpdate(@Param("orderId") Long orderId);

    /** 仅允许订单所有者把待支付订单原子更新为已取消。 */
    @Update("""
            UPDATE mall_order
            SET status_code = 'CANCELLED',
                cancelled_at = CURRENT_TIMESTAMP(3),
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{orderId}
              AND user_id = #{userId}
              AND status_code = 'PENDING_PAYMENT'
            """)
    int cancelPendingOrder(@Param("orderId") Long orderId, @Param("userId") Long userId);

    /** 仅允许待支付订单原子更新为已支付。 */
    @Update("""
            UPDATE mall_order
            SET status_code = 'PAID',
                paid_at = CURRENT_TIMESTAMP(3),
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{orderId}
              AND status_code = 'PENDING_PAYMENT'
            """)
    int markPaid(@Param("orderId") Long orderId);

    /** 仅允许已经到期的待支付订单原子更新为已关闭。 */
    @Update("""
            UPDATE mall_order
            SET status_code = 'CLOSED',
                closed_at = CURRENT_TIMESTAMP(3),
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{orderId}
              AND status_code = 'PENDING_PAYMENT'
              AND expires_at <= #{now}
            """)
    int closeExpiredOrder(@Param("orderId") Long orderId, @Param("now") LocalDateTime now);

    /**
     * 按 ID 顺序分批扫描到期订单，避免定时任务一次加载或锁定过多数据。
     */
    @Select("""
            SELECT id
            FROM mall_order
            WHERE status_code = 'PENDING_PAYMENT'
              AND expires_at <= #{now}
            ORDER BY id
            LIMIT #{batchSize}
            """)
    List<Long> selectExpiredPendingOrderIds(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );
}
