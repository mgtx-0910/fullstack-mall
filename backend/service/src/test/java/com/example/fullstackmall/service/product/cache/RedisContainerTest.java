package com.example.fullstackmall.service.product.cache;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.product.ProductDetailResponse;
import com.example.fullstackmall.contract.product.ProductStatus;
import com.example.fullstackmall.service.cache.RedisOperatorClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用真实 Redis 容器验证 SET + TTL + GET + DEL。
 *
 * <p>默认不运行，只有显式设置 RUN_REDIS_CONTAINER_TESTS=true 才启动 Docker，
 * 这样日常 H2 测试不会依赖本机 Redis。</p>
 */
@Testcontainers
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_CONTAINER_TESTS", matches = "true")
class RedisContainerTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @Test
    void shouldStoreReadExpireAndEvictProductCache() {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
                REDIS.getHost(),
                REDIS.getMappedPort(6379)
        );
        connectionFactory.afterPropertiesSet();

        try {
            StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
            template.afterPropertiesSet();

            RedisOperatorClient redisOperatorClient = new RedisOperatorClient();
            ReflectionTestUtils.setField(redisOperatorClient, "stringRedisTemplate", template);

            ProductDetailCacheProperties properties = new ProductDetailCacheProperties();
            properties.setEnabled(true);
            properties.setValueTtl(Duration.ofMinutes(10));
            properties.setNullTtl(Duration.ofSeconds(30));

            ProductDetailCacheService cacheService = new ProductDetailCacheService();
            ReflectionTestUtils.setField(cacheService, "redisOperatorClient", redisOperatorClient);
            ReflectionTestUtils.setField(cacheService, "objectMapper", new ObjectMapper().findAndRegisterModules());
            ReflectionTestUtils.setField(cacheService, "properties", properties);

            ProductDetailResponse response = sampleResponse();
            cacheService.put(1L, response);

            ProductDetailCacheLookup hit = cacheService.lookup(1L);
            assertThat(hit.getStatus()).isEqualTo(ProductDetailCacheStatus.HIT);
            assertThat(hit.getResponse()).isEqualTo(response);
            assertThat(template.getExpire(cacheService.buildKey(1L), TimeUnit.SECONDS))
                    .isBetween(1L, 600L);

            cacheService.putNull(999L, ApiCode.PRODUCT_NOT_FOUND);
            ProductDetailCacheLookup nullHit = cacheService.lookup(999L);
            assertThat(nullHit.getStatus()).isEqualTo(ProductDetailCacheStatus.NULL_HIT);
            assertThat(template.getExpire(cacheService.buildKey(999L), TimeUnit.SECONDS))
                    .isBetween(1L, 30L);

            cacheService.evict(1L);
            assertThat(cacheService.lookup(1L).getStatus()).isEqualTo(ProductDetailCacheStatus.MISS);
        } finally {
            connectionFactory.destroy();
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
