package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.RoleListRequest;
import com.zhangyichuang.medicine.admin.model.request.RolePermissionUpdateRequest;
import com.zhangyichuang.medicine.admin.service.PermissionService;
import com.zhangyichuang.medicine.admin.service.RoleService;
import com.zhangyichuang.medicine.common.core.base.Option;
import com.zhangyichuang.medicine.model.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleControllerTests {

    @Mock
    private RoleService roleService;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private RoleController roleController;

    @Test
    void roleList_ShouldReturnPagedResult() {
        RoleListRequest request = new RoleListRequest();
        Page<Role> page = new Page<>(1, 10, 1);
        Role role = new Role();
        role.setId(1L);
        role.setRoleCode("admin");
        role.setRoleName("管理员");
        page.setRecords(List.of(role));
        when(roleService.roleList(request)).thenReturn(page);

        var result = roleController.roleList(request);

        assertEquals(200, result.getCode());
        verify(roleService).roleList(request);
    }

    @Test
    void getRolePermission_ShouldReturnRolePermissionVo() {
        when(roleService.getRolePermission(1L)).thenReturn(List.of(10L, 20L));
        when(permissionService.permissionOption()).thenReturn(List.of(new Option<>(10L, "系统用户列表")));

        var result = roleController.getRolePermission(1L);

        assertEquals(200, result.getCode());
        assertEquals(List.of(10L, 20L), result.getData().getRolePermission());
        assertEquals(1, result.getData().getPermissionOption().size());
        verify(roleService).getRolePermission(1L);
        verify(permissionService).permissionOption();
    }

    @Test
    void updateRolePermission_ShouldDelegateToService() {
        RolePermissionUpdateRequest request = new RolePermissionUpdateRequest();
        request.setRoleId(2L);
        request.setPermissionIds(List.of(1L, 2L));
        when(roleService.updateRolePermission(request)).thenReturn(true);

        var result = roleController.updateRolePermission(request);

        assertEquals(200, result.getCode());
        verify(roleService).updateRolePermission(request);
    }
}
