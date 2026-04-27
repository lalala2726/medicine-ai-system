package com.zhangyichuang.medicine.common.redis.core;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 有序集合（ZSet）操作缓存工具类
 * 提供有序集合数据结构的各种操作方法，包括批量扫描功能
 *
 * @author Chuang
 * <p>
 * created on 2025/7/25
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings(value = {"unchecked", "rawtypes"})
public final class RedisZSetCache {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(RedisZSetCache.class);

    private final static int scanCount = 1000;
    public final RedisTemplate redisTemplate;

    /**
     * 添加有序集合成员
     *
     * @param key    Redis 键
     * @param member 成员
     * @param score  分值
     * @param <T>    成员类型
     */
    public <T> void zAdd(final String key, final T member, final double score) {
        redisTemplate.opsForZSet().add(key, member, score);
    }

    /**
     * 添加有序集合成员（设置超时时间）
     *
     * @param key     Redis 键
     * @param member  成员
     * @param score   分值
     * @param <T>     成员类型
     * @param timeout 超时时间 单位秒
     */
    public <T> void zAdd(final String key, final T member, final double score, final long timeout) {
        redisTemplate.opsForZSet().add(key, member, score);
        redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 添加有序集合成员（设置超时时间和单位）
     *
     * @param key      Redis 键
     * @param member   成员
     * @param score    分值
     * @param <T>      成员类型
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     */
    public <T> void zAdd(final String key, final T member, final double score, final long timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForZSet().add(key, member, score);
        redisTemplate.expire(key, timeout, timeUnit);
    }

    /**
     * 设置有序集合有效期(单位秒)
     *
     * @param key     Redis 键
     * @param timeout 有效期
     */
    public void expire(final String key, final long timeout) {
        redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 获取有序集合有效期
     *
     * @param key Redis 键
     */
    public void expire(final String key, final long timeout, final TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }


    /**
     * 批量删除有序集合中的一个或多个成员
     *
     * @param key     Redis 键
     * @param members 要删除的成员数组
     * @param <T>     成员类型
     * @return 删除的成员个数
     */
    public <T> Long zRemove(final String key, final T... members) {
        return redisTemplate.opsForZSet().remove(key, members);
    }

    /**
     * 按排名（索引）范围获取有序集合成员
     *
     * @param key   Redis 键
     * @param start 开始索引（从 0 开始）
     * @param end   结束索引（-1 表示最后一个）
     * @param <T>   成员类型
     * @return 成员集合（按分值从小到大排序）
     */
    public <T> Set<T> zRange(final String key, final long start, final long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 按分值范围获取有序集合成员
     *
     * @param key      Redis 键
     * @param minScore 最小分值
     * @param maxScore 最大分值
     * @param <T>      成员类型
     * @return 成员集合（分值在 [minScore, maxScore] 区间内）
     */
    public <T> Set<T> zRangeByScore(final String key, final double minScore, final double maxScore) {
        return redisTemplate.opsForZSet().rangeByScore(key, minScore, maxScore);
    }

    /**
     * 统计有序集合中分值在给定区间的成员数量
     *
     * @param key      Redis 键
     * @param minScore 最小分值
     * @param maxScore 最大分值
     * @return 成员数量
     */
    public Long zCount(final String key, final double minScore, final double maxScore) {
        return redisTemplate.opsForZSet().count(key, minScore, maxScore);
    }

    /**
     * 获取有序集合的总成员数
     *
     * @param key Redis 键
     * @return 成员数量
     */
    public Long zCard(final String key) {
        return redisTemplate.opsForZSet().zCard(key);
    }


    /**
     * 获取有序集合的所有成员（带分值）
     *
     * @param key Redis 键
     * @return 值集合（带分值）
     */
    public Set<ZSetOperations.TypedTuple<String>> getAllWithScore(String key) {
        return redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
    }


    /**
     * 获取某个成员的分值
     *
     * @param key    Redis 键
     * @param member 成员
     * @param <T>    成员类型
     * @return 分值；null 表示成员不存在
     */
    public <T> Double zScore(final String key, final T member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    /**
     * 为有序集合中的某个成员分值加上给定增量
     *
     * @param key    Redis 键
     * @param member 成员
     * @param delta  增量（可以为负数，用于减分）
     * @param <T>    成员类型
     * @return 操作后的新分值
     */
    public <T> Double zIncrementScore(final String key, final T member, final double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, member, delta);
    }


    /**
     * 批量扫描ZSet键 - 使用Redis SCAN操作高效获取匹配的有序集合键
     * 推荐在生产环境中使用此方法，避免阻塞Redis服务器
     *
     * @param keyPattern Redis键模式，支持通配符（如：ranking:*）
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
                    // 每次扫描的数量，可根据实际情况调整
                    .count(scanCount)
                    .build();

            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }

            log.debug("Redis ZSet scan completed, pattern: {}, found {} keys", keyPattern, keys.size());
        } catch (Exception e) {
            log.error("Redis ZSet scan keys failed, pattern: {}, error: {}", keyPattern, e.getMessage(), e);
            // 发生异常时返回空列表，避免影响业务流程
            return new ArrayList<>();
        }

        return keys;
    }

    /**
     * 批量扫描ZSet键和成员 - 获取匹配模式的有序集合键及其所有成员（带分值）
     * 使用Redis SCAN操作高效扫描键，然后批量获取每个ZSet的所有成员和分值
     *
     * @param keyPattern Redis键模式，支持通配符（如：ranking:*）
     * @return Map，键为ZSet键名，值为该ZSet的所有成员（带分值），如果没有匹配的键则返回空Map
     */
    public Map<String, Set<ZSetOperations.TypedTuple<Object>>> scanKeysWithValues(final String keyPattern) {
        if (!StringUtils.hasText(keyPattern)) {
            return new HashMap<>();
        }

        // 首先扫描获取所有匹配的键
        List<String> keys = scanKeys(keyPattern);
        if (keys.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Set<ZSetOperations.TypedTuple<Object>>> result = new HashMap<>();
        try {
            // 逐个获取每个ZSet的所有成员（带分值）
            for (String key : keys) {
                Set<ZSetOperations.TypedTuple<Object>> zsetMembers =
                        redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
                result.put(key, zsetMembers);
            }
        } catch (Exception e) {
            log.error("Redis ZSet scan keys with values failed, pattern: {}, error: {}", keyPattern, e.getMessage(), e);
            return new HashMap<>();
        }

        return result;
    }


    /**
     * 使用ZSCAN扫描单个有序集合的成员（带分值）
     * 注意：这是惰性、增量式扫描；本方法会将结果聚合到内存中返回。
     * 如需极致大集合的流式处理，建议提供回调式消费。
     *
     * @param key ZSet键名
     * @return 成员（带分值）集合
     */
    public Set<ZSetOperations.TypedTuple<Object>> zScanWithScores(final String key) {
        if (!StringUtils.hasText(key)) {
            return Collections.emptySet();
        }

        Set<ZSetOperations.TypedTuple<Object>> result = new LinkedHashSet<>();
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .count(scanCount)
                    .build();
            try (Cursor<ZSetOperations.TypedTuple<Object>> cursor = redisTemplate.opsForZSet().scan(key, options)) {
                while (cursor.hasNext()) {
                    result.add(cursor.next());
                }
            }
        } catch (Exception e) {
            log.error("Redis ZSet scan members failed, key: {}, error: {}", key, e.getMessage(), e);
            return Collections.emptySet();
        }
        return result;
    }


}
