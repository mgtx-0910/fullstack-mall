package com.example.fullstackmall.contract.cart;

/**
 * 购物车用例契约。
 */
public interface ICartFacade {

    /**
     * 把 SKU 加入当前登录用户的购物车；用户 ID 只从登录上下文获取。
     *
     * @param request SKU 和购买数量
     * @return 新增后的购物车行
     */
    CartItemResponse addItem(CartAddRequest request);

    /**
     * 查询当前登录用户的购物车，并批量组装商品、SKU、价格和库存当前值。
     *
     * @return 当前用户购物车
     */
    CartResponse queryMyCart();
}
