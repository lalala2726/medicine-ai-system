package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.PermissionMapper;
import com.zhangyichuang.medicine.admin.mapper.RolePermissionMapper;
import com.zhangyichuang.medicine.admin.model.request.PermissionAddRequest;
import com.zhangyichuang.medicine.admin.model.request.PermissionUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.PermissionTreeVo;
import com.zhangyichuang.medicine.admin.service.PermissionService;
import com.zhangyichuang.medicine.admin.service.RolePermissionService;
import com.zhangyichuang.medicine.admin.service.RoleService;
import com.zhangyichuang.medicine.common.core.base.Option;
import com.zhangyichuang.medicine.common.core.constants.RolesConstant;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.entity.Permission;
import com.zhangyichuang.medicine.model.entity.RolePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission>
        implements PermissionService, BaseService {

    private final RolePermissionMapper rolePermissionMapper;
    private final RoleService roleService;
    private final RolePermissionService rolePermissionService;

    @Override
    public List<PermissionTreeVo> permissionTree() {
        List<Permission> permissions = lambdaQuery().list();
        return buildPermissionTree(permissions);
    }

    @Override
    public Permission getPermissionById(Long id) {
        Assert.isPositive(id, "权限ID必须大于0");
        return getById(id);
    }

    @Override
    public boolean addPermission(PermissionAddRequest request) {
        Assert.notNull(request, "权限信息不能为空");
        Long parentId = Optional.ofNullable(request.getParentId()).orElse(0L);
        if (parentId > 0) {
            Permission parent = getById(parentId);
            Assert.isTrue(parent != null, "父级权限不存在");
            Assert.isTrue(request.getPermissionCode().startsWith(parent.getPermissionCode()),
                    "权限前缀必须以父级权限前缀开头");
        }
        Permission permission = copyProperties(request, Permission.class);
        return save(permission);
    }

    @Override
    public boolean updatePermissionById(PermissionUpdateRequest request) {
        Assert.notNull(request, "权限信息不能为空");
        Assert.isPositive(request.getId(), "权限ID必须大于0");
        Long parentId = Optional.ofNullable(request.getParentId()).orElse(0L);
        if (parentId > 0) {
            Assert.isTrue(!parentId.equals(request.getId()), "父级权限不能是自己");
            Permission parent = getById(parentId);
            Assert.isTrue(parent != null, "父级权限不存在");
            Assert.isTrue(request.getPermissionCode().startsWith(parent.getPermissionCode()),
                    "权限前缀必须以父级权限前缀开头");
        }
        Permission permission = copyProperties(request, Permission.class);
        return updateById(permission);
    }

    @Override
    public boolean deletePermissionByIds(List<Long> ids) {
        Assert.notEmpty(ids, "权限ID不能为空");
        ids.forEach(id -> Assert.isTrue(
                count(new LambdaQueryWrapper<Permission>().eq(Permission::getParentId, id)) == 0,
                "存在子级权限，不能删除"));
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().in(RolePermission::getPermissionId, ids));
        return removeByIds(ids);
    }

    @Override
    public List<Option<Long>> permissionOption() {
        List<Permission> permissions = lambdaQuery().list();
        return buildPermissionOption(permissions);
    }

    @Override
    public Set<String> getPermissionCodesByUserId(Long userId) {
        Assert.isPositive(userId, "用户ID必须大于0");
        if (RolesConstant.SUPER_ADMIN_USER_ID.equals(userId)) {
            return getAllPermissionCodes();
        }

        Set<String> roleCodes = Optional.ofNullable(roleService.getUserRoleByUserId(userId)).orElseGet(Set::of);
        if (roleCodes.stream().anyMatch(RolesConstant.SUPER_ADMIN::equalsIgnoreCase)) {
            return getAllPermissionCodes();
        }

        Set<Long> roleIds = roleService.getRoleIdByUserId(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return Set.of();
        }
        if (roleIds.contains(RolesConstant.SUPER_ADMIN_ROLE_ID)) {
            return getAllPermissionCodes();
        }

        List<Long> permissionIds = rolePermissionService.getRolePermission(List.copyOf(roleIds));
        if (CollectionUtils.isEmpty(permissionIds)) {
            return Set.of();
        }

        List<String> permissionCodes = lambdaQuery()
                .in(Permission::getId, permissionIds)
                .eq(Permission::getStatus, 0)
                .list()
                .stream()
                .map(Permission::getPermissionCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
        return Set.copyOf(permissionCodes);
    }

    private Set<String> getAllPermissionCodes() {
        return lambdaQuery()
                .eq(Permission::getStatus, 0)
                .list()
                .stream()
                .map(Permission::getPermissionCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toSet());
    }

    private List<PermissionTreeVo> buildPermissionTree(List<Permission> permissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            return Collections.emptyList();
        }
        Map<Long, List<Permission>> parentMap = permissions.stream()
                .collect(Collectors.groupingBy(permission -> Optional.ofNullable(permission.getParentId()).orElse(0L)));
        return buildTreeByParent(0L, parentMap);
    }

    private List<PermissionTreeVo> buildTreeByParent(Long parentId, Map<Long, List<Permission>> parentMap) {
        List<Permission> children = parentMap.getOrDefault(parentId, Collections.emptyList());
        children.sort(Comparator.comparing(Permission::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
        return children.stream().map(permission -> {
            PermissionTreeVo treeVo = copyProperties(permission, PermissionTreeVo.class);
            List<PermissionTreeVo> subTree = buildTreeByParent(permission.getId(), parentMap);
            if (!subTree.isEmpty()) {
                treeVo.setChildren(subTree);
            }
            return treeVo;
        }).toList();
    }

    private List<Option<Long>> buildPermissionOption(List<Permission> permissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            return Collections.emptyList();
        }
        Map<Long, List<Permission>> parentMap = permissions.stream()
                .collect(Collectors.groupingBy(permission -> Optional.ofNullable(permission.getParentId()).orElse(0L)));
        return buildOptionByParent(0L, parentMap);
    }

    private List<Option<Long>> buildOptionByParent(Long parentId, Map<Long, List<Permission>> parentMap) {
        List<Permission> children = parentMap.getOrDefault(parentId, Collections.emptyList());
        children.sort(Comparator.comparing(Permission::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
        return children.stream().map(permission -> {
            List<Option<Long>> subOptions = buildOptionByParent(permission.getId(), parentMap);
            if (subOptions.isEmpty()) {
                return new Option<>(permission.getId(), permission.getPermissionName());
            }
            return new Option<>(permission.getId(), permission.getPermissionName(), subOptions);
        }).toList();
    }
}
