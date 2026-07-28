package com.example.fullstackmall.service.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fullstackmall.service.payment.entity.PaymentEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 支付流水 Mapper。回调先锁定支付行，再执行状态条件更新。
 */
public interface PaymentMapper extends BaseMapper<PaymentEntity> {

    /** 按支付单号锁定支付行，用于串行处理重复回调。 */
    @Select("""
            SELECT *
            FROM mall_payment
            WHERE payment_no = #{paymentNo}
            FOR UPDATE
            """)
    PaymentEntity selectByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);

    /** 按订单 ID 锁定支付行，用于取消、回调和关单之间的并发协调。 */
    @Select("""
            SELECT *
            FROM mall_payment
            WHERE order_id = #{orderId}
            FOR UPDATE
            """)
    PaymentEntity selectByOrderIdForUpdate(@Param("orderId") Long orderId);

    /**
     * 只允许 PENDING → SUCCESS；影响行数为 0 表示其他事务已先改变状态。
     */
    @Update("""
            UPDATE mall_payment
            SET status_code = 'SUCCESS',
                provider_transaction_no = #{providerTransactionNo},
                callback_id = #{callbackId},
                paid_at = CURRENT_TIMESTAMP(3),
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{paymentId}
              AND status_code = 'PENDING'
            """)
    int markSuccess(
            @Param("paymentId") Long paymentId,
            @Param("providerTransactionNo") String providerTransactionNo,
            @Param("callbackId") String callbackId
    );

    /** 只关闭仍为 PENDING 的支付流水，不能覆盖 SUCCESS。 */
    @Update("""
            UPDATE mall_payment
            SET status_code = 'CLOSED',
                closed_at = CURRENT_TIMESTAMP(3),
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE order_id = #{orderId}
              AND status_code = 'PENDING'
            """)
    int closePendingByOrderId(@Param("orderId") Long orderId);
}
