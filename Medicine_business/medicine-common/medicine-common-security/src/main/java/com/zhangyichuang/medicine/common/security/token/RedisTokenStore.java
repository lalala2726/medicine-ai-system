package com.zhangyichuang.medicine.common.security.token;

import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.AuthorizationException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import com.zhangyichuang.medicine.common.redis.core.RedisSetCache;
import com.zhangyichuang.medicine.common.security.config.SecurityProperties;
import com.zhangyichuang.medicine.common.security.entity.OnlineLoginUser;
import com.zhangyichuang.medicine.common.security.entity.RefreshTokenSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Redis Token 会话存储。
 * <p>
 * 统一维护访问令牌、刷新令牌以及用户维度的令牌索引，保证权限变更时可以按用户清理在线会话。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class RedisTokenStore {

    private final RedisCache redisCache;
    private final RedisSetCache redisSetCache;
    private final SecurityProperties securityProperties;

    /**
     * 保存登录会话。
     *
     * @param accessTokenId   访问令牌会话ID
     * @param refreshTokenId  刷新令牌会话ID
     * @param onlineLoginUser 在线用户信息
     */
    public void saveLoginSession(String accessTokenId, String refreshTokenId, OnlineLoginUser onlineLoginUser) {
        validateSessionInput(accessTokenId, refreshTokenId, onlineLoginUser);

        redisCache.setCacheObject(
                buildAccessTokenKey(accessTokenId),
                onlineLoginUser,
                securityProperties.getAccessTokenExpireTime()
        );

        long now = System.currentTimeMillis();
        RefreshTokenSession refreshTokenSession = RefreshTokenSession.builder()
                .refreshTokenId(refreshTokenId)
                .accessTokenId(accessTokenId)
                .userId(onlineLoginUser.getUserId())
                .username(onlineLoginUser.getUsername())
                .createTime(now)
                .updateTime(now)
                .build();
        redisCache.setCacheObject(
                buildRefreshTokenKey(refreshTokenId),
                refreshTokenSession,
                securityProperties.getRefreshTokenExpireTime()
        );

        addUserTokenIndexes(
                onlineLoginUser.getUserId(),
                accessTokenId,
                refreshTokenId,
                securityProperties.getRefreshTokenExpireTime()
        );
    }

    /**
     * 使用刷新令牌替换当前访问令牌。
     *
     * @param refreshTokenId  刷新令牌会话ID
     * @param newAccessTokenId 新访问令牌会话ID
     * @param onlineLoginUser 新访问令牌对应的在线用户信息
     */
    public void replaceAccessToken(String refreshTokenId, String newAccessTokenId, OnlineLoginUser onlineLoginUser) {
        Assert.hasText(refreshTokenId, "刷新令牌ID不能为空");
        Assert.hasText(newAccessTokenId, "访问令牌ID不能为空");
        Assert.notNull(onlineLoginUser, "在线用户信息不能为空");
        Assert.isPositive(onlineLoginUser.getUserId(), "用户ID必须大于0");

        String refreshTokenKey = buildRefreshTokenKey(refreshTokenId);
        RefreshTokenSession refreshTokenSession = getRefreshTokenSession(refreshTokenId);
        if (refreshTokenSession == null) {
            throw new AuthorizationException(ResponseCode.REFRESH_TOKEN_INVALID);
        }
        long refreshTtlSeconds = getValidRefreshTokenTtl(refreshTokenKey);
        Long userId = refreshTokenSession.getUserId();
        Assert.isPositive(userId, "刷新令牌用户ID必须大于0");

        String oldAccessTokenId = refreshTokenSession.getAccessTokenId();
        if (StringUtils.hasText(oldAccessTokenId)) {
            redisCache.deleteObject(buildAccessTokenKey(oldAccessTokenId));
            removeUserAccessTokenIndex(userId, oldAccessTokenId);
        }

        redisCache.setCacheObject(
                buildAccessTokenKey(newAccessTokenId),
                onlineLoginUser,
                securityProperties.getAccessTokenExpireTime()
        );

        refreshTokenSession.setAccessTokenId(newAccessTokenId);
        refreshTokenSession.setUserId(onlineLoginUser.getUserId());
        refreshTokenSession.setUsername(onlineLoginUser.getUsername());
        refreshTokenSession.setUpdateTime(System.currentTimeMillis());
        redisCache.setCacheObject(refreshTokenKey, refreshTokenSession, refreshTtlSeconds);

        addUserAccessTokenIndex(refreshTokenSession.getUserId(), newAccessTokenId, refreshTtlSeconds);
        addUserRefreshTokenIndex(refreshTokenSession.getUserId(), refreshTokenId, refreshTtlSeconds);
    }

    /**
     * 读取访问令牌会话。
     *
     * @param accessTokenId 访问令牌会话ID
     * @return 在线用户信息；不存在时返回 null
     */
    public OnlineLoginUser getAccessToken(String accessTokenId) {
        String accessTokenRedisKey = buildAccessTokenKey(accessTokenId);
        return redisCache.getCacheObject(accessTokenRedisKey);
    }

    /**
     * 判断访问令牌是否有效。
     *
     * @param accessTokenId 访问令牌会话ID
     * @return true 表示访问令牌存在
     */
    public boolean isValidAccessToken(String accessTokenId) {
        return redisCache.exists(buildAccessTokenKey(accessTokenId));
    }

    /**
     * 更新访问令牌访问时间。
     *
     * @param accessTokenId 访问令牌会话ID
     * @return true 表示更新成功
     */
    public boolean updateAccessTime(String accessTokenId) {
        String accessTokenKey = buildAccessTokenKey(accessTokenId);
        OnlineLoginUser onlineLoginUser = redisCache.getCacheObject(accessTokenKey);
        if (onlineLoginUser == null) {
            return false;
        }

        long expire = redisCache.getExpire(accessTokenKey);
        if (expire <= 0) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (onlineLoginUser.getCreateTime() == null) {
            onlineLoginUser.setCreateTime(now);
        }
        onlineLoginUser.setAccessTime(now);
        onlineLoginUser.setUpdateTime(now);
        redisCache.setCacheObject(accessTokenKey, onlineLoginUser, expire);
        return true;
    }

    /**
     * 判断刷新令牌是否有效。
     *
     * @param refreshTokenId 刷新令牌会话ID
     * @return true 表示刷新令牌存在
     */
    public boolean isValidRefreshToken(String refreshTokenId) {
        return redisCache.exists(buildRefreshTokenKey(refreshTokenId));
    }

    /**
     * 获取刷新令牌ID。
     *
     * @param accessTokenId 访问令牌会话ID
     * @return 刷新令牌会话ID
     */
    public String getRefreshTokenIdByAccessTokenId(String accessTokenId) {
        Assert.hasText(accessTokenId, "访问令牌ID不能为空");
        OnlineLoginUser onlineLoginUser = getAccessToken(accessTokenId);
        Assert.notNull(onlineLoginUser, "访问令牌不存在");
        return onlineLoginUser.getRefreshTokenId();
    }

    /**
     * 通过访问令牌删除完整登录会话。
     *
     * @param accessTokenId 访问令牌会话ID
     */
    public void deleteSessionByAccessId(String accessTokenId) {
        Assert.hasText(accessTokenId, "访问令牌ID不能为空");
        OnlineLoginUser onlineLoginUser = getAccessToken(accessTokenId);
        if (onlineLoginUser == null) {
            redisCache.deleteObject(buildAccessTokenKey(accessTokenId));
            return;
        }
        deleteSession(onlineLoginUser.getUserId(), accessTokenId, onlineLoginUser.getRefreshTokenId());
    }

    /**
     * 通过刷新令牌删除完整登录会话。
     *
     * @param refreshTokenId 刷新令牌会话ID
     */
    public void deleteSessionByRefreshId(String refreshTokenId) {
        Assert.hasText(refreshTokenId, "刷新令牌ID不能为空");
        RefreshTokenSession refreshTokenSession = getRefreshTokenSession(refreshTokenId);
        if (refreshTokenSession == null) {
            redisCache.deleteObject(buildRefreshTokenKey(refreshTokenId));
            return;
        }
        deleteSession(refreshTokenSession.getUserId(), refreshTokenSession.getAccessTokenId(), refreshTokenId);
    }

    /**
     * 按用户批量删除所有登录会话。
     *
     * @param userIds 用户ID集合
     */
    public void deleteSessionsByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        userIds.stream()
                .filter(Objects::nonNull)
                .filter(userId -> userId > 0)
                .distinct()
                .forEach(this::deleteSessionsByUserId);
    }

    /**
     * 读取刷新令牌会话。
     *
     * @param refreshTokenId 刷新令牌会话ID
     * @return 刷新令牌会话；不存在时返回 null
     */
    private RefreshTokenSession getRefreshTokenSession(String refreshTokenId) {
        return redisCache.getCacheObject(buildRefreshTokenKey(refreshTokenId));
    }

    /**
     * 按用户删除全部登录会话。
     *
     * @param userId 用户ID
     */
    private void deleteSessionsByUserId(Long userId) {
        String accessTokenIndexKey = buildUserAccessTokenIndexKey(userId);
        String refreshTokenIndexKey = buildUserRefreshTokenIndexKey(userId);
        Set<String> accessTokenIds = getIndexMembers(accessTokenIndexKey);
        Set<String> refreshTokenIds = getIndexMembers(refreshTokenIndexKey);

        refreshTokenIds.forEach(this::deleteSessionByRefreshId);
        accessTokenIds.forEach(this::deleteSessionByAccessId);

        redisCache.deleteObject(accessTokenIndexKey);
        redisCache.deleteObject(refreshTokenIndexKey);
    }

    /**
     * 删除单个完整登录会话。
     *
     * @param userId         用户ID
     * @param accessTokenId  访问令牌会话ID
     * @param refreshTokenId 刷新令牌会话ID
     */
    private void deleteSession(Long userId, String accessTokenId, String refreshTokenId) {
        if (StringUtils.hasText(accessTokenId)) {
            redisCache.deleteObject(buildAccessTokenKey(accessTokenId));
        }
        if (StringUtils.hasText(refreshTokenId)) {
            redisCache.deleteObject(buildRefreshTokenKey(refreshTokenId));
        }
        if (userId != null && userId > 0) {
            if (StringUtils.hasText(accessTokenId)) {
                removeUserAccessTokenIndex(userId, accessTokenId);
            }
            if (StringUtils.hasText(refreshTokenId)) {
                removeUserRefreshTokenIndex(userId, refreshTokenId);
            }
            deleteUserTokenIndexIfEmpty(userId);
        }
    }

    /**
     * 校验登录会话保存参数。
     *
     * @param accessTokenId   访问令牌会话ID
     * @param refreshTokenId  刷新令牌会话ID
     * @param onlineLoginUser 在线用户信息
     */
    private void validateSessionInput(String accessTokenId, String refreshTokenId, OnlineLoginUser onlineLoginUser) {
        Assert.hasText(accessTokenId, "访问令牌ID不能为空");
        Assert.hasText(refreshTokenId, "刷新令牌ID不能为空");
        Assert.notNull(onlineLoginUser, "在线用户信息不能为空");
        Assert.isPositive(onlineLoginUser.getUserId(), "用户ID必须大于0");
    }

    /**
     * 获取有效刷新令牌剩余时间。
     *
     * @param refreshTokenKey 刷新令牌 Redis Key
     * @return 剩余有效期，单位秒
     */
    private long getValidRefreshTokenTtl(String refreshTokenKey) {
        long ttlSeconds = redisCache.getKeyExpire(refreshTokenKey);
        if (ttlSeconds <= 0) {
            throw new AuthorizationException(ResponseCode.REFRESH_TOKEN_INVALID);
        }
        return ttlSeconds;
    }

    /**
     * 写入用户令牌索引。
     *
     * @param userId         用户ID
     * @param accessTokenId  访问令牌会话ID
     * @param refreshTokenId 刷新令牌会话ID
     * @param ttlSeconds     索引有效期，单位秒
     */
    private void addUserTokenIndexes(Long userId, String accessTokenId, String refreshTokenId, long ttlSeconds) {
        addUserAccessTokenIndex(userId, accessTokenId, ttlSeconds);
        addUserRefreshTokenIndex(userId, refreshTokenId, ttlSeconds);
    }

    /**
     * 写入用户访问令牌索引。
     *
     * @param userId        用户ID
     * @param accessTokenId 访问令牌会话ID
     * @param ttlSeconds    索引有效期，单位秒
     */
    private void addUserAccessTokenIndex(Long userId, String accessTokenId, long ttlSeconds) {
        String accessTokenIndexKey = buildUserAccessTokenIndexKey(userId);
        redisSetCache.add(accessTokenIndexKey, accessTokenId);
        extendIndexTtlIfLonger(accessTokenIndexKey, ttlSeconds);
    }

    /**
     * 写入用户刷新令牌索引。
     *
     * @param userId         用户ID
     * @param refreshTokenId 刷新令牌会话ID
     * @param ttlSeconds     索引有效期，单位秒
     */
    private void addUserRefreshTokenIndex(Long userId, String refreshTokenId, long ttlSeconds) {
        String refreshTokenIndexKey = buildUserRefreshTokenIndexKey(userId);
        redisSetCache.add(refreshTokenIndexKey, refreshTokenId);
        extendIndexTtlIfLonger(refreshTokenIndexKey, ttlSeconds);
    }

    /**
     * 移除用户访问令牌索引。
     *
     * @param userId        用户ID
     * @param accessTokenId 访问令牌会话ID
     */
    private void removeUserAccessTokenIndex(Long userId, String accessTokenId) {
        redisSetCache.remove(buildUserAccessTokenIndexKey(userId), accessTokenId);
    }

    /**
     * 移除用户刷新令牌索引。
     *
     * @param userId         用户ID
     * @param refreshTokenId 刷新令牌会话ID
     */
    private void removeUserRefreshTokenIndex(Long userId, String refreshTokenId) {
        redisSetCache.remove(buildUserRefreshTokenIndexKey(userId), refreshTokenId);
    }

    /**
     * 仅当新 TTL 更长时延长索引有效期。
     *
     * @param key        Redis Key
     * @param ttlSeconds 新有效期，单位秒
     */
    private void extendIndexTtlIfLonger(String key, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        Long currentTtl = redisCache.getExpire(key);
        if (currentTtl == null || currentTtl < 0 || currentTtl < ttlSeconds) {
            redisCache.expire(key, ttlSeconds);
        }
    }

    /**
     * 索引为空时删除索引 Key。
     *
     * @param userId 用户ID
     */
    private void deleteUserTokenIndexIfEmpty(Long userId) {
        deleteIndexIfEmpty(buildUserAccessTokenIndexKey(userId));
        deleteIndexIfEmpty(buildUserRefreshTokenIndexKey(userId));
    }

    /**
     * 索引为空时删除指定 Key。
     *
     * @param indexKey 索引 Key
     */
    private void deleteIndexIfEmpty(String indexKey) {
        Long size = redisSetCache.size(indexKey);
        if (size == null || size <= 0) {
            redisCache.deleteObject(indexKey);
        }
    }

    /**
     * 读取索引成员。
     *
     * @param indexKey 索引 Key
     * @return 索引成员集合
     */
    private Set<String> getIndexMembers(String indexKey) {
        Set<String> members = redisSetCache.members(indexKey);
        if (members == null || members.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(members);
    }

    /**
     * 构建访问令牌 Key。
     *
     * @param accessTokenId 访问令牌会话ID
     * @return 访问令牌 Redis Key
     */
    private String buildAccessTokenKey(String accessTokenId) {
        return RedisConstants.Auth.USER_ACCESS_TOKEN + accessTokenId;
    }

    /**
     * 构建刷新令牌 Key。
     *
     * @param refreshTokenId 刷新令牌会话ID
     * @return 刷新令牌 Redis Key
     */
    private String buildRefreshTokenKey(String refreshTokenId) {
        return RedisConstants.Auth.USER_REFRESH_TOKEN + refreshTokenId;
    }

    /**
     * 构建用户访问令牌索引 Key。
     *
     * @param userId 用户ID
     * @return 用户访问令牌索引 Key
     */
    private String buildUserAccessTokenIndexKey(Long userId) {
        return String.format(RedisConstants.Auth.USER_ACCESS_TOKEN_INDEX, userId);
    }

    /**
     * 构建用户刷新令牌索引 Key。
     *
     * @param userId 用户ID
     * @return 用户刷新令牌索引 Key
     */
    private String buildUserRefreshTokenIndexKey(Long userId) {
        return String.format(RedisConstants.Auth.USER_REFRESH_TOKEN_INDEX, userId);
    }
}
