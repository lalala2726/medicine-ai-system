package com.zhangyichuang.medicine.common.redis.core;


import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存操作工具类
 *
 * @author Chuang
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings(value = {"unchecked", "rawtypes"})
public final class RedisCache {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(RedisCache.class);

    private final static int scanCount = 1000;
    public final RedisTemplate redisTemplate;

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final long timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等，默认时间单位为秒
     *
     * @param key     key
     * @param value   value
     * @param timeout 超时时间（秒）
     */
    public <T> void setCacheObject(final String key, final T value, final long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }


    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间（秒）
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key      Redis键
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit timeUnit) {
        return redisTemplate.expire(key, timeout, timeUnit);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key) {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * 对指定 key 执行自增 1。
     *
     * @param key Redis key
     * @return 自增后的值
     */
    public Long increment(final String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 对指定 key 执行自增 delta。
     *
     * @param key   Redis key
     * @param delta 增量
     * @return 自增后的值
     */
    public Long increment(final String key, final long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }


    /**
     * 删除单个对象
     *
     * @param key 缓存键值
     */
    public void deleteObject(final String key) {
        redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     *
     * @param keyCollection 多个键的集合
     * @return 删除成功的个数
     */
    public long deleteObject(final Collection keyCollection) {
        return redisTemplate.delete(keyCollection);
    }


    /**
     * 获得缓存的基本对象列表
     * 注意：此方法使用 KEYS 命令，在生产环境中应谨慎使用，建议使用 scanKeys 方法
     *
     * @param keyPattern 键模式，支持通配符
     * @return 对象列表
     */
    public Collection<String> keys(final String keyPattern) {
        return redisTemplate.keys(keyPattern);
    }

    /**
     * 批量键扫描方法 - 使用 Redis SCAN 操作高效获取匹配的键
     * 推荐在生产环境中使用此方法替代 keys() 方法，避免阻塞 Redis 服务器
     *
     * @param keyPattern Redis 键模式，支持通配符
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
            log.error("Redis scan keys failed, pattern: {}, error: {}", keyPattern, e.getMessage(), e);
            return new ArrayList<>();
        }

        return keys;
    }

    /**
     * 批量值获取方法 - 获取匹配模式的键及其对应的值
     * 使用 Redis SCAN 操作高效扫描键，然后批量获取对应的值
     *
     * @param keyPattern Redis 键模式，支持通配符（如：auth:session:index:*）
     * @return 键值对Map，键为Redis键，值为对应的Redis值，如果没有匹配的键则返回空Map
     */
    public Map<String, Object> scanKeysWithValues(final String keyPattern) {
        if (!StringUtils.hasText(keyPattern)) {
            return new HashMap<>();
        }
        // 首先扫描获取所有匹配的键
        List<String> keys = scanKeys(keyPattern);
        if (keys.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> result = new HashMap<>();
        try {
            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                Object value = (values != null && i < values.size()) ? values.get(i) : null;
                result.put(key, value);
            }
        } catch (Exception e) {
            log.error("Redis scan keys with values failed, pattern: {}, error: {}", keyPattern, e.getMessage(), e);
            return new HashMap<>();
        }

        return result;
    }


    /**
     * 判断缓存中是否有对应的value
     *
     * @param key 键
     * @return true=存在；false=不存在
     */
    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 判断给定键是否存在于Redis中
     *
     * @param key key
     * @return true=存在；false=不存在
     */
    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 获取指定key的过期时间
     *
     * @param key key
     * @return 过期时间（秒），返回-1表示永不过期，返回-2表示键不存在
     */
    public Long getKeyExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 获取指定key的过期时间
     *
     * @param key      key
     * @param timeUnit 时间单位
     * @return 过期时间，返回-1示永不过期，返回-2表示键不存在
     */
    public Long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }

    /**
     * 获取指定key的过期时间（默认秒）
     *
     * @param key key
     * @return 过期时间（秒），返回-1表示永不过期，返回-2表示键不存在
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }


}
