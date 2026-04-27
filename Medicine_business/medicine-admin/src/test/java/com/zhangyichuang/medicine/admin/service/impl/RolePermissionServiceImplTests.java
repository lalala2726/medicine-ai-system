package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zhangyichuang.medicine.admin.mapper.PermissionMapper;
import com.zhangyichuang.medicine.admin.model.request.RolePermissionUpdateRequest;
import com.zhangyichuang.medicine.common.core.constants.RolesConstant;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.model.entity.Permission;
import com.zhangyichuang.medicine.model.entity.RolePermission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceImplTests {

    @Mock
    private PermissionMapper permissionMapper;

    @Spy
    @InjectMocks
    private RolePermissionServiceImpl rolePermissionService;

    @SuppressWarnings("unchecked")
    @Test
    void getRolePermission_ShouldDistinctPermissionIds() {
        LambdaQueryChainWrapper<RolePermission> wrapper = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        RolePermission rp1 = new RolePermission();
        rp1.setPermissionId(10L);
        RolePermission rp2 = new RolePermission();
        rp2.setPermissionId(10L);
        RolePermission rp3 = new RolePermission();
        rp3.setPermissionId(20L);

        doReturn(wrapper).when(rolePermissionService).lambdaQuery();
        doReturn(wrapper).when(wrapper).in(any(), anyCollection());
        when(wrapper.list()).thenReturn(List.of(rp1, rp2, rp3));

        List<Long> result = rolePermissionService.getRolePermission(List.of(1L, 2L));

        assertEquals(List.of(10L, 20L), result);
    }

    @Test
    void updateRolePermission_ShouldFilterNonExistingPermissionIds() {
        RolePermissionUpdateRequest request = new RolePermissionUpdateRequest();
        request.setRoleId(2L);
        request.setPermissionIds(List.of(11L, 22L));

        Permission permission = new Permission();
        permission.setId(22L);

        doReturn(true).when(rolePermissionService).remove(any());
        when(permissionMapper.selectList(any())).thenReturn(List.of(permission));
        doReturn(true).when(rolePermissionService).saveBatch(any());

        boolean updated = rolePermissionService.updateRolePermission(request);

        assertTrue(updated);
        ArgumentCaptor<List<RolePermission>> captor = ArgumentCaptor.forClass(List.class);
        verify(rolePermissionService).saveBatch(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(2L, captor.getValue().getFirst().getRoleId());
        assertEquals(22L, captor.getValue().getFirst().getPermissionId());
    }

    @Test
    void updateRolePermission_WhenPermissionIdsEmpty_ShouldOnlyClearRelation() {
        RolePermissionUpdateRequest request = new RolePermissionUpdateRequest();
        request.setRoleId(2L);
        request.setPermissionIds(List.of());
        doReturn(true).when(rolePermissionService).remove(any());

        boolean updated = rolePermissionService.updateRolePermission(request);

        assertTrue(updated);
        verify(rolePermissionService).remove(any());
    }

    @Test
    void updateRolePermission_WhenSuperAdminRole_ShouldThrowException() {
        RolePermissionUpdateRequest request = new RolePermissionUpdateRequest();
        request.setRoleId(RolesConstant.SUPER_ADMIN_ROLE_ID);
        request.setPermissionIds(List.of(1L, 2L));

        assertThrows(ServiceException.class, () -> rolePermissionService.updateRolePermission(request));
    }

    @Test
    void getAllPermissionIds_ShouldReturnAllPermissionIds() {
        Permission p1 = new Permission();
        p1.setId(101L);
        Permission p2 = new Permission();
        p2.setId(202L);
        when(permissionMapper.selectList(any())).thenReturn(List.of(p1, p2));

        List<Long> permissionIds = rolePermissionService.getAllPermissionIds();

        assertEquals(List.of(101L, 202L), permissionIds);
    }
}
