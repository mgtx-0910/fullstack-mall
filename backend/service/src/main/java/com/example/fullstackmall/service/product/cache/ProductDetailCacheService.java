package com.example.fullstackmall.service.product.cache;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.product.ProductDetailResponse;
import com.example.fullstackmall.service.cache.RedisOperatorClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 商品详情缓存服务：统一负责 key、JSON、TTL、空值标记、失效和 Redis 降级。
 *
 * <p>这里实现 Cache Aside（旁路缓存），Redis 只是 MySQL 前面的性能层，
 * 商品真实数据仍以数据库为准。</p>
 */
@Service
public class ProductDetailCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductDetailCacheService.class);
    private static final String KEY_PREFIX = "mall:product:detail:v1:";
    private static final String NULL_PREFIX = "__NULL__:";

    @Resource
    private RedisOperatorClient redisOperatorClient;

    /** 使用 Spring Boot 已配置的 ObjectMapper，确保 LocalDateTime 等类型规则和 HTTP JSON 一致。 */
    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ProductDetailCacheProperties properties;

    /**
     * 查询商品详情缓存。Redis 不可用时返回 DEGRADED，让 Facade 继续查 MySQL。
     */
    public ProductDetailCacheLookup lookup(Long productId) {
        if (!properties.isEnabled()) {
            LOGGER.debug("product_detail_cache event=BYPASS productId={}", productId);
            return ProductDetailCacheLookup.bypass();
        }

        String key = buildKey(productId);
        String cachedValue;
        try {
            cachedValue = redisOperatorClient.get(key);
        } catch (RuntimeException exception) {
            // fail-open：缓存故障不能让原本可用的商品详情接口一起不可用。
            LOGGER.warn("product_detail_cache event=DEGRADED operation=GET productId={}", productId, exception);
            return ProductDetailCacheLookup.degraded();
        }

        if (cachedValue == null) {
            LOGGER.info("product_detail_cache event=MISS productId={}", productId);
            return ProductDetailCacheLookup.miss();
        }

        if (cachedValue.startsWith(NULL_PREFIX)) {
            ApiCode apiCode = parseNullApiCode(cachedValue);
            if (apiCode != null) {
                LOGGER.info("product_detail_cache event=NULL_HIT productId={} code={}", productId, apiCode);
                return ProductDetailCacheLookup.nullHit(apiCode);
            }
            // 非法空值标记也属于坏缓存，删除后回源，不能把未知字符串当成业务 404。
            evictBrokenValue(productId, key, "invalid null marker");
            return ProductDetailCacheLookup.miss();
        }

        try {
            ProductDetailResponse response = objectMapper.readValue(cachedValue, ProductDetailResponse.class);
            if (response == null) {
                evictBrokenValue(productId, key, "JSON result is null");
                return ProductDetailCacheLookup.miss();
            }
            LOGGER.info("product_detail_cache event=HIT productId={}", productId);
            return ProductDetailCacheLookup.hit(response);
        } catch (JsonProcessingException exception) {
            // 坏 JSON 如果不删除，每次请求都会重复反序列化失败，直到 TTL 到期。
            evictBrokenValue(productId, key, exception.getOriginalMessage());
            return ProductDetailCacheLookup.miss();
        }
    }

    /**
     * 回填正常详情。序列化或 Redis 写入失败只记录降级，不影响本次数据库查询结果。
     */
    public void put(Long productId, ProductDetailResponse response) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(response);
            redisOperatorClient.setEx(buildKey(productId), json, properties.getValueTtl());
            LOGGER.info("product_detail_cache event=PUT productId={} ttl={}", productId, properties.getValueTtl());
        } catch (JsonProcessingException | RuntimeException exception) {
            LOGGER.warn("product_detail_cache event=DEGRADED operation=PUT productId={}", productId, exception);
        }
    }

    /**
     * 回填短 TTL 空值，缓解同一个不存在商品 ID 被反复请求造成的缓存穿透。
     */
    public void putNull(Long productId, ApiCode apiCode) {
        if (!properties.isEnabled() || !isCacheableNotFound(apiCode)) {
            return;
        }
        try {
            redisOperatorClient.setEx(
                    buildKey(productId),
                    NULL_PREFIX + apiCode.name(),
                    properties.getNullTtl()
            );
            LOGGER.info("product_detail_cache event=PUT_NULL productId={} code={} ttl={}",
                    productId, apiCode, properties.getNullTtl());
        } catch (RuntimeException exception) {
            LOGGER.warn("product_detail_cache event=DEGRADED operation=PUT_NULL productId={}", productId, exception);
        }
    }

    /**
     * 数据库写成功后删除旧缓存。删除失败不回滚数据库，由 TTL 作为最终兜底。
     */
    public void evict(Long productId) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            redisOperatorClient.delete(buildKey(productId));
            LOGGER.info("product_detail_cache event=EVICT productId={}", productId);
        } catch (RuntimeException exception) {
            LOGGER.warn("product_detail_cache event=DEGRADED operation=EVICT productId={}", productId, exception);
        }
    }

    /** 固定版本前缀便于未来缓存结构变化时平滑切换到 v2。 */
    String buildKey(Long productId) {
        return KEY_PREFIX + productId;
    }

    private ApiCode parseNullApiCode(String cachedValue) {
        String codeText = cachedValue.substring(NULL_PREFIX.length());
        try {
            ApiCode apiCode = ApiCode.valueOf(codeText);
            return isCacheableNotFound(apiCode) ? apiCode : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isCacheableNotFound(ApiCode apiCode) {
        return apiCode == ApiCode.PRODUCT_NOT_FOUND || apiCode == ApiCode.CATEGORY_NOT_FOUND;
    }

    private void evictBrokenValue(Long productId, String key, String reason) {
        LOGGER.warn("product_detail_cache event=CORRUPTED productId={} reason={}", productId, reason);
        try {
            redisOperatorClient.delete(key);
            LOGGER.info("product_detail_cache event=EVICT productId={} reason=corrupted", productId);
        } catch (RuntimeException exception) {
            LOGGER.warn("product_detail_cache event=DEGRADED operation=EVICT_CORRUPTED productId={}",
                    productId, exception);
        }
    }
}
