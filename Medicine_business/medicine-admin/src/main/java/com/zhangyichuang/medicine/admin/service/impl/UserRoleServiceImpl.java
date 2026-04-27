package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.RoleMapper;
import com.zhangyichuang.medicine.admin.mapper.UserRoleMapper;
import com.zhangyichuang.medicine.admin.service.UserRoleService;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.model.entity.Role;
import com.zhangyichuang.medicine.model.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole>
        implements UserRoleService {

    private final RoleMapper roleMapper;

    @Override
    public Set<Long> getUserRoleByUserId(Long id) {
        List<Long> list = lambdaQuery()
                .eq(UserRole::getUserId, id)
                .list()
                .stream()
                .map(UserRole::getRoleId)
                .toList();
        return new HashSet<>(list);
    }

    /**
     * 根据角色ID查询绑定用户ID集合。
     *
     * @param roleId 角色ID
     * @return 用户ID集合
     */
    @Override
    public Set<Long> getUserIdsByRoleId(Long roleId) {
        Assert.isPositive(roleId, "角色ID必须大于0");
        List<Long> userIds = lambdaQuery()
                .eq(UserRole::getRoleId, roleId)
                .list()
                .stream()
                .map(UserRole::getUserId)
                .toList();
        return new HashSet<>(userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRole(Long id, Set<Long> roles) {
        Assert.isPositive(id, "用户ID必须大于0");
        remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        if (CollectionUtils.isEmpty(roles)) {
            return;
        }

        LambdaQueryWrapper<Role> in = new LambdaQueryWrapper<Role>().in(Role::getId, roles);
        List<Long> roleIds = roleMapper.selectList(in).stream().map(Role::getId).toList();
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }

        List<UserRole> userRoles = roleIds.stream()
                .map(roleId -> {
                    UserRole userRole = new UserRole();
                    userRole.setUserId(id);
                    userRole.setRoleId(roleId);
                    return userRole;
                })
                .toList();
        saveBatch(userRoles);
    }
}
