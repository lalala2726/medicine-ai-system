package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.RoleMapper;
import com.zhangyichuang.medicine.admin.model.request.RoleAddRequest;
import com.zhangyichuang.medicine.admin.model.request.RoleListRequest;
import com.zhangyichuang.medicine.admin.model.request.RolePermissionUpdateRequest;
import com.zhangyichuang.medicine.admin.model.request.RoleUpdateRequest;
import com.zhangyichuang.medicine.admin.service.RolePermissionService;
import com.zhangyichuang.medicine.admin.service.RoleService;
import com.zhangyichuang.medicine.admin.service.UserRoleService;
import com.zhangyichuang.medicine.common.core.base.Option;
import com.zhangyichuang.medicine.common.core.constants.RolesConstant;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.common.security.token.RedisTokenStore;
import com.zhangyichuang.medicine.model.entity.Role;
import com.zhangyichuang.medicine.model.entity.RolePermission;
import com.zhangyichuang.medicine.model.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role>
        implements RoleService, BaseService {

    private final RoleMapper roleMapper;
    private final UserRoleService userRoleService;
    private final RolePermissionService rolePermissionService;
    private final RedisTokenStore redisTokenStore;

    @Override
    public Page<Role> roleList(RoleListRequest query) {
        Page<Role> page = query.toPage();
        return roleMapper.roleList(page, query);
    }

    @Override
    public Role getRoleById(Long id) {
        Assert.isPositive(id, "角色ID必须大于0");
        return getById(id);
    }

    @Override
    public boolean addRole(RoleAddRequest request) {
        Assert.notNull(request, "角色信息不能为空");
        checkRoleCodeUnique(request.getRoleCode(), null);
        checkRoleNameUnique(request.getRoleName(), null);
        Role role = copyProperties(request, Role.class);
        return save(role);
    }

    @Override
    public boolean updateRoleById(RoleUpdateRequest request) {
        Assert.notNull(request, "角色信息不能为空");
        Assert.isPositive(request.getId(), "角色ID必须大于0");
        if (RolesConstant.SUPER_ADMIN_ROLE_ID.equals(request.getId())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "超级管理员角色禁止修改");
        }
        checkRoleCodeUnique(request.getRoleCode(), request.getId());
        checkRoleNameUnique(request.getRoleName(), request.getId());
        Role role = copyProperties(request, Role.class);
        return updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRoleByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "角色ID不能为空");
        }
        if (ids.contains(RolesConstant.SUPER_ADMIN_ROLE_ID)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "超级管理员角色禁止删除");
        }

        boolean hasAssignedUsers = userRoleService.lambdaQuery()
                .in(UserRole::getRoleId, ids)
                .count() > 0;
        Assert.isTrue(!hasAssignedUsers, "已分配给用户的角色禁止删除");

        rolePermissionService.remove(new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, ids));
        return removeByIds(ids);
    }

    @Override
    public void isRoleExistById(Long id) {
        Assert.isPositive(id, "角色ID必须大于0");
        Role role = getById(id);
        Assert.isTrue(role != null, "角色不存在");
    }

    @Override
    public void isRoleExistById(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        long count = lambdaQuery().in(Role::getId, ids).count();
        Assert.isTrue(count == ids.size(), "角色不存在");
    }

    @Override
    public boolean isRoleExitsByRoleCode(String roleCode) {
        Assert.notEmpty(roleCode, "角色编码不能为空");
        return lambdaQuery().eq(Role::getRoleCode, roleCode).count() > 0;
    }

    @Override
    public List<Long> getRolePermission(Long id) {
        isRoleExistById(id);
        if (RolesConstant.SUPER_ADMIN_ROLE_ID.equals(id)) {
            return rolePermissionService.getAllPermissionIds();
        }
        return rolePermissionService.getRolePermission(id);
    }

    /**
     * 更新角色权限，并清理绑定该角色用户的在线会话。
     *
     * @param request 角色权限更新请求
     * @return true 表示更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRolePermission(RolePermissionUpdateRequest request) {
        Assert.notNull(request, "角色权限信息不能为空");
        if (RolesConstant.SUPER_ADMIN_ROLE_ID.equals(request.getRoleId())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "超级管理员角色禁止修改");
        }
        boolean updated = rolePermissionService.updateRolePermission(request);
        if (updated) {
            Set<Long> userIds = userRoleService.getUserIdsByRoleId(request.getRoleId());
            clearRoleUserSessionsAfterCommit(userIds);
        }
        return updated;
    }

    /**
     * 注册角色绑定用户会话清理动作，保证角色权限提交成功后旧权限立即失效。
     *
     * @param userIds 绑定该角色的用户ID集合
     */
    private void clearRoleUserSessionsAfterCommit(Set<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        Set<Long> normalizedUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .filter(userId -> userId > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedUserIds.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisTokenStore.deleteSessionsByUserIds(normalizedUserIds);
                }
            });
            return;
        }
        redisTokenStore.deleteSessionsByUserIds(normalizedUserIds);
    }

    @Override
    public List<Option<Long>> roleOption() {
        return lambdaQuery()
                .eq(Role::getStatus, 0)
                .list()
                .stream()
                .map(role -> new Option<>(role.getId(), role.getRoleName()))
                .toList();
    }

    @Override
    public Set<String> getUserRoleByUserId(Long userId) {
        Assert.isPositive(userId, "用户ID必须大于0");
        Set<Long> roleIds = userRoleService.getUserRoleByUserId(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return Set.of();
        }
        List<String> roleCodes = lambdaQuery()
                .in(Role::getId, roleIds)
                .eq(Role::getStatus, 0)
                .list()
                .stream()
                .map(Role::getRoleCode)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .toList();
        if (CollectionUtils.isEmpty(roleCodes)) {
            return Set.of();
        }
        return Set.copyOf(roleCodes);
    }

    @Override
    public Set<Long> getRoleIdByUserId(Long userId) {
        Assert.isPositive(userId, "用户ID必须大于0");
        Set<Long> roleIds = userRoleService.getUserRoleByUserId(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return Set.of();
        }
        List<Long> existingIds = lambdaQuery()
                .in(Role::getId, roleIds)
                .eq(Role::getStatus, 0)
                .list()
                .stream()
                .map(Role::getId)
                .toList();
        if (CollectionUtils.isEmpty(existingIds)) {
            return Set.of();
        }
        return Set.copyOf(existingIds);
    }

    private void checkRoleCodeUnique(String roleCode, Long excludeId) {
        if (roleCode == null) {
            return;
        }
        boolean exists = lambdaQuery()
                .eq(Role::getRoleCode, roleCode)
                .ne(excludeId != null, Role::getId, excludeId)
                .count() > 0;
        Assert.isTrue(!exists, "角色标识已存在");
    }

    private void checkRoleNameUnique(String roleName, Long excludeId) {
        if (roleName == null) {
            return;
        }
        boolean exists = lambdaQuery()
                .eq(Role::getRoleName, roleName)
                .ne(excludeId != null, Role::getId, excludeId)
                .count() > 0;
        Assert.isTrue(!exists, "角色名称已存在");
    }
}
