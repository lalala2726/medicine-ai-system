package com.zhangyichuang.medicine.common.redis.core;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 通用的 Redis List 操作封装
 * 取消泛型要求，统一使用 String key 和 Object value
 * 提供 List 数据结构的各种操作方法，包括批量扫描功能
 *
 * @author Chuang
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings(value = {"unchecked", "rawtypes"})
public final class RedisListCache {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(RedisListCache.class);

    private final static int scanCount = 1000;
    private final RedisTemplate redisTemplate;

    /**
     * 从左侧入队
     */
    public Long leftPush(final String key, final Object element) {
        return redisTemplate.opsForList().leftPush(key, element);
    }

    /**
     * 从右侧入队
     */
    public Long rightPush(final String key, final Object element) {
        return redisTemplate.opsForList().rightPush(key, element);
    }

    /**
     * 从左侧弹出
     */
    @SuppressWarnings("unchecked")
    public <T> T leftPop(final String key) {
        return (T) redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 从右侧弹出
     */
    @SuppressWarnings("unchecked")
    public <T> T rightPop(final String key) {
        return (T) redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 获取指定范围的数据（start~end，包括两端）
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> range(final String key, final long start, final long end) {
        return (List<T>) redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取列表长度
     */
    public Long size(final String key) {
        return redisTemplate.opsForList().size(key);
    }

    /**
     * 移除列表中等于指定元素的元素，count>0 删除左边第 count 个；count<0 删除右边；0 删除所有
     */
    public Long remove(final String key, final long count, final Object element) {
        return redisTemplate.opsForList().remove(key, count, element);
    }

    /**
     * 将列表修剪到指定区间
     */
    public void trim(final String key, final long start, final long end) {
        redisTemplate.opsForList().trim(key, start, end);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(final String key, final long timeout, final TimeUnit timeUnit) {
        return redisTemplate.expire(key, timeout, timeUnit);
    }

    /**
     * 批量键扫描方法 - 使用 Redis SCAN 操作高效获取匹配的 List 键
     * 推荐在生产环境中使用此方法，避免阻塞 Redis 服务器
     *
     * @param keyPattern Redis 键模式，支持通配符（如：queue:tasks:*）
     * @return 匹配的键列表，如果没有匹配的键则返回空列表
     */
    public List<String> scanKeys(final String keyPattern) {
        if (!StringUtils.hasText(keyPattern)) {
            log.warn("Redis List scan pattern is empty, returning empty list");
            return new ArrayList<>();
        }

        List<String> keys = new ArrayList<>();
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(keyPattern)
                    // 每次扫描的数量，可根据实际情况调整
                    .count(scanCount)
                    .build();

            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
        } catch (Exception e) {
            log.error("Redis List scan keys failed, pattern: {}, error: {}", keyPattern, e.getMessage(), e);
            // 发生异常时返回空列表，避免影响业务流程
            return new ArrayList<>();
        }

        return keys;
    }

    /**
     * 批量值获取方法 - 获取匹配模式的 List 键及其所有元素
     * 使用 Redis SCAN 操作高效扫描键，然后批量获取每个 List 的所有元素
     *
     * @param keyPattern Redis 键模式，支持通配符（如：queue:tasks:*）
     * @return Map，键为List键，值为该List的所有元素，如果没有匹配的键则返回空Map
     */
    public Map<String, List<Object>> scanKeysWithValues(final String keyPattern) {
        if (!StringUtils.hasText(keyPattern)) {
            return new HashMap<>();
        }

        // 首先扫描获取所有匹配的键
        List<String> keys = scanKeys(keyPattern);
        if (keys.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, List<Object>> result = new HashMap<>();
        try {
            // 逐个获取每个 List 的所有元素
            for (String key : keys) {
                List<Object> listElements = redisTemplate.opsForList().range(key, 0, -1);
                result.put(key, listElements);
            }
        } catch (Exception e) {
            log.error("Redis List scan keys with values failed, pattern: {}, error: {}", keyPattern, e.getMessage(), e);
            return new HashMap<>();
        }

        return result;
    }


}
