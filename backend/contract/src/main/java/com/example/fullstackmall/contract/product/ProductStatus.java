package com.example.fullstackmall.contract.product;

/**
 * 商品生命周期状态。
 */
public enum ProductStatus {
    /** 草稿：只能由管理员维护，匿名用户不可见。 */
    DRAFT,
    /** 已上架：匿名用户可以查询，且上架时必须存在可售 SKU。 */
    ON_SALE,
    /** 已下架：保留历史数据，但匿名用户不可见。 */
    OFF_SHELF
}
