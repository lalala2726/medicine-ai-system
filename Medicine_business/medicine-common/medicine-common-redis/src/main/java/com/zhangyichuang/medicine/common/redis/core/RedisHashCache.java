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
 * Redis Hash 操作缓存工具类
 * 提供 Hash 数据结构的各种操作方法，包括批量扫描功能
 *
 * @author Chuang
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings(value = {"unchecked", "rawtypes"})
public final class RedisHashCache {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(RedisHashCache.class);

    private final static int scanCount = 1000;
    private final RedisTemplate redisTemplate;

    /**
     * 向 Hash 添加单个字段
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     * @param <HK>  字段类型
     * @param <HV>  值类型
     */
    public <HK, HV> void hPut(final String key, final HK field, final HV value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 向 Hash 批量添加字段
     *
     * @param key      Redis 键
     * @param fieldMap 字段–值 对
     * @param <HK>     字段类型
     * @param <HV>     值类型
     */
    public <HK, HV> void hPutAll(final String key, final Map<HK, HV> fieldMap) {
        redisTemplate.opsForHash().putAll(key, fieldMap);
    }

    /**
     * 从 Hash 中取单个字段
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param <HK>  字段类型
     * @param <HV>  值类型
     * @return 对应字段的值；不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <HK, HV> HV hGet(final String key, final HK field) {
        return (HV) redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 取整个 Hash 的所有字段和值
     *
     * @param key  Redis 键
     * @param <HK> 字段类型
     * @param <HV> 值类型
     * @return 一个 Map，key 对应字段，value 对应值
     */
    @SuppressWarnings("unchecked")
    public <HK, HV> Map<HK, HV> hGetAll(final String key) {
        return (Map<HK, HV>) redisTemplate.opsForHash().entries(key);
    }


    /**
     * 从 Hash 中删除一个或多个字段
     *
     * @param key    Redis 键
     * @param fields 要删除的字段名
     * @param <HK>   字段类型
     * @return 删除的字段数量
     */
    public <HK> Long hRemove(final String key, final HK... fields) {
        return redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    /**
     * 判断 Hash 中是否存在某个字段
     *
     * @param key   Redis 键
     * @param field 字段名
     * @return true=存在；false=不存在
     */
    public Boolean hExists(final String key, final Object field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    /**
     * 获取 Hash 中字段的数量
     *
     * @param key Redis 键
     * @return 字段个数
     */
    public Long hSize(final String key) {
        return redisTemplate.opsForHash().size(key);
    }

    /**
     * 对 Hash 中的数值字段做增量操作
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param delta 增量（负值表示减）
     * @param <HK>  字段类型
     * @return 操作后的新值
     */
    public <HK> Long hIncrement(final String key, final HK field, final long delta) {
        return redisTemplate.opsForHash().increment(key, field, delta);
    }

    /**
     * 给任意 key 设置过期时间（Hash 也能用）
     *
     * @param key      Redis 键
     * @param timeout  时长
     * @param timeUnit 时间单位
     * @return 设置是否成功
     */
    public Boolean expire(final String key, final long timeout, final TimeUnit timeUnit) {
        return redisTemplate.expire(key, timeout, timeUnit);
    }

    /**
     * 查询 key 的剩余存活秒数
     *
     * @param key Redis 键
     * @return 剩余秒数；-2=不存在，-1=未设置过期
     */
    public Long getExpire(final String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 批量键扫描方法 - 使用 Redis SCAN 操作高效获取匹配的 Hash 键
     * 推荐在生产环境中使用此方法，避免阻塞 Redis 服务器
     *
     * @param keyPattern Redis 键模式，支持通配符（如：auth:session:index:*）
     * @return 匹配的键列表，如果没有匹配的键则返回空列表
     */
    public List<String> scanKeys(final String keyPattern) {
        if (!StringUtils.hasText(keyPattern)) {
            log.warn("Redis哈希扫描模式为空，返回空列表");
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
            log.error("Redis Hash scan keys failed, pattern: {}, error: {}", keyPattern, e.getMessage(), e);
            // 发生异常时返回空列表，避免影响业务流程
            return new ArrayList<>();
        }

        return keys;
    }

    /**
     * 批量值获取方法 - 获取匹配模式的 Hash 键及其所有字段值
     * 使用 Redis SCAN 操作高效扫描键，然后批量获取每个 Hash 的所有字段值
     *
     * @param keyPattern Redis 键模式，支持通配符（如：auth:session:index:*）
     * @return 嵌套Map，外层键为Hash键，内层Map为该Hash的所有字段值，如果没有匹配的键则返回空Map
     */
    public Map<String, Map<Object, Object>> scanKeysWithValues(final String keyPattern) {
        if (!StringUtils.hasText(keyPattern)) {
            return new HashMap<>();
        }

        // 首先扫描获取所有匹配的键
        List<String> keys = scanKeys(keyPattern);
        if (keys.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Map<Object, Object>> result = new HashMap<>();
        try {
            // 逐个获取每个 Hash 的所有字段值
            for (String key : keys) {
                Map<Object, Object> hashData = redisTemplate.opsForHash().entries(key);
                result.put(key, hashData);
            }
        } catch (Exception e) {
            log.error("Redis Hash scan keys with values failed, pattern: {}, error: {}", keyPattern, e.getMessage(), e);
            return new HashMap<>();
        }

        return result;
    }


}
