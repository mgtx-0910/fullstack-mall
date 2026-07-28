package com.example.fullstackmall.contract.order;

/**
 * 订单用例契约。
 */
public interface IOrderFacade {

    /**
     * 使用当前用户已勾选的购物车创建待支付订单。
     *
     * @param idempotencyKey 客户端重试使用的幂等键
     * @return 创建成功或同一幂等键已存在的订单
     */
    OrderResponse createOrder(String idempotencyKey);

    /**
     * 查询当前登录用户自己的订单，防止通过订单 ID 越权读取。
     *
     * @param orderId 订单主键
     * @return 订单及订单项快照
     */
    OrderResponse queryMyOrder(Long orderId);

    /**
     * 取消当前用户的待支付订单，并在同一事务中释放库存和关闭待支付流水。
     *
     * @param orderId 订单主键
     * @return 取消后的订单
     */
    OrderResponse cancelMyOrder(Long orderId);
}
