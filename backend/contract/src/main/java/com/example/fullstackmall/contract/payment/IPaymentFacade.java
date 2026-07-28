package com.example.fullstackmall.contract.payment;

/**
 * 支付用例契约。
 */
public interface IPaymentFacade {

    /**
     * 为当前用户的待支付订单创建或取得唯一支付流水。
     *
     * @param orderId 订单主键
     * @return 支付流水
     */
    PaymentResponse createMyPayment(Long orderId);

    /**
     * 查询当前用户订单对应的支付流水。
     *
     * @param orderId 订单主键
     * @return 支付流水
     */
    PaymentResponse queryMyPayment(Long orderId);

    /**
     * 处理本地模拟支付成功回调，并完成回调幂等、订单状态和库存确认。
     *
     * @param callbackSecret 请求头携带的模拟平台共享密钥
     * @param request 回调事件数据
     * @return 处理后的支付流水
     */
    PaymentResponse handleMockCallback(String callbackSecret, MockPaymentCallbackRequest request);
}
