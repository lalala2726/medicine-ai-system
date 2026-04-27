package com.zhangyichuang.medicine.common.security.utils;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.zhangyichuang.medicine.common.core.constants.RolesConstant;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.LoginException;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 安全工具类
 *
 * @author Chuang
 */
@Slf4j
public class SecurityUtils {

    private static final String ROLE_PREFIX = "ROLE_";

    @Value("${security.header}")
    private static final String HEADER = "Authorization";

    /**
     * 令牌前缀
     */
    @Value("${security.session.token-prefix}")
    private static final String TOKEN_PREFIX = "Bearer";

    /**
     * 获取用户
     *
     * @return LoginUser
     */
    public static SysUserDetails getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SysUserDetails)) {
            throw new LoginException(ResponseCode.UNAUTHORIZED, "用户未登录");
        }
        return (SysUserDetails) authentication.getPrincipal();
    }

    /**
     * 生成BCryptPasswordEncoder密码
     *
     * @param password 密码
     * @return 加密字符串
     */
    public static String encryptPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }


    /**
     * 判断密码是否相同,这边原始的密码是明文的不需要额外加密
     *
     * @param rawPassword     真实密码(未加密的密码)
     * @param encodedPassword 加密后字符
     * @return true代表相同，false代表不同
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }


    /**
     * 是否拥有某个角色
     *
     * @return true代表有，false代表无
     */
    public static boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    /**
     * 归一化编码集合。
     * <p>
     * 该方法会对输入集合执行统一清洗规则：
     * 1) 过滤 {@code null} 元素；
     * 2) 去除首尾空白；
     * 3) 过滤空字符串；
     * 4) 按插入顺序去重；
     * 5) 返回不可变集合。
     * </p>
     *
     * @param values 待清洗编码集合，可为 {@code null}
     * @return 归一化后的不可变集合；当输入为空或清洗后无有效元素时返回 {@link Set#of()}
     */
    public static Set<String> normalizeCodes(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        if (normalized.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(normalized);
    }

    /**
     * 归一化角色编码集合。
     * <p>
     * 在 {@link #normalizeCodes(Collection)} 基础上，进一步移除每个角色项可能携带的
     * {@code ROLE_} 前缀，并返回纯角色编码集合（如 {@code admin}、{@code super_admin}）。
     * </p>
     *
     * @param roleCodes 角色编码集合（可混合 {@code ROLE_xxx} 与纯角色码）
     * @return 去前缀后的角色编码不可变集合
     */
    public static Set<String> normalizeRoleCodes(Collection<String> roleCodes) {
        Set<String> normalized = normalizeCodes(roleCodes);
        if (normalized.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> roles = new LinkedHashSet<>();
        for (String roleCode : normalized) {
            String normalizedRole = removeRolePrefix(roleCode);
            if (!normalizedRole.isEmpty()) {
                roles.add(normalizedRole);
            }
        }
        if (roles.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(roles);
    }

    /**
     * 将角色编码集合转换为标准角色权限集合（统一带 {@code ROLE_} 前缀）。
     *
     * @param roleCodes 角色编码集合（可混合纯角色码和 {@code ROLE_} 前缀角色）
     * @return 标准角色权限集合，如 {@code ROLE_admin}
     */
    public static Set<String> toRoleAuthorities(Collection<String> roleCodes) {
        Set<String> normalizedRoleCodes = normalizeRoleCodes(roleCodes);
        if (normalizedRoleCodes.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> roleAuthorities = new LinkedHashSet<>();
        for (String roleCode : normalizedRoleCodes) {
            String roleAuthority = toRoleAuthority(roleCode);
            if (roleAuthority != null) {
                roleAuthorities.add(roleAuthority);
            }
        }
        if (roleAuthorities.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(roleAuthorities);
    }

    /**
     * 将权限编码集合标准化为业务权限集合。
     * <p>
     * 会过滤掉角色权限项（即带 {@code ROLE_} 前缀的编码），仅保留业务权限码。
     * </p>
     *
     * @param permissionCodes 待处理权限编码集合
     * @return 业务权限不可变集合
     */
    public static Set<String> toPermissionAuthorities(Collection<String> permissionCodes) {
        Set<String> normalized = normalizeCodes(permissionCodes);
        if (normalized.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> permissions = new LinkedHashSet<>();
        for (String permissionCode : normalized) {
            if (!isPrefixedRoleAuthority(permissionCode)) {
                permissions.add(permissionCode);
            }
        }
        if (permissions.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(permissions);
    }

    /**
     * 将角色权限集合转换为纯角色编码集合。
     * <p>
     * 该方法与 {@link #normalizeRoleCodes(Collection)} 语义一致，主要用于表达
     * “由角色权限恢复角色码”的意图。
     * </p>
     *
     * @param roleAuthorities 角色权限集合（通常为 {@code ROLE_xxx}）
     * @return 纯角色编码集合
     */
    public static Set<String> toRoleCodes(Collection<String> roleAuthorities) {
        return normalizeRoleCodes(roleAuthorities);
    }

    /**
     * 去除角色编码前缀 {@code ROLE_}。
     *
     * @param roleOrAuthority 角色编码或角色权限编码
     * @return 去前缀后的角色码；若输入为空白则返回空字符串
     */
    public static String removeRolePrefix(String roleOrAuthority) {
        if (roleOrAuthority == null) {
            return "";
        }
        String trimmed = roleOrAuthority.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (isPrefixedRoleAuthority(trimmed)) {
            return trimmed.substring(ROLE_PREFIX.length()).trim();
        }
        return trimmed;
    }

    /**
     * 将角色编码转换为标准角色权限编码（带 {@code ROLE_} 前缀）。
     *
     * @param roleCode 角色编码或角色权限编码
     * @return 标准角色权限编码；当入参无有效角色内容时返回 {@code null}
     */
    public static String toRoleAuthority(String roleCode) {
        String normalizedRoleCode = removeRolePrefix(roleCode);
        if (normalizedRoleCode.isEmpty()) {
            return null;
        }
        return ROLE_PREFIX + normalizedRoleCode;
    }

    /**
     * 判断编码是否带有标准角色前缀 {@code ROLE_}（忽略大小写）。
     *
     * @param authority 待判断编码
     * @return true 表示带有角色前缀
     */
    public static boolean isPrefixedRoleAuthority(String authority) {
        return authority != null
                && authority.regionMatches(true, 0, ROLE_PREFIX, 0, ROLE_PREFIX.length());
    }

    /**
     * 是否拥有任一权限编码（精确匹配）。
     *
     * @param permissionCodes 权限编码集合
     * @return true: 命中任意权限，false: 未命中
     */
    public static boolean hasAnyPermission(Set<String> permissionCodes) {
        Set<String> normalizedPermissions = normalizeCodes(permissionCodes);
        if (normalizedPermissions.isEmpty()) {
            return false;
        }
        Set<String> currentPermissions = getPermissions();
        if (currentPermissions.isEmpty()) {
            return false;
        }
        return normalizedPermissions.stream().anyMatch(currentPermissions::contains);
    }

    /**
     * 是否拥有指定权限编码（精确匹配）。
     *
     * @param permissionCode 权限编码
     * @return true: 命中权限，false: 未命中
     */
    public static boolean hasPermission(String permissionCode) {
        if (permissionCode == null) {
            return false;
        }
        String trimmed = permissionCode.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return getPermissions().contains(trimmed);
    }

    /**
     * 是否拥有任一角色编码（精确匹配，支持 ROLE_ 前缀入参）。
     *
     * @param roleCodes 角色编码集合
     * @return true: 命中任意角色，false: 未命中
     */
    public static boolean hasAnyRole(Set<String> roleCodes) {
        Set<String> normalizedRoles = normalizeRoleCodes(roleCodes);
        if (normalizedRoles.isEmpty()) {
            return false;
        }

        Set<String> currentRoles = getRoles();
        if (currentRoles.isEmpty()) {
            return false;
        }
        return normalizedRoles.stream().anyMatch(currentRoles::contains);
    }


    /**
     * 获取当前请求对象
     */
    private static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取用户名
     */
    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    /**
     * 判断是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        Set<String> roles = getRoles();
        return roles.contains(RolesConstant.SUPER_ADMIN);
    }

    /**
     * 判断当前请求是否已认证
     */
    public static boolean isAuthenticated() {
        // 匿名请求在 Spring Security 中会以 AnonymousAuthenticationToken 表示，需要单独排除
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    /**
     * 获取用户角色集合
     *
     * @return 角色集合
     */
    public static Set<String> getRoles() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getAuthorities)
                .filter(CollectionUtils::isNotEmpty)
                .stream()
                .flatMap(Collection::stream)
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(authority -> !authority.isEmpty())
                .map(SecurityUtils::normalizeRoleAuthority)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 获取当前用户权限集合（不包含角色权限）。
     *
     * @return 权限集合
     */
    public static Set<String> getPermissions() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getAuthorities)
                .filter(CollectionUtils::isNotEmpty)
                .stream()
                .flatMap(Collection::stream)
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(authority -> !authority.isEmpty())
                .filter(authority -> normalizeRoleAuthority(authority) == null)
                .collect(Collectors.toSet());
    }

    private static String normalizeRoleAuthority(String authority) {
        if (isPrefixedRoleAuthority(authority)) {
            String role = removeRolePrefix(authority);
            return role.isEmpty() ? null : role;
        }
        // 兼容历史数据: 旧 token 中角色可能未带 ROLE_ 前缀
        if (isLikelyRoleAuthority(authority)) {
            return authority.trim();
        }
        return null;
    }

    private static boolean isLikelyRoleAuthority(String authority) {
        return !authority.contains(":") && !authority.contains(".") && !authority.contains("/");
    }

    /**
     * 获取当前请求的 Token
     */
    public static String getToken() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(TOKEN_PREFIX.trim() + " ")) {
            header = header.substring(TOKEN_PREFIX.length()).trim();
        }
        return header;
    }

    /**
     * 获取当前请求的Request
     *
     * @return request
     */
    public static HttpServletRequest getHttpServletRequest() {
        return ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
    }
}
