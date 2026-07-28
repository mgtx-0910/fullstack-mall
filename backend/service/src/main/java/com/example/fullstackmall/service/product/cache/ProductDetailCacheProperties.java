package com.example.fullstackmall.service.product.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 商品详情缓存配置。
 *
 * <p>{@code @ConfigurationProperties} 会把 application.yml 中
 * {@code mall.cache.product-detail} 下的配置绑定到普通 Java 字段。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "mall.cache.product-detail")
public class ProductDetailCacheProperties {

    /** 是否启用缓存；关闭后请求直接查询 MySQL，便于本地排障和测试。 */
    private boolean enabled = true;

    /** 正常商品详情的存活时间，默认 10 分钟。 */
    private Duration valueTtl = Duration.ofMinutes(10);

    /** 商品或分类不可用时空值标记的存活时间，默认 30 秒。 */
    private Duration nullTtl = Duration.ofSeconds(30);
}
