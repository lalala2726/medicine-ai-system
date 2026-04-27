package com.zhangyichuang.medicine.common.security.token;

import com.zhangyichuang.medicine.common.core.constants.SecurityConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.AuthorizationException;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.common.core.utils.IpAddressUtils;
import com.zhangyichuang.medicine.common.core.utils.UUIDUtils;
import com.zhangyichuang.medicine.common.security.entity.AuthTokenVo;
import com.zhangyichuang.medicine.common.security.entity.AuthUser;
import com.zhangyichuang.medicine.common.security.entity.OnlineLoginUser;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import com.zhangyichuang.medicine.model.dto.LoginSessionDTO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zhangyichuang.medicine.common.core.constants.SecurityConstants.CLAIM_KEY_SESSION_ID;

/**
 * Token 颁发与解析服务。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenStore redisTokenStore;
    private final ObjectProvider<UserDetailsService> userDetailsServices;

    /**
     * 基于认证结果创建访问令牌与刷新令牌，并将会话信息写入 Redis。
     *
     * @param authentication Spring Security 认证对象
     * @return 登录会话信息（包含访问令牌与刷新令牌）
     */
    public LoginSessionDTO createToken(Authentication authentication) {
        SysUserDetails userDetails = (SysUserDetails) authentication.getPrincipal();
        AuthUser authUser = userDetails.getUser();
        if (authUser == null) {
            throw new AuthorizationException(ResponseCode.UNAUTHORIZED, "用户信息异常");
        }
        String username = authUser.getUsername();
        Long userId = authUser.getId();

        String accessTokenSessionId = UUIDUtils.simple();
        String refreshTokenSessionId = UUIDUtils.simple();
        HttpServletRequest request = SecurityUtils.getHttpServletRequest();
        String ipAddress = IpAddressUtils.getIpAddress(request);
        long now = System.currentTimeMillis();

        Set<String> authorityCodes = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        RolePermissionSnapshot snapshot = resolveRolePermissionForSession(authUser, authorityCodes);

        OnlineLoginUser onlineLoginUser = OnlineLoginUser.builder()
                .accessTokenId(accessTokenSessionId)
                .refreshTokenId(refreshTokenSessionId)
                .userId(userId)
                .username(username)
                .user(buildSessionUser(authUser))
                .roles(snapshot.roleAuthorities())
                .permissions(snapshot.permissionAuthorities())
                .ip(ipAddress)
                .createTime(now)
                .updateTime(now)
                .accessTime(now)
                .build();

        redisTokenStore.saveLoginSession(accessTokenSessionId, refreshTokenSessionId, onlineLoginUser);

        String jwtAccessToken = jwtTokenProvider.createJwt(accessTokenSessionId, username);
        String jwtRefreshToken = jwtTokenProvider.createJwt(refreshTokenSessionId, username);

        return LoginSessionDTO.builder()
                .userId(userId)
                .accessTokenSessionId(accessTokenSessionId)
                .refreshTokenSessionId(refreshTokenSessionId)
                .username(username)
                .accessToken(jwtAccessToken)
                .refreshToken(jwtRefreshToken)
                .build();
    }

    /**
     * 使用刷新令牌换发新的访问令牌，并更新 Redis 中的访问会话。
     *
     * @param jwtRefreshToken 刷新令牌
     * @return 新的访问令牌与原刷新令牌
     */
    public AuthTokenVo refreshToken(String jwtRefreshToken) {
        Claims refreshClaims = jwtTokenProvider.getClaimsFromToken(jwtRefreshToken);
        if (refreshClaims == null) {
            throw new AuthorizationException(ResponseCode.REFRESH_TOKEN_INVALID);
        }

        String refreshTokenSessionId = refreshClaims.get(CLAIM_KEY_SESSION_ID, String.class);
        if (!redisTokenStore.isValidRefreshToken(refreshTokenSessionId)) {
            throw new AuthorizationException(ResponseCode.REFRESH_TOKEN_INVALID);
        }

        String username = refreshClaims.get(SecurityConstants.CLAIM_KEY_USERNAME, String.class);
        SysUserDetails userDetails = loadUserDetails(username);
        AuthUser authUser = userDetails.getUser();
        if (authUser == null) {
            throw new AuthorizationException(ResponseCode.REFRESH_TOKEN_INVALID, "无法加载用户信息");
        }
        Set<String> authorityCodes = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        RolePermissionSnapshot snapshot = resolveRolePermissionForSession(authUser, authorityCodes);

        String accessTokenSessionId = UUIDUtils.simple();
        String accessToken = jwtTokenProvider.createJwt(accessTokenSessionId, username);

        HttpServletRequest request = SecurityUtils.getHttpServletRequest();
        String ipAddress = IpAddressUtils.getIpAddress(request);
        long now = System.currentTimeMillis();

        OnlineLoginUser onlineLoginUser = OnlineLoginUser.builder()
                .accessTokenId(accessTokenSessionId)
                .refreshTokenId(refreshTokenSessionId)
                .userId(authUser.getId())
                .username(username)
                .user(buildSessionUser(authUser))
                .roles(snapshot.roleAuthorities())
                .permissions(snapshot.permissionAuthorities())
                .ip(ipAddress)
                .createTime(now)
                .updateTime(now)
                .accessTime(now)
                .build();

        redisTokenStore.replaceAccessToken(refreshTokenSessionId, accessTokenSessionId, onlineLoginUser);

        return AuthTokenVo.builder()
                .accessToken(accessToken)
                .refreshToken(jwtRefreshToken)
                .build();
    }

    /**
     * 根据用户名加载统一用户详情对象。
     *
     * @param username 用户名
     * @return 标准化用户详情
     */
    private SysUserDetails loadUserDetails(String username) {
        UserDetailsService userDetailsService = resolveUserDetailsService();
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails instanceof SysUserDetails sysUserDetails) {
            return sysUserDetails;
        }
        throw new AuthorizationException(ResponseCode.REFRESH_TOKEN_INVALID, "无法加载用户信息");
    }

    /**
     * 解析并选择唯一的 {@link UserDetailsService} 实现。
     *
     * @return 用户详情服务
     */
    private UserDetailsService resolveUserDetailsService() {
        var iterator = userDetailsServices.iterator();
        if (!iterator.hasNext()) {
            throw new AuthorizationException(ResponseCode.REFRESH_TOKEN_INVALID, "未配置用户详情服务");
        }
        UserDetailsService service = iterator.next();
        if (iterator.hasNext()) {
            throw new AuthorizationException(ResponseCode.REFRESH_TOKEN_INVALID, "存在多个用户详情服务，无法确定刷新令牌使用的实现");
        }
        return service;
    }

    /**
     * 解析访问令牌并从 Redis 会话恢复 Spring Security 认证信息。
     *
     * @param accessToken 访问令牌
     * @return 认证对象；无效时返回 {@code null}
     */
    public Authentication parseAccessToken(String accessToken) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(accessToken);
        if (claims == null) {
            log.warn("解析访问令牌失败或Claims为空: {}", accessToken);
            return null;
        }

        String accessTokenId = claims.get(CLAIM_KEY_SESSION_ID, String.class);
        if (StringUtils.isBlank(accessTokenId)) {
            log.warn("访问令牌JWT中未找到sessionId ({}): {}", CLAIM_KEY_SESSION_ID, accessToken);
            return null;
        }

        OnlineLoginUser onlineUser = redisTokenStore.getAccessToken(accessTokenId);
        if (onlineUser == null) {
            log.warn("Redis 中未找到访问令牌: {}", accessTokenId);
            return null;
        }

        boolean updateSuccess = redisTokenStore.updateAccessTime(accessTokenId);
        if (!updateSuccess) {
            log.warn("更新访问时间失败，令牌可能已被删除: {}", accessTokenId);
            return null;
        }

        RolePermissionSnapshot snapshot = resolveRolePermissionFromSession(onlineUser);
        Set<SimpleGrantedAuthority> authorities = Stream.concat(
                        snapshot.roleAuthorities().stream(),
                        snapshot.permissionAuthorities().stream())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        SysUserDetails userDetails = buildUserDetails(
                onlineUser,
                authorities,
                snapshot.roleAuthorities(),
                snapshot.permissionAuthorities());
        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    /**
     * 基于 Redis 在线会话构造 {@link SysUserDetails}。
     *
     * @param onlineUser            Redis 中的在线用户
     * @param authorities           已计算的权限集合
     * @param roleAuthorities       角色权限（ROLE_ 前缀）
     * @param permissionAuthorities 业务权限码
     * @return 统一用户详情
     */
    private SysUserDetails buildUserDetails(OnlineLoginUser onlineUser,
                                            Set<SimpleGrantedAuthority> authorities,
                                            Set<String> roleAuthorities,
                                            Set<String> permissionAuthorities) {
        AuthUser authUser = onlineUser.getUser();
        Set<String> roleCodes = SecurityUtils.toRoleCodes(roleAuthorities);
        if (authUser == null) {
            authUser = new AuthUser();
            authUser.setId(onlineUser.getUserId());
            authUser.setUsername(onlineUser.getUsername());
            authUser.setRoles(roleCodes);
            authUser.setPermissions(permissionAuthorities);
        } else {
            authUser.setRoles(roleCodes);
            authUser.setPermissions(permissionAuthorities);
        }
        SysUserDetails userDetails = new SysUserDetails(authUser);
        userDetails.setAuthorities(authorities);
        return userDetails;
    }

    /**
     * 构建用于 Redis 会话存储的用户信息快照。
     * <p>
     * 该快照仅保留基础用户字段。
     * 会话角色与权限统一由 {@link OnlineLoginUser#roles}/{@link OnlineLoginUser#permissions}
     * 作为单一来源进行存储，密码字段不写入 Redis。
     * </p>
     *
     * @param authUser 运行期认证用户
     * @return 适用于会话持久化的裁剪用户对象
     */
    private AuthUser buildSessionUser(AuthUser authUser) {
        AuthUser sessionUser = BeanCotyUtils.copyProperties(authUser, AuthUser.class);
        sessionUser.setPassword(null);
        sessionUser.setRoles(Set.of());
        sessionUser.setPermissions(Set.of());
        return sessionUser;
    }

    /**
     * 解析待写入会话的角色与权限快照。
     * <p>
     * 优先使用 {@link AuthUser} 中的角色/权限；若缺失则从 authorities 回退拆分。
     * </p>
     *
     * @param authUser       统一用户对象
     * @param authorityCodes 认证对象中的权限编码
     * @return 角色与权限快照
     */
    private RolePermissionSnapshot resolveRolePermissionForSession(AuthUser authUser, Set<String> authorityCodes) {
        Set<String> roleAuthorities = SecurityUtils.toRoleAuthorities(
                authUser == null ? Collections.emptySet() : authUser.getRoles());
        Set<String> permissionAuthorities = SecurityUtils.toPermissionAuthorities(
                authUser == null ? Collections.emptySet() : authUser.getPermissions());
        if (!authorityCodes.isEmpty()) {
            RolePermissionSnapshot authoritySnapshot = splitAuthorities(authorityCodes);
            if (roleAuthorities.isEmpty()) {
                roleAuthorities = authoritySnapshot.roleAuthorities();
            }
            if (permissionAuthorities.isEmpty()) {
                permissionAuthorities = authoritySnapshot.permissionAuthorities();
            }
        }
        return new RolePermissionSnapshot(roleAuthorities, permissionAuthorities);
    }

    /**
     * 从 Redis 会话恢复角色与权限快照。
     * <p>
     * 优先读取会话中的 permissions；若旧会话缺失 permissions，则从混合 roles 回退拆分。
     * </p>
     *
     * @param onlineUser Redis 在线会话
     * @return 角色与权限快照
     */
    private RolePermissionSnapshot resolveRolePermissionFromSession(OnlineLoginUser onlineUser) {
        RolePermissionSnapshot roleSnapshot = splitAuthorities(onlineUser.getRoles());
        Set<String> permissions = SecurityUtils.toPermissionAuthorities(onlineUser.getPermissions());
        if (permissions.isEmpty()) {
            permissions = roleSnapshot.permissionAuthorities();
        } else if (!roleSnapshot.permissionAuthorities().isEmpty()) {
            LinkedHashSet<String> merged = new LinkedHashSet<>(permissions);
            merged.addAll(roleSnapshot.permissionAuthorities());
            permissions = Set.copyOf(merged);
        }
        return new RolePermissionSnapshot(roleSnapshot.roleAuthorities(), permissions);
    }

    /**
     * 将混合权限集合拆分为角色权限与业务权限。
     *
     * @param authorityCodes 混合权限编码集合
     * @return 角色与权限快照
     */
    private RolePermissionSnapshot splitAuthorities(Set<String> authorityCodes) {
        LinkedHashSet<String> roleAuthorities = new LinkedHashSet<>();
        LinkedHashSet<String> permissionAuthorities = new LinkedHashSet<>();
        for (String authority : SecurityUtils.normalizeCodes(authorityCodes)) {
            if (isRoleAuthority(authority)) {
                String roleAuthority = SecurityUtils.toRoleAuthority(authority);
                if (roleAuthority != null) {
                    roleAuthorities.add(roleAuthority);
                }
                continue;
            }
            permissionAuthorities.add(authority);
        }
        return new RolePermissionSnapshot(Set.copyOf(roleAuthorities), Set.copyOf(permissionAuthorities));
    }

    /**
     * 判断编码是否应视为角色项。
     *
     * @param authority 待判断编码
     * @return true 表示角色项
     */
    private boolean isRoleAuthority(String authority) {
        if (SecurityUtils.isPrefixedRoleAuthority(authority)) {
            return true;
        }
        return !looksLikePermissionAuthority(authority);
    }

    /**
     * 判断编码是否更像业务权限（包含分隔符）。
     *
     * @param authority 编码
     * @return true 表示业务权限风格编码
     */
    private boolean looksLikePermissionAuthority(String authority) {
        return authority.contains(":") || authority.contains(".") || authority.contains("/");
    }

    private record RolePermissionSnapshot(Set<String> roleAuthorities, Set<String> permissionAuthorities) {
    }
}
