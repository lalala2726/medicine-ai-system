package com.zhangyichuang.medicine.common.captcha.cache;

import cloud.tianai.captcha.cache.CacheStore;
import cloud.tianai.captcha.common.AnyMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于项目 RedisTemplate 的天爱验证码缓存实现。
 *
 * @author Chuang
 */
@RequiredArgsConstructor
public class TianaiRedisCacheStore implements CacheStore {

    /**
     * 项目统一 RedisTemplate。
     */
    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * 获取缓存内容。
     *
     * @param key 缓存键
     * @return 缓存值
     */
    @Override
    public AnyMap getCache(String key) {
        Object cachedValue = redisTemplate.opsForValue().get(key);
        return convertToAnyMap(cachedValue);
    }

    /**
     * 获取并删除缓存内容。
     *
     * @param key 缓存键
     * @return 缓存值
     */
    @Override
    public AnyMap getAndRemoveCache(String key) {
        ValueOperations<Object, Object> valueOperations = redisTemplate.opsForValue();
        Object cachedValue = valueOperations.getAndDelete(key);
        return convertToAnyMap(cachedValue);
    }

    /**
     * 写入缓存内容。
     *
     * @param key      缓存键
     * @param anyMap   缓存值
     * @param expire   过期时间
     * @param timeUnit 时间单位
     * @return true 表示写入成功
     */
    @Override
    public boolean setCache(String key, AnyMap anyMap, Long expire, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, anyMap, expire, timeUnit);
        return true;
    }

    /**
     * 对指定键执行自增。
     *
     * @param key      缓存键
     * @param val      自增值
     * @param expire   过期时间
     * @param timeUnit 时间单位
     * @return 自增后的值
     */
    @Override
    public Long incr(String key, long val, Long expire, TimeUnit timeUnit) {
        Long result = redisTemplate.opsForValue().increment(key, val);
        redisTemplate.expire(key, expire, timeUnit);
        return result;
    }

    /**
     * 读取长整型缓存值。
     *
     * @param key 缓存键
     * @return 缓存值
     */
    @Override
    public Long getLong(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * 关闭缓存实现。
     */
    @Override
    public void close() {
        // RedisTemplate 由 Spring 容器托管，这里无需手动释放资源。
    }

    /**
     * 将 Redis 读取结果转换为验证码组件可识别的 AnyMap。
     *
     * @param cachedValue Redis 读取出的原始缓存值
     * @return 验证码组件可识别的 AnyMap；无法转换时返回 null
     */
    @SuppressWarnings("unchecked")
    private AnyMap convertToAnyMap(Object cachedValue) {
        if (cachedValue instanceof AnyMap anyMap) {
            return anyMap;
        }
        if (!(cachedValue instanceof Map<?, ?> cachedMap)) {
            return null;
        }
        Map<String, Object> normalizedMap = new LinkedHashMap<>(cachedMap.size());
        for (Map.Entry<?, ?> entry : cachedMap.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            normalizedMap.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return new AnyMap(normalizedMap);
    }
}
