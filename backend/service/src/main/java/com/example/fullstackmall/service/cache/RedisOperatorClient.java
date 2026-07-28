package com.example.fullstackmall.service.cache;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 字符串操作的统一封装。
 *
 * <p>Spring Data Redis 提供 {@link StringRedisTemplate}，默认通过 Lettuce 客户端连接 Redis。
 * 业务代码只依赖这个封装，避免在各个 Facade 中散落 GET、SET、DEL 等底层命令。</p>
 */
@Component
public class RedisOperatorClient {

    /** Spring Boot 根据 spring.data.redis 配置自动创建的字符串 Redis 模板。 */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 读取字符串值，对应 Redis 的 GET 命令。
     */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 写入字符串并同时设置过期时间，对应 Redis 的 SET key value EX/PX 命令。
     * 必须同时设置 TTL，防止业务缓存永久占用内存或长期保存旧数据。
     */
    public void setEx(String key, String value, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * 删除 key，对应 Redis 的 DEL 命令。
     */
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }
}
