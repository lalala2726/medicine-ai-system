package com.zhangyichuang.medicine.common.redis.core;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 通用的 Redis Set（无序集合）操作封装
 * 取消泛型要求，统一使用 String key 和 Object value
 * 提供 Set 数据结构的各种操作方法，包括批量扫描功能
 *
 * @author Chuang
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings(value = {"unchecked", "rawtypes"})
public final class RedisSetCache {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(RedisSetCache.class);

    private final static int scanCount = 1000;
    private final RedisTemplate redisTemplate;

    /**
     * 添加一个成员到集合
     */
    public Long add(final String key, final Object member) {
        return redisTemplate.opsForSet().add(key, member);
    }

    /**
     * 批量添加成员
     */
    public Long addAll(final String key, final Object... members) {
        return redisTemplate.opsForSet().add(key, members);
    }

    /**
     * 判断成员是否存在
     */
    public Boolean isMember(final String key, final Object member) {
        return redisTemplate.opsForSet().isMember(key, member);
    }

    /**
     * 获取所有成员
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> members(final String key) {
        return (Set<T>) redisTemplate.opsForSet().members(key);
    }

    /**
     * 从集合中移除成员
     */
    public Long remove(final String key, final Object... members) {
        return redisTemplate.opsForSet().remove(key, members);
    }

    /**
     * 获取集合大小
     */
    public Long size(final String key) {
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 随机弹出并返回一个成员
     */
    @SuppressWarnings("unchecked")
    public <T> T pop(final String key) {
        return (T) redisTemplate.opsForSet().pop(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(final String key, final long timeout, final TimeUnit timeUnit) {
        return redisTemplate.expire(key, timeout, timeUnit);
    }

    /**
     * 批量键扫描方法 - 使用 Redis SCAN 操作高效获取匹配的 Set 键
     * 推荐在生产环境中使用此方法，避免阻塞 Redis 服务器
     *
     * @param keyPattern Redis 键模式，支持通配符（如：auth:session:index:*）
     * @return 匹配的键列表，如果没有匹配的键则返回空列表
     */
    public List<String> scanKeys(final String keyPattern) {
        if (!StringUtils.hasText(keyPattern)) {
            return new ArrayList<>();
        }

        List<String> keys = new ArrayList<>();
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(keyPattern)
                    .count(scanCount)
                    .build();

            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }


        } catch (Exception e) {
            log.error("Redis Set scan keys failed, pattern: {}, error: {}", keyPattern, e.getMessage(), e);
            // 发生异常时返回空列表，避免影响业务流程
            return new ArrayList<>();
        }

        return keys;
    }

    /**
     * 批量值获取方法 - 获取匹配模式的 Set 键及其所有成员
     * 使用 Redis SCAN 操作高效扫描键，然后批量获取每个 Set 的所有成员
     *
     * @param keyPattern Redis 键模式，支持通配符（如：auth:permissions:*）
     * @return Map，键为Set键，值为该Set的所有成员，如果没有匹配的键则返回空Map
     */
    public Map<String, Set<Object>> scanKeysWithValues(final String keyPattern) {
        if (!StringUtils.hasText(keyPattern)) {
            return new HashMap<>();
        }

        // 首先扫描获取所有匹配的键
        List<String> keys = scanKeys(keyPattern);
        if (keys.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Set<Object>> result = new HashMap<>();
        try {
            // 逐个获取每个 Set 的所有成员
            for (String key : keys) {
                Set<Object> setMembers = redisTemplate.opsForSet().members(key);
                result.put(key, setMembers);
            }
        } catch (Exception e) {
            log.error("Redis Set scan keys with values failed, pattern: {}, error: {}", keyPattern, e.getMessage(), e);
            return new HashMap<>();
        }

        return result;
    }


}
