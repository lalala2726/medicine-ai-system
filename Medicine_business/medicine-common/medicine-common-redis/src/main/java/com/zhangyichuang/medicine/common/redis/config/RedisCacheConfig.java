package com.zhangyichuang.medicine.common.redis.config;

import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置，负责开启 Spring Cache 并统一 Redis 序列化与 TTL 设置。
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
                                     RedisCacheConfiguration redisCacheConfiguration) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(
                RedisConstants.MallProduct.CACHE_NAME,
                redisCacheConfiguration
                        .entryTtl(Duration.ofSeconds(RedisConstants.MallProduct.CACHE_TTL_SECONDS))
                        .computePrefixWith(cacheName -> RedisConstants.MallProduct.KEY_PREFIX)
        );

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
