package com.zhangyichuang.medicine.admin.service.impl;

import com.zhangyichuang.medicine.admin.mapper.RolePermissionMapper;
import com.zhangyichuang.medicine.admin.model.request.PermissionAddRequest;
import com.zhangyichuang.medicine.admin.service.RolePermissionService;
import com.zhangyichuang.medicine.admin.service.RoleService;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.model.entity.Permission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTests {

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @Mock
    private RoleService roleService;

    @Mock
    private RolePermissionService rolePermissionService;

    @Spy
    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Test
    void addPermission_WhenParentPrefixMismatch_ShouldThrowException() {
        Permission parent = new Permission();
        parent.setId(1L);
        parent.setPermissionCode("system:user");
        doReturn(parent).when(permissionService).getById(1L);

        PermissionAddRequest request = new PermissionAddRequest();
        request.setParentId(1L);
        request.setPermissionCode("system:role:list");
        request.setPermissionName("角色列表");

        assertThrows(ServiceException.class, () -> permissionService.addPermission(request));
    }

    @Test
    void addPermission_WhenParentPrefixMatch_ShouldSave() {
        Permission parent = new Permission();
        parent.setId(1L);
        parent.setPermissionCode("system:user");
        doReturn(parent).when(permissionService).getById(1L);
        doReturn(true).when(permissionService).save(any(Permission.class));

        PermissionAddRequest request = new PermissionAddRequest();
        request.setParentId(1L);
        request.setPermissionCode("system:user:add");
        request.setPermissionName("用户新增");

        boolean result = permissionService.addPermission(request);

        assertTrue(result);
    }

    @Test
    void getPermissionCodesByUserId_WhenUserHasNoRole_ShouldReturnEmptySet() {
        when(roleService.getUserRoleByUserId(100L)).thenReturn(Set.of());
        when(roleService.getRoleIdByUserId(100L)).thenReturn(Set.of());

        Set<String> permissions = permissionService.getPermissionCodesByUserId(100L);

        assertEquals(Set.of(), permissions);
    }
}
