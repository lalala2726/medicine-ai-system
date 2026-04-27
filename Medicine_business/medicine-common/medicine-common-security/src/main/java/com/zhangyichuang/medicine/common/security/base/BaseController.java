package com.zhangyichuang.medicine.common.security.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Chuang
 * <p>
 * created on 2025/3/20
 */
@Component
public class BaseController {


    /**
     * 将 List<T> 转换为 List<V>，使用 BeanUtils 进行属性拷贝
     *
     * @param sourceList  源数据列表
     * @param targetClass 目标类型的 Class
     * @param <T>         源数据类型
     * @param <V>         目标数据类型
     * @return 转换后的目标数据列表
     */
    protected static <T, V> List<V> copyListProperties(List<T> sourceList, Class<V> targetClass) {
        return BeanCotyUtils.copyListProperties(sourceList, targetClass);
    }

    /**
     * 将 source 对象的属性复制到一个新的 targetClass 实例中
     *
     * @param source      原对象
     * @param targetClass 目标类 class 对象
     * @return 拷贝后的目标对象
     */
    protected static <T, V> V copyProperties(T source, Class<V> targetClass) {
        return BeanCotyUtils.copyProperties(source, targetClass);
    }


    /**
     * 将 Page<T> 转换为 List<V>，使用 BeanUtils 进行属性拷贝
     *
     * @param sourceList  源数据列表
     * @param targetClass 目标类型的 Class
     * @param <T>         源数据类型
     * @param <V>         目标数据类型
     * @return 转换后的目标数据列表
     */
    protected static <T, V> List<V> copyListProperties(Page<T> sourceList, Class<V> targetClass) {
        return BeanCotyUtils.copyListProperties(sourceList, targetClass);
    }

    /**
     * 封装分页数据,直接返回数据
     *
     * @param page 分页对象
     */
    protected AjaxResult<TableDataResult> getTableData(Page<?> page) {
        return TableDataResult.build(page);
    }

    /**
     * 封装分页数据,如果想要返回VO必须传入VO对象,否则返回的数据总数和页码不正确
     *
     * @param page 分页对象
     * @param rows 列表数据
     */
    protected AjaxResult<TableDataResult> getTableData(Page<?> page, List<?> rows) {
        return TableDataResult.build(page, rows);
    }

    /**
     * 封装分页数据,如果想要返回VO必须传入VO对象,否则返回的数据总数和页码不正确
     *
     * @param page  分页对象
     * @param rows  列表数据
     * @param extra 额外的数据
     * @return 封装后的分页数据
     */
    protected AjaxResult<TableDataResult> getTableData(Page<?> page, List<?> rows, Map<String, Object> extra) {
        return TableDataResult.build(page, rows, extra);
    }

    /**
     * 使用自定义分页封装分页结果
     *
     * @param page 自定义分页
     * @return 封装后的分页数据
     */
    protected AjaxResult<TableDataResult> getTableData(PageResult<?> page) {
        return TableDataResult.build(page);
    }


    /**
     * 获取当前用户信息
     *
     * @return 当前用户信息
     */
    protected SysUserDetails getLoginUser() {
        return SecurityUtils.getLoginUser();
    }


    /**
     * 获取当前用户名
     *
     * @return 当前用户名
     */
    protected String getUsername() {
        return SecurityUtils.getUsername();
    }


    /**
     * 获取当前用户角色
     *
     * @return 角色集合
     */
    protected Set<String> getRoles() {
        return SecurityUtils.getRoles();
    }

    /**
     * 获取当前用户权限
     *
     * @return 权限集合
     */
    protected Set<String> getPermissions() {
        return SecurityUtils.getPermissions();
    }

    /**
     * 检查当前用户是否拥有指定权限
     *
     * @param permission 权限编码
     * @return 是否拥有权限
     */
    protected boolean hasPermission(String permission) {
        return SecurityUtils.hasPermission(permission);
    }

    /**
     * 统一归一化编码集合。
     * <p>
     * 该方法委托 {@link SecurityUtils#normalizeCodes(Collection)}，
     * 用于控制层在处理请求参数时复用统一的清洗规则。
     * </p>
     *
     * @param values 待归一化编码集合
     * @return 去空、去空白、去重后的不可变集合
     */
    protected Set<String> normalizeCodes(Collection<String> values) {
        return SecurityUtils.normalizeCodes(values);
    }

