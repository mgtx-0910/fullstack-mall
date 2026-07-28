package com.example.fullstackmall.service.product.cache;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.product.ProductDetailResponse;
import lombok.Data;

/**
 * 一次商品详情缓存查询的结构化结果。
 *
 * <p>普通对象比直接返回 null 更清楚：调用方能区分“未命中”“Redis 故障”和“命中空值”。</p>
 */
@Data
public class ProductDetailCacheLookup {

    /** 本次缓存查询状态。 */
    private ProductDetailCacheStatus status;

    /** 只有 HIT 时才有正常商品详情。 */
    private ProductDetailResponse response;

    /** 只有 NULL_HIT 时才有需要恢复的 404 业务码。 */
    private ApiCode nullApiCode;

    public static ProductDetailCacheLookup hit(ProductDetailResponse response) {
        ProductDetailCacheLookup lookup = ofStatus(ProductDetailCacheStatus.HIT);
        lookup.setResponse(response);
        return lookup;
    }

    public static ProductDetailCacheLookup nullHit(ApiCode apiCode) {
        ProductDetailCacheLookup lookup = ofStatus(ProductDetailCacheStatus.NULL_HIT);
        lookup.setNullApiCode(apiCode);
        return lookup;
    }

    public static ProductDetailCacheLookup miss() {
        return ofStatus(ProductDetailCacheStatus.MISS);
    }

    public static ProductDetailCacheLookup degraded() {
        return ofStatus(ProductDetailCacheStatus.DEGRADED);
    }

    public static ProductDetailCacheLookup bypass() {
        return ofStatus(ProductDetailCacheStatus.BYPASS);
    }

    private static ProductDetailCacheLookup ofStatus(ProductDetailCacheStatus status) {
        ProductDetailCacheLookup lookup = new ProductDetailCacheLookup();
        lookup.setStatus(status);
        return lookup;
    }
}
