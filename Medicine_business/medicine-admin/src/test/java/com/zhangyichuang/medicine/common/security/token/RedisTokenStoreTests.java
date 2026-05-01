package com.zhangyichuang.medicine.common.security.token;

import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import com.zhangyichuang.medicine.common.redis.core.RedisSetCache;
import com.zhangyichuang.medicine.common.security.config.SecurityProperties;
import com.zhangyichuang.medicine.common.security.entity.OnlineLoginUser;
import com.zhangyichuang.medicine.common.security.entity.RefreshTokenSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTokenStoreTests {

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    @SuppressWarnings("rawtypes")
    private ValueOperations valueOperations;

    @Mock
    @SuppressWarnings("rawtypes")
    private SetOperations setOperations;

    @Mock
    private SecurityProperties securityProperties;

    @Test
    void updateAccessTime_ShouldUpdateAccessAndUpdateTime() {
        RedisTokenStore redisTokenStore = createRedisTokenStore();
        String accessTokenId = "access-1";
        String accessTokenKey = RedisConstants.Auth.USER_ACCESS_TOKEN + accessTokenId;
        Long userId = 10L;
        String accessIndexKey = String.format(RedisConstants.Auth.USER_ACCESS_TOKEN_INDEX, userId);
        String refreshIndexKey = String.format(RedisConstants.Auth.USER_REFRESH_TOKEN_INDEX, userId);

        OnlineLoginUser onlineLoginUser = OnlineLoginUser.builder()
                .accessTokenId(accessTokenId)
                .userId(userId)
                .createTime(123456L)
                .build();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(accessTokenKey)).thenReturn(onlineLoginUser);
        when(redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS)).thenReturn(600L);

        boolean updated = redisTokenStore.updateAccessTime(accessTokenId);

        assertTrue(updated);
        assertEquals(123456L, onlineLoginUser.getCreateTime());
        assertNotNull(onlineLoginUser.getAccessTime());
        assertNotNull(onlineLoginUser.getUpdateTime());
        verify(valueOperations).set(eq(accessTokenKey), eq(onlineLoginUser), eq(600L), eq(TimeUnit.SECONDS));
        verify(redisTemplate, never()).expire(eq(accessIndexKey), anyLong(), eq(TimeUnit.SECONDS));
        verify(redisTemplate, never()).expire(eq(refreshIndexKey), anyLong(), eq(TimeUnit.SECONDS));
        verify(redisTemplate, never()).opsForSet();
    }

    @Test
    void updateAccessTime_ShouldBackfillCreateTimeForLegacySession() {
        RedisTokenStore redisTokenStore = createRedisTokenStore();
        String accessTokenId = "access-legacy";
        String accessTokenKey = RedisConstants.Auth.USER_ACCESS_TOKEN + accessTokenId;
        Long userId = 10L;

        OnlineLoginUser onlineLoginUser = OnlineLoginUser.builder()
                .accessTokenId(accessTokenId)
                .userId(userId)
                .build();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(accessTokenKey)).thenReturn(onlineLoginUser);
        when(redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS)).thenReturn(600L);

        boolean updated = redisTokenStore.updateAccessTime(accessTokenId);

        assertTrue(updated);
        assertNotNull(onlineLoginUser.getCreateTime());
        assertNotNull(onlineLoginUser.getAccessTime());
        assertNotNull(onlineLoginUser.getUpdateTime());
        assertEquals(onlineLoginUser.getCreateTime(), onlineLoginUser.getUpdateTime());
        assertEquals(onlineLoginUser.getCreateTime(), onlineLoginUser.getAccessTime());
        verify(valueOperations).set(eq(accessTokenKey), eq(onlineLoginUser), eq(600L), eq(TimeUnit.SECONDS));
        verify(redisTemplate, never()).opsForSet();
    }

    @Test
    void saveLoginSession_ShouldWriteAccessRefreshAndUserIndexes() {
        RedisTokenStore redisTokenStore = createRedisTokenStore();
        String accessTokenId = "access-1";
        String refreshTokenId = "refresh-1";
        Long userId = 10L;
        String accessTokenKey = RedisConstants.Auth.USER_ACCESS_TOKEN + accessTokenId;
        String refreshTokenKey = RedisConstants.Auth.USER_REFRESH_TOKEN + refreshTokenId;
        String accessIndexKey = String.format(RedisConstants.Auth.USER_ACCESS_TOKEN_INDEX, userId);
        String refreshIndexKey = String.format(RedisConstants.Auth.USER_REFRESH_TOKEN_INDEX, userId);
        assertEquals("auth:token:user:access:" + userId, accessIndexKey);
        assertEquals("auth:token:user:refresh:" + userId, refreshIndexKey);
        OnlineLoginUser onlineLoginUser = OnlineLoginUser.builder()
                .accessTokenId(accessTokenId)
                .refreshTokenId(refreshTokenId)
                .userId(userId)
                .username("alice")
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(securityProperties.getAccessTokenExpireTime()).thenReturn(1800L);
        when(securityProperties.getRefreshTokenExpireTime()).thenReturn(259200L);

        redisTokenStore.saveLoginSession(accessTokenId, refreshTokenId, onlineLoginUser);

        verify(valueOperations).set(eq(accessTokenKey), eq(onlineLoginUser), eq(1800L), eq(TimeUnit.SECONDS));
        ArgumentCaptor<RefreshTokenSession> refreshSessionCaptor = ArgumentCaptor.forClass(RefreshTokenSession.class);
        verify(valueOperations).set(eq(refreshTokenKey), refreshSessionCaptor.capture(), eq(259200L), eq(TimeUnit.SECONDS));
        RefreshTokenSession refreshTokenSession = refreshSessionCaptor.getValue();
        assertEquals(refreshTokenId, refreshTokenSession.getRefreshTokenId());
        assertEquals(accessTokenId, refreshTokenSession.getAccessTokenId());
        assertEquals(userId, refreshTokenSession.getUserId());
        assertEquals("alice", refreshTokenSession.getUsername());
        assertNotNull(refreshTokenSession.getCreateTime());
        assertNotNull(refreshTokenSession.getUpdateTime());
        verify(setOperations).add(accessIndexKey, accessTokenId);
        verify(setOperations).add(refreshIndexKey, refreshTokenId);
        verify(redisTemplate).expire(accessIndexKey, 259200L, TimeUnit.SECONDS);
        verify(redisTemplate).expire(refreshIndexKey, 259200L, TimeUnit.SECONDS);
    }

    @Test
    void replaceAccessToken_ShouldDeleteOldAccessAndRefreshIndexes() {
        RedisTokenStore redisTokenStore = createRedisTokenStore();
        String oldAccessTokenId = "access-old";
        String newAccessTokenId = "access-new";
        String refreshTokenId = "refresh-1";
        Long userId = 10L;
        String oldAccessTokenKey = RedisConstants.Auth.USER_ACCESS_TOKEN + oldAccessTokenId;
        String newAccessTokenKey = RedisConstants.Auth.USER_ACCESS_TOKEN + newAccessTokenId;
        String refreshTokenKey = RedisConstants.Auth.USER_REFRESH_TOKEN + refreshTokenId;
        String accessIndexKey = String.format(RedisConstants.Auth.USER_ACCESS_TOKEN_INDEX, userId);
        String refreshIndexKey = String.format(RedisConstants.Auth.USER_REFRESH_TOKEN_INDEX, userId);
        RefreshTokenSession refreshTokenSession = RefreshTokenSession.builder()
                .refreshTokenId(refreshTokenId)
                .accessTokenId(oldAccessTokenId)
                .userId(userId)
                .username("alice")
                .createTime(100L)
                .updateTime(100L)
                .build();
        OnlineLoginUser onlineLoginUser = OnlineLoginUser.builder()
                .accessTokenId(newAccessTokenId)
                .refreshTokenId(refreshTokenId)
                .userId(userId)
                .username("alice")
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.get(refreshTokenKey)).thenReturn(refreshTokenSession);
        when(redisTemplate.getExpire(refreshTokenKey)).thenReturn(600L);
        when(securityProperties.getAccessTokenExpireTime()).thenReturn(1800L);

        redisTokenStore.replaceAccessToken(refreshTokenId, newAccessTokenId, onlineLoginUser);

        verify(redisTemplate).delete(oldAccessTokenKey);
        verify(setOperations).remove(accessIndexKey, oldAccessTokenId);
        verify(valueOperations).set(eq(newAccessTokenKey), eq(onlineLoginUser), eq(1800L), eq(TimeUnit.SECONDS));
        ArgumentCaptor<RefreshTokenSession> refreshSessionCaptor = ArgumentCaptor.forClass(RefreshTokenSession.class);
        verify(valueOperations).set(eq(refreshTokenKey), refreshSessionCaptor.capture(), eq(600L), eq(TimeUnit.SECONDS));
        RefreshTokenSession updatedRefreshSession = refreshSessionCaptor.getValue();
        assertEquals(newAccessTokenId, updatedRefreshSession.getAccessTokenId());
        assertEquals(userId, updatedRefreshSession.getUserId());
        assertNotNull(updatedRefreshSession.getUpdateTime());
        verify(setOperations).add(accessIndexKey, newAccessTokenId);
        verify(setOperations).add(refreshIndexKey, refreshTokenId);
        verify(redisTemplate).expire(accessIndexKey, 600L, TimeUnit.SECONDS);
        verify(redisTemplate).expire(refreshIndexKey, 600L, TimeUnit.SECONDS);
    }

    /**
     * 验证通过访问令牌删除会话时会同步删除刷新令牌和用户索引。
     */
    @Test
    void deleteSessionByAccessId_ShouldDeleteAccessRefreshAndUserIndexes() {
        RedisTokenStore redisTokenStore = createRedisTokenStore();
        String accessTokenId = "access-1";
        String refreshTokenId = "refresh-1";
        Long userId = 10L;
        String accessTokenKey = RedisConstants.Auth.USER_ACCESS_TOKEN + accessTokenId;
        String refreshTokenKey = RedisConstants.Auth.USER_REFRESH_TOKEN + refreshTokenId;
        String accessIndexKey = String.format(RedisConstants.Auth.USER_ACCESS_TOKEN_INDEX, userId);
        String refreshIndexKey = String.format(RedisConstants.Auth.USER_REFRESH_TOKEN_INDEX, userId);
        OnlineLoginUser onlineLoginUser = OnlineLoginUser.builder()
                .accessTokenId(accessTokenId)
                .refreshTokenId(refreshTokenId)
                .userId(userId)
                .username("alice")
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.get(accessTokenKey)).thenReturn(onlineLoginUser);

        redisTokenStore.deleteSessionByAccessId(accessTokenId);

        verify(redisTemplate).delete(accessTokenKey);
        verify(redisTemplate).delete(refreshTokenKey);
        verify(setOperations).remove(accessIndexKey, accessTokenId);
        verify(setOperations).remove(refreshIndexKey, refreshTokenId);
        verify(redisTemplate).delete(accessIndexKey);
        verify(redisTemplate).delete(refreshIndexKey);
    }

    @Test
    void deleteSessionByRefreshId_ShouldDeleteAccessRefreshAndUserIndexes() {
        RedisTokenStore redisTokenStore = createRedisTokenStore();
        String accessTokenId = "access-1";
        String refreshTokenId = "refresh-1";
        Long userId = 10L;
        String accessTokenKey = RedisConstants.Auth.USER_ACCESS_TOKEN + accessTokenId;
        String refreshTokenKey = RedisConstants.Auth.USER_REFRESH_TOKEN + refreshTokenId;
        String accessIndexKey = String.format(RedisConstants.Auth.USER_ACCESS_TOKEN_INDEX, userId);
        String refreshIndexKey = String.format(RedisConstants.Auth.USER_REFRESH_TOKEN_INDEX, userId);
        RefreshTokenSession refreshTokenSession = RefreshTokenSession.builder()
                .refreshTokenId(refreshTokenId)
                .accessTokenId(accessTokenId)
                .userId(userId)
                .username("alice")
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.get(refreshTokenKey)).thenReturn(refreshTokenSession);

        redisTokenStore.deleteSessionByRefreshId(refreshTokenId);

        verify(redisTemplate).delete(accessTokenKey);
        verify(redisTemplate).delete(refreshTokenKey);
        verify(setOperations).remove(accessIndexKey, accessTokenId);
        verify(setOperations).remove(refreshIndexKey, refreshTokenId);
        verify(redisTemplate).delete(accessIndexKey);
        verify(redisTemplate).delete(refreshIndexKey);
    }

    @Test
    void deleteSessionsByUserIds_ShouldDeleteRefreshAndOrphanAccessSessions() {
        RedisTokenStore redisTokenStore = createRedisTokenStore();
        Long userId = 10L;
        String accessIndexKey = String.format(RedisConstants.Auth.USER_ACCESS_TOKEN_INDEX, userId);
        String refreshIndexKey = String.format(RedisConstants.Auth.USER_REFRESH_TOKEN_INDEX, userId);
        String refreshTokenId = "refresh-1";
        String boundAccessTokenId = "access-bound";
        String orphanAccessTokenId = "access-orphan";
        String orphanRefreshTokenId = "refresh-orphan";
        String refreshTokenKey = RedisConstants.Auth.USER_REFRESH_TOKEN + refreshTokenId;
        RefreshTokenSession refreshTokenSession = RefreshTokenSession.builder()
                .refreshTokenId(refreshTokenId)
                .accessTokenId(boundAccessTokenId)
                .userId(userId)
                .username("alice")
                .build();
        OnlineLoginUser orphanOnlineUser = OnlineLoginUser.builder()
                .accessTokenId(orphanAccessTokenId)
                .refreshTokenId(orphanRefreshTokenId)
                .userId(userId)
                .username("alice")
                .build();

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(setOperations.members(accessIndexKey)).thenReturn(Set.of(orphanAccessTokenId));
        when(setOperations.members(refreshIndexKey)).thenReturn(Set.of(refreshTokenId));
        when(valueOperations.get(refreshTokenKey)).thenReturn(refreshTokenSession);
        when(valueOperations.get(RedisConstants.Auth.USER_ACCESS_TOKEN + orphanAccessTokenId)).thenReturn(orphanOnlineUser);

        redisTokenStore.deleteSessionsByUserIds(Set.of(userId));

        ArgumentCaptor<Collection> deleteKeysCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(redisTemplate, times(2)).delete(deleteKeysCaptor.capture());
        assertTrue(deleteKeysCaptor.getAllValues().stream()
                .anyMatch(keys -> keys.containsAll(Set.of(
                        RedisConstants.Auth.USER_ACCESS_TOKEN + boundAccessTokenId,
                        refreshTokenKey,
                        RedisConstants.Auth.USER_ACCESS_TOKEN + orphanAccessTokenId,
                        RedisConstants.Auth.USER_REFRESH_TOKEN + orphanRefreshTokenId
                ))));
        assertTrue(deleteKeysCaptor.getAllValues().stream()
                .anyMatch(keys -> keys.containsAll(Set.of(accessIndexKey, refreshIndexKey))));
        verify(setOperations, never()).remove(any(), any());
    }

    /**
     * 创建 RedisTokenStore 测试对象。
     *
     * @return RedisTokenStore 测试对象
     */
    private RedisTokenStore createRedisTokenStore() {
        RedisCache redisCache = new RedisCache(redisTemplate);
        RedisSetCache redisSetCache = new RedisSetCache(redisTemplate);
        return new RedisTokenStore(redisCache, redisSetCache, securityProperties);
    }
}