    /**
     * 统一归一化角色编码集合（输出纯角色码）。
     *
     * @param roleCodes 待处理角色集合，可包含 {@code ROLE_} 前缀
     * @return 纯角色编码集合
     */
    protected Set<String> normalizeRoleCodes(Collection<String> roleCodes) {
        return SecurityUtils.normalizeRoleCodes(roleCodes);
    }

    /**
     * 将角色编码集合转换为标准角色权限集合（带 {@code ROLE_} 前缀）。
     *
     * @param roleCodes 待处理角色集合
     * @return 标准角色权限集合
     */
    protected Set<String> toRoleAuthorities(Collection<String> roleCodes) {
        return SecurityUtils.toRoleAuthorities(roleCodes);
    }

    /**
     * 统一归一化业务权限集合（自动过滤角色权限项）。
     *
     * @param permissionCodes 待处理权限集合
     * @return 业务权限集合
     */
    protected Set<String> toPermissionAuthorities(Collection<String> permissionCodes) {
        return SecurityUtils.toPermissionAuthorities(permissionCodes);
    }

    /**
     * 获取当前用户id
     *
     * @return 当前用户id
     */
    protected Long getUserId() {
        return SecurityUtils.getUserId();
    }

    /**
     * 成功返回
     *
     * @return AjaxResult
     */
    protected <T> AjaxResult<T> success() {
        return AjaxResult.success();
    }

    /**
     * 失败返回
     *
     * @return AjaxResult
     */
    protected <T> AjaxResult<T> error() {
        return AjaxResult.error();
    }

    /**
     * 返回结果,根据boolean值返回成功或者失败
     */
    protected <T> AjaxResult<T> toAjax(boolean result) {
        return result ? success() : error();
    }

    /**
     * 返回结果,根据int值返回成功或者失败
     *
     * @param result int值
     * @return 结果
     */
    protected <T> AjaxResult<T> toAjax(int result) {
        return result > 0 ? success() : error();
    }

    /**
     * 返回结果,根据long值返回成功或者失败
     *
     * @param result long值
     * @return 结果
     */
    protected <T> AjaxResult<T> toAjax(long result) {
        return result > 0 ? success() : error();
    }


    /**
     * 判断用户是否登录
     *
     * @return true:已登录, false:未登录
     */
    protected boolean isLogin() {
        return SecurityUtils.getUserId() != null;
    }

    /**
     * 成功返回
     */
    protected <T> AjaxResult<T> success(T data) {
        return AjaxResult.success(data);
    }

    /**
     * 成功返回
     *
     * @param message 消息
     * @return AjaxResult
     */
    protected <T> AjaxResult<T> success(String message) {
        return AjaxResult.success(message);
    }

    /**
     * 警告返回
     *
     * @param message 消息
     * @return AjaxResult
     */
    protected <T> AjaxResult<T> warning(String message) {
        return AjaxResult.warning(message);
    }

    /**
     * 失败返回
     *
     * @param message 消息
     */
    protected <T> AjaxResult<T> error(String message) {
        return AjaxResult.error(message);
    }


    /**
     * 失败返回
     *
     * @param resultCode 响应枚举
     * @return 结果
     */
    protected <T> AjaxResult<T> error(ResponseCode resultCode) {
        return AjaxResult.error(resultCode);
    }

    /**
     * 加密密码
     *
     * @param password 密码
     * @return 加密后的密码
     */
    protected String encryptPassword(String password) {
        return SecurityUtils.encryptPassword(password);
    }

    /**
     * 验证密码是否匹配
     *
     * @param rawPassword     密码
     * @param encodedPassword 密码
     * @return 是否匹配
     */
    protected boolean matchesPassword(String rawPassword, String encodedPassword) {
        return SecurityUtils.matchesPassword(rawPassword, encodedPassword);
    }


    /**
     * 获取当前登录用户的访问令牌
     *
     * @return 访问令牌
     */
    protected String getToken() {
        return SecurityUtils.getToken();
    }


}
