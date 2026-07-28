package com.example.fullstackmall.contract.inventory;

import java.util.List;

/**
 * SKU、售价与库存相关用例契约。
 */
public interface ISkuFacade {

    /**
     * 为未上架商品创建 SKU。
     *
     * @param productId 商品主键
     * @param request SKU 编码、规格、价格和初始库存
     * @return 创建后的 SKU
     */
    SkuResponse createSku(Long productId, SkuCreateRequest request);

    /**
     * 查询指定商品的全部 SKU，供管理员维护。
     *
     * @param productId 商品主键
     * @return SKU 列表
     */
    List<SkuResponse> queryAdminSkus(Long productId);

    /**
     * 查询已上架商品的公开 SKU；服务端会校验商品公开状态。
     *
     * @param productId 商品主键
     * @return 公开可见的 SKU 列表
     */
    List<SkuResponse> queryPublishedSkus(Long productId);

    /**
     * 使用期望版本号乐观锁修改 SKU 售价。
     *
     * @param skuId SKU 主键
     * @param request 新售价和期望版本
     * @return 更新后的 SKU
     */
    SkuResponse updatePrice(Long skuId, SkuPriceUpdateRequest request);

    /**
     * 使用期望版本号原子调整可售库存。
     *
     * @param skuId SKU 主键
     * @param request 库存变化量和期望版本
     * @return 更新后的 SKU
     */
    SkuResponse adjustStock(Long skuId, SkuStockAdjustRequest request);
}
