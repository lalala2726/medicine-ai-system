package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.admin.model.request.PermissionAddRequest;
import com.zhangyichuang.medicine.admin.model.vo.PermissionTreeVo;
import com.zhangyichuang.medicine.admin.service.PermissionService;
import com.zhangyichuang.medicine.common.core.base.Option;
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
class PermissionControllerTests {

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private PermissionController permissionController;

    @Test
    void permissionTree_ShouldReturnTreeData() {
        PermissionTreeVo root = new PermissionTreeVo();
        root.setId(1L);
        root.setPermissionCode("system:user:list");
        root.setPermissionName("用户列表");
        when(permissionService.permissionTree()).thenReturn(List.of(root));

        var result = permissionController.permissionTree();

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        verify(permissionService).permissionTree();
    }

    @Test
    void addPermission_ShouldDelegateToService() {
        PermissionAddRequest request = new PermissionAddRequest();
        request.setPermissionCode("system:user:add");
        request.setPermissionName("用户新增");
        when(permissionService.addPermission(request)).thenReturn(true);

        var result = permissionController.addPermission(request);

        assertEquals(200, result.getCode());
        verify(permissionService).addPermission(request);
    }

    @Test
    void permissionOption_ShouldReturnOptionTree() {
        when(permissionService.permissionOption()).thenReturn(List.of(new Option<>(1L, "系统权限")));

        var result = permissionController.permissionOption();

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        verify(permissionService).permissionOption();
    }
}
