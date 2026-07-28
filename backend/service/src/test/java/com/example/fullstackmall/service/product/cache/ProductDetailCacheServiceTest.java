package com.example.fullstackmall.service.product.cache;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.product.ProductDetailResponse;
import com.example.fullstackmall.contract.product.ProductStatus;
import com.example.fullstackmall.service.cache.RedisOperatorClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品详情缓存服务单元测试，不启动真实 Redis，专注验证缓存协议和降级分支。
 */
class ProductDetailCacheServiceTest {

    private RedisOperatorClient redisOperatorClient;
    private ObjectMapper objectMapper;
    private ProductDetailCacheProperties properties;
    private ProductDetailCacheService cacheService;

    @BeforeEach
    void setUp() {
        redisOperatorClient = mock(RedisOperatorClient.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        properties = new ProductDetailCacheProperties();
        properties.setEnabled(true);
        properties.setValueTtl(Duration.ofMinutes(10));
        properties.setNullTtl(Duration.ofSeconds(30));

        cacheService = new ProductDetailCacheService();
        ReflectionTestUtils.setField(cacheService, "redisOperatorClient", redisOperatorClient);
        ReflectionTestUtils.setField(cacheService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(cacheService, "properties", properties);
    }

    @Test
    void shouldReturnMissWhenKeyDoesNotExist() {
        when(redisOperatorClient.get(cacheService.buildKey(1L))).thenReturn(null);

        ProductDetailCacheLookup lookup = cacheService.lookup(1L);

        assertThat(lookup.getStatus()).isEqualTo(ProductDetailCacheStatus.MISS);
    }

    @Test
    void shouldDeserializeNormalCacheHit() throws Exception {
        ProductDetailResponse response = sampleResponse();
        when(redisOperatorClient.get(cacheService.buildKey(1L)))
                .thenReturn(objectMapper.writeValueAsString(response));

        ProductDetailCacheLookup lookup = cacheService.lookup(1L);

        assertThat(lookup.getStatus()).isEqualTo(ProductDetailCacheStatus.HIT);
        assertThat(lookup.getResponse()).isEqualTo(response);
    }

    @Test
    void shouldRestoreBusinessCodeFromNullCacheHit() {
        when(redisOperatorClient.get(cacheService.buildKey(9L)))
                .thenReturn("__NULL__:CATEGORY_NOT_FOUND");

        ProductDetailCacheLookup lookup = cacheService.lookup(9L);

        assertThat(lookup.getStatus()).isEqualTo(ProductDetailCacheStatus.NULL_HIT);
        assertThat(lookup.getNullApiCode()).isEqualTo(ApiCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void shouldDeleteBrokenJsonAndTreatItAsMiss() {
        String key = cacheService.buildKey(1L);
        when(redisOperatorClient.get(key)).thenReturn("{broken-json");

        ProductDetailCacheLookup lookup = cacheService.lookup(1L);

        assertThat(lookup.getStatus()).isEqualTo(ProductDetailCacheStatus.MISS);
        verify(redisOperatorClient).delete(key);
    }

    @Test
    void shouldDegradeWhenRedisReadFails() {
        when(redisOperatorClient.get(cacheService.buildKey(1L)))
                .thenThrow(new IllegalStateException("redis unavailable"));

        ProductDetailCacheLookup lookup = cacheService.lookup(1L);

        assertThat(lookup.getStatus()).isEqualTo(ProductDetailCacheStatus.DEGRADED);
    }

    @Test
    void shouldUseDifferentTtlForValueAndNullMarker() {
        ProductDetailResponse response = sampleResponse();

        cacheService.put(1L, response);
        cacheService.putNull(2L, ApiCode.PRODUCT_NOT_FOUND);

        verify(redisOperatorClient).setEx(
                cacheService.buildKey(1L),
                objectMapperWrite(response),
                Duration.ofMinutes(10)
        );
        verify(redisOperatorClient).setEx(
                cacheService.buildKey(2L),
                "__NULL__:PRODUCT_NOT_FOUND",
                Duration.ofSeconds(30)
        );
    }

    @Test
    void shouldFailOpenWhenRedisWriteOrDeleteFails() {
        org.mockito.Mockito.doThrow(new IllegalStateException("write failed"))
                .when(redisOperatorClient)
                .setEx(cacheService.buildKey(1L), objectMapperWrite(sampleResponse()), Duration.ofMinutes(10));
        when(redisOperatorClient.delete(cacheService.buildKey(1L)))
                .thenThrow(new IllegalStateException("delete failed"));

        assertThatCode(() -> cacheService.put(1L, sampleResponse())).doesNotThrowAnyException();
        assertThatCode(() -> cacheService.evict(1L)).doesNotThrowAnyException();
    }

    @Test
    void shouldBypassRedisWhenCacheIsDisabled() {
        properties.setEnabled(false);

        ProductDetailCacheLookup lookup = cacheService.lookup(1L);

        assertThat(lookup.getStatus()).isEqualTo(ProductDetailCacheStatus.BYPASS);
        verify(redisOperatorClient, never()).get(cacheService.buildKey(1L));
    }

    private String objectMapperWrite(ProductDetailResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ProductDetailResponse sampleResponse() {
        LocalDateTime time = LocalDateTime.of(2026, 7, 12, 12, 0);
        return new ProductDetailResponse(
                1L,
                1L,
                "手机数码",
                "二手手机",
                "自用好物",
                "九成新",
                ProductStatus.ON_SALE,
                5L,
                time,
                time
        );
    }
}
