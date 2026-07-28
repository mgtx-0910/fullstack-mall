package com.example.fullstackmall.service.product.cache;

/**
 * 商品详情缓存查询结果。
 */
public enum ProductDetailCacheStatus {
    /** 命中可直接返回的正常商品详情。 */
    HIT,
    /** 命中短 TTL 空值标记，不需要重复查询数据库。 */
    NULL_HIT,
    /** Redis 中没有该 key，或者坏缓存已被删除。 */
    MISS,
    /** Redis 访问异常，本次请求降级查询 MySQL。 */
    DEGRADED,
    /** 配置关闭缓存，本次请求直接绕过 Redis。 */
    BYPASS
}
