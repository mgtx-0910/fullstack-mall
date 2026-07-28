package com.example.fullstackmall.contract.common;

/**
 * 对外 API 使用的稳定业务码。
 *
 * <p>前端可以根据 code 判断业务结果，不应解析 message 文案。</p>
 */
public enum ApiCode {
    // 通用成功、请求格式与参数校验。
    SUCCESS("操作成功"),
    VALIDATION_ERROR("请求参数校验失败"),
    MALFORMED_JSON("请求 JSON 格式错误"),
    // 分类与商品生命周期。
    CATEGORY_NAME_RESERVED("分类名称不可使用"),
    CATEGORY_NOT_FOUND("商品分类不存在或已停用"),
    PRODUCT_NOT_FOUND("商品不存在"),
    INVALID_PRODUCT_STATUS_TRANSITION("商品状态转换不合法"),
    // SKU、金额和库存并发。
    SKU_NOT_FOUND("SKU 不存在"),
    SKU_CODE_ALREADY_EXISTS("SKU 编码已存在"),
    PRODUCT_SKU_NOT_EDITABLE("已上架商品不能新增 SKU"),
    PRODUCT_NOT_READY_FOR_SALE("商品至少需要一个有价格且有库存的 SKU 才能上架"),
    INVENTORY_VERSION_CONFLICT("库存数据已被其他请求修改，请刷新后重试"),
    INSUFFICIENT_STOCK("可售库存不足"),
    // 购物车。
    CART_ITEM_ALREADY_EXISTS("该 SKU 已在购物车中，请修改数量"),
    CART_SKU_NOT_SALEABLE("该 SKU 当前不可加入购物车"),
    CART_QUANTITY_EXCEEDS_STOCK("加入数量超过当前可售库存"),
    // 订单、幂等、状态流转和库存释放。
    ORDER_IDEMPOTENCY_KEY_INVALID("Idempotency-Key 必须为 8 到 64 位字母、数字或 ._:-"),
    ORDER_CART_EMPTY("没有已勾选的购物车商品"),
    ORDER_CART_CHANGED("购物车内容已发生变化，请刷新后重试"),
    ORDER_ITEM_NOT_SALEABLE("订单中存在当前不可售商品"),
    ORDER_INSUFFICIENT_STOCK("订单商品库存不足"),
    ORDER_NOT_FOUND("订单不存在"),
    ORDER_STATUS_CONFLICT("当前订单状态不允许此操作"),
    ORDER_STOCK_RELEASE_FAILED("订单锁定库存释放失败"),
    ORDER_CLOSE_STOCK_RELEASE_FAILED("超时关单释放库存失败"),
    // 支付流水、回调幂等和支付库存确认。
    PAYMENT_NOT_FOUND("支付流水不存在"),
    PAYMENT_ORDER_NOT_PAYABLE("当前订单不能创建支付流水"),
    PAYMENT_STATUS_CONFLICT("当前支付状态不允许此操作"),
    PAYMENT_AMOUNT_MISMATCH("支付金额与订单金额不一致"),
    PAYMENT_CALLBACK_SECRET_INVALID("模拟支付回调密钥错误"),
    PAYMENT_CALLBACK_DUPLICATE("回调事件 ID 已被其他支付流水使用"),
    PAYMENT_STOCK_CONFIRM_FAILED("确认锁定库存失败"),
    // 认证、授权和通用资源错误。
    USERNAME_ALREADY_EXISTS("用户名已存在"),
    INVALID_CREDENTIALS("用户名或密码错误"),
    ACCOUNT_DISABLED("账号已停用"),
    UNAUTHORIZED("请先登录或重新登录"),
    FORBIDDEN("没有权限访问该资源"),
    NOT_FOUND("请求的资源不存在"),
    INTERNAL_ERROR("服务器内部错误");

    /** 业务码对应的默认中文提示；前端逻辑应依赖稳定 code，而不是解析这段文案。 */
    private final String defaultMessage;

    ApiCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    /** 返回默认提示文案；调用方可以覆盖 message，但不能改变业务码本身的语义。 */
    public String defaultMessage() {
        return defaultMessage;
    }
}
