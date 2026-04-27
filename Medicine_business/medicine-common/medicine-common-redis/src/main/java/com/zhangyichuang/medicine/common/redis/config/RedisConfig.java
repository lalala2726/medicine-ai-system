package com.zhangyichuang.medicine.common.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author Chuang
 * <p>
 * created on 2025/2/19
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate配置
     *
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate
     */
    @Primary
    @Bean
    @SuppressWarnings(value = {"unchecked", "rawtypes"})
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 创建 GsonJsonRedisSerializer 序列化器，用于处理值序列化
        GsonJsonRedisSerializer serializer = new GsonJsonRedisSerializer(Object.class);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        // Hash的key也采用StringRedisSerializer的序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        // 重要：设置默认的序列化器，确保所有操作都使用相同的序列化规则
        template.setDefaultSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
