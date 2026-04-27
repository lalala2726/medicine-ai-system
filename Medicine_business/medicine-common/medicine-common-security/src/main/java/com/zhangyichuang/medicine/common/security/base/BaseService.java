package com.zhangyichuang.medicine.common.security.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Chuang
 */
public interface BaseService {

    /**
     * 获取当前用户ID
     */
    default Long getUserId() {
        return SecurityUtils.getUserId();
    }

    /**
     * 获取当前用户名
     */
    default String getUsername() {
        return SecurityUtils.getUsername();
    }

    /**
     * 获取当前用户信息
     */
    default SysUserDetails getUser() {
        return SecurityUtils.getLoginUser();
    }

    /**
     * 获取当前用户角色
     */
    default Set<String> getRoles() {
        return SecurityUtils.getRoles();
    }

    /**
     * 获取当前用户权限
     */
    default Set<String> getPermissions() {
        return SecurityUtils.getPermissions();
    }

    /**
     * 检查当前用户是否拥有指定权限
     */
    default boolean hasPermission(String permission) {
        return SecurityUtils.hasPermission(permission);
    }

    /**
     * 统一归一化编码集合。
     * <p>
     * 该方法委托 {@link SecurityUtils#normalizeCodes(Collection)}，
     * 用于在服务层以一致规则处理角色码、权限码等字符串集合。
     * </p>
     *
     * @param values 待归一化编码集合
     * @return 去空、去空白、去重后的不可变集合
     */
    default Set<String> normalizeCodes(Collection<String> values) {
        return SecurityUtils.normalizeCodes(values);
    }

    /**
     * 统一归一化角色编码集合（输出纯角色码）。
     *
     * @param roleCodes 待处理角色集合，可包含 {@code ROLE_} 前缀
     * @return 纯角色编码集合
     */
    default Set<String> normalizeRoleCodes(Collection<String> roleCodes) {
        return SecurityUtils.normalizeRoleCodes(roleCodes);
    }

    /**
     * 将角色编码集合转换为标准角色权限集合（带 {@code ROLE_} 前缀）。
     *
     * @param roleCodes 角色集合
     * @return 标准角色权限集合
     */
    default Set<String> toRoleAuthorities(Collection<String> roleCodes) {
        return SecurityUtils.toRoleAuthorities(roleCodes);
    }

    /**
     * 统一归一化业务权限集合（自动过滤角色权限项）。
     *
     * @param permissionCodes 待处理权限集合
     * @return 业务权限集合
     */
    default Set<String> toPermissionAuthorities(Collection<String> permissionCodes) {
        return SecurityUtils.toPermissionAuthorities(permissionCodes);
    }

    /**
     * 对象属性拷贝
     */
    default <T, V> V copyProperties(T source, Class<V> targetClass) {
        return BeanCotyUtils.copyProperties(source, targetClass);
    }

    /**
     * 列表属性拷贝
     */
    default <T, V> List<V> copyListProperties(List<T> sourceList, Class<V> targetClass) {
        return BeanCotyUtils.copyListProperties(sourceList, targetClass);
    }

    /**
     * 分页对象属性拷贝
     */
    default <T, V> List<V> copyListProperties(Page<T> sourcePage, Class<V> targetClass) {
        return BeanCotyUtils.copyListProperties(sourcePage, targetClass);
    }

    /**
     * 判断是否登录
     *
     * @return boolean 如登录返回true, 否则返回false
     */
    default boolean isLogin() {
        return getUser() != null;
    }

    /**
     * 加密密码
     */
    default String encryptPassword(String password) {
        return SecurityUtils.encryptPassword(password);
    }

    /**
     * 密码匹配
     *
     * @param rawPassword     原始密码(未加密啊)
     * @param encodedPassword 已加密的密码
     * @return 密码匹配结果
     */
    default boolean matchesPassword(String rawPassword, String encodedPassword) {
        return SecurityUtils.matchesPassword(rawPassword, encodedPassword);
    }
}
