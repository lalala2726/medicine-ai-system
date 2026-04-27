package com.zhangyichuang.medicine.admin.service.impl;

import com.zhangyichuang.medicine.admin.mapper.RoleMapper;
import com.zhangyichuang.medicine.admin.model.request.RolePermissionUpdateRequest;
import com.zhangyichuang.medicine.admin.model.request.RoleUpdateRequest;
import com.zhangyichuang.medicine.admin.service.RolePermissionService;
import com.zhangyichuang.medicine.admin.service.UserRoleService;
import com.zhangyichuang.medicine.common.core.constants.RolesConstant;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.security.token.RedisTokenStore;
import com.zhangyichuang.medicine.model.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTests {

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private RolePermissionService rolePermissionService;

    @Mock
    private RedisTokenStore redisTokenStore;

    @Spy
    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void deleteRoleByIds_WhenContainsSuperAdmin_ShouldThrowException() {
        assertThrows(ServiceException.class,
                () -> roleService.deleteRoleByIds(List.of(RolesConstant.SUPER_ADMIN_ROLE_ID, 2L)));
    }

    @Test
    void getUserRoleByUserId_WhenNoRoleRelation_ShouldReturnEmptySet() {
        when(userRoleService.getUserRoleByUserId(100L)).thenReturn(Set.of());

        Set<String> roleCodes = roleService.getUserRoleByUserId(100L);

        assertEquals(Set.of(), roleCodes);
    }

    @Test
    void getRoleIdByUserId_WhenNoRoleRelation_ShouldReturnEmptySet() {
        when(userRoleService.getUserRoleByUserId(100L)).thenReturn(Set.of());

        Set<Long> roleIds = roleService.getRoleIdByUserId(100L);

        assertEquals(Set.of(), roleIds);
    }

    @Test
    void updateRoleById_WhenSuperAdminRole_ShouldThrowException() {
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setId(RolesConstant.SUPER_ADMIN_ROLE_ID);

        assertThrows(ServiceException.class, () -> roleService.updateRoleById(request));
    }

    @Test
    void updateRolePermission_WhenSuperAdminRole_ShouldThrowException() {
        RolePermissionUpdateRequest request = new RolePermissionUpdateRequest();
        request.setRoleId(RolesConstant.SUPER_ADMIN_ROLE_ID);

        assertThrows(ServiceException.class, () -> roleService.updateRolePermission(request));
    }

    @Test
    void updateRolePermission_WhenUpdated_ShouldDeleteBoundUserSessions() {
        RolePermissionUpdateRequest request = new RolePermissionUpdateRequest();
        request.setRoleId(2L);
        when(rolePermissionService.updateRolePermission(request)).thenReturn(true);
        when(userRoleService.getUserIdsByRoleId(2L)).thenReturn(Set.of(10L, 20L));

        boolean updated = roleService.updateRolePermission(request);

        assertEquals(true, updated);
        verify(userRoleService).getUserIdsByRoleId(2L);
        verify(redisTokenStore).deleteSessionsByUserIds(Set.of(10L, 20L));
    }

    @Test
    void updateRolePermission_WhenNotUpdated_ShouldNotDeleteBoundUserSessions() {
        RolePermissionUpdateRequest request = new RolePermissionUpdateRequest();
        request.setRoleId(2L);
        when(rolePermissionService.updateRolePermission(request)).thenReturn(false);

        boolean updated = roleService.updateRolePermission(request);

        assertEquals(false, updated);
        verify(userRoleService, never()).getUserIdsByRoleId(2L);
        verify(redisTokenStore, never()).deleteSessionsByUserIds(any());
    }

    @Test
    void getRolePermission_WhenSuperAdminRole_ShouldReturnAllPermissionIds() {
        Role role = new Role();
        role.setId(RolesConstant.SUPER_ADMIN_ROLE_ID);
        role.setRoleCode(RolesConstant.SUPER_ADMIN);
        doReturn(role).when(roleService).getById(RolesConstant.SUPER_ADMIN_ROLE_ID);
        when(rolePermissionService.getAllPermissionIds()).thenReturn(List.of(11L, 22L, 33L));

        List<Long> permissionIds = roleService.getRolePermission(RolesConstant.SUPER_ADMIN_ROLE_ID);

        assertEquals(List.of(11L, 22L, 33L), permissionIds);
        verify(rolePermissionService).getAllPermissionIds();
    }
}
