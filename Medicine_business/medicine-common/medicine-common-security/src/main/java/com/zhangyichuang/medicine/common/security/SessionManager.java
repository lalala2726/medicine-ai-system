package com.zhangyichuang.medicine.common.security;

import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.constants.SecurityConstants;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import com.zhangyichuang.medicine.common.security.entity.OnlineLoginUser;
import com.zhangyichuang.medicine.common.security.token.JwtTokenProvider;
import com.zhangyichuang.medicine.common.security.token.RedisTokenStore;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Chuang
 * <p>
 * created on 2025/9/25
 */
@Component("sessionManager")
public class SessionManager {


    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenStore redisTokenStore;
    private final RedisCache redisCache;

    public SessionManager(JwtTokenProvider jwtTokenProvider, RedisTokenStore redisTokenStore, RedisCache redisCache) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTokenStore = redisTokenStore;
        this.redisCache = redisCache;
    }

    /**
     * 检查角色
     *
     * @param role 角色
     * @return true 表示当前用户拥有该角色
     */
    public boolean checkRole(String role) {
        return SecurityUtils.getRoles().contains(role);
    }

    /**
     * 注销特定用户的登录
     *
     * @param username 用户名
     * @return true 表示注销流程执行完成
     */
    public boolean logout(String username) {
        Map<String, Object> map = redisCache.scanKeysWithValues(RedisConstants.Auth.USER_ACCESS_TOKEN + "*");
        map.forEach((key, value) -> {
            OnlineLoginUser onlineLoginUser = (OnlineLoginUser) value;
            if (onlineLoginUser.getUser() != null && onlineLoginUser.getUser().getUsername().equals(username)) {
                redisTokenStore.deleteSessionByAccessId(onlineLoginUser.getAccessTokenId());
            }
        });
        return true;
    }

    /**
     * 通过访问令牌注销此用户。
     *
     * @param accessToken 访问令牌
     */
    public void logoutByToken(String accessToken) {
        Claims claimsFromToken = jwtTokenProvider.getClaimsFromToken(accessToken);
        String accessTokenId = claimsFromToken.get(SecurityConstants.CLAIM_KEY_SESSION_ID).toString();
        //删除令牌
        redisTokenStore.deleteSessionByAccessId(accessTokenId);
    }

}
