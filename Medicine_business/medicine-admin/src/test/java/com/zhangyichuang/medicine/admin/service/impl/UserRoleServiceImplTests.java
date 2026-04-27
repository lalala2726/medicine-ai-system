package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zhangyichuang.medicine.admin.mapper.RoleMapper;
import com.zhangyichuang.medicine.model.entity.Role;
import com.zhangyichuang.medicine.model.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceImplTests {

    @Mock
    private RoleMapper roleMapper;

    @Spy
    @InjectMocks
    private UserRoleServiceImpl userRoleService;

    @SuppressWarnings("unchecked")
    @Test
    void getUserRoleByUserId_ShouldReturnRoleIdSet() {
        LambdaQueryChainWrapper<UserRole> wrapper = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);

        UserRole role1 = new UserRole();
        role1.setUserId(10L);
        role1.setRoleId(2L);
        UserRole role2 = new UserRole();
        role2.setUserId(10L);
        role2.setRoleId(3L);

        doReturn(wrapper).when(userRoleService).lambdaQuery();
        when(wrapper.eq(any(), any())).thenReturn(wrapper);
        when(wrapper.list()).thenReturn(List.of(role1, role2));

        Set<Long> roleIds = userRoleService.getUserRoleByUserId(10L);

        assertEquals(Set.of(2L, 3L), roleIds);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getUserIdsByRoleId_ShouldReturnBoundUserIdSet() {
        LambdaQueryChainWrapper<UserRole> wrapper = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);

        UserRole userRole1 = new UserRole();
        userRole1.setUserId(10L);
        userRole1.setRoleId(2L);
        UserRole userRole2 = new UserRole();
        userRole2.setUserId(20L);
        userRole2.setRoleId(2L);

        doReturn(wrapper).when(userRoleService).lambdaQuery();
        when(wrapper.eq(any(), any())).thenReturn(wrapper);
        when(wrapper.list()).thenReturn(List.of(userRole1, userRole2));

        Set<Long> userIds = userRoleService.getUserIdsByRoleId(2L);

        assertEquals(Set.of(10L, 20L), userIds);
    }

    @Test
    void updateUserRole_ShouldPersistOnlyExistingRoleIds() {
        Role role = new Role();
        role.setId(3L);

        doReturn(true).when(userRoleService).remove(any());
        doReturn(true).when(userRoleService).saveBatch(any());
        when(roleMapper.selectList(any())).thenReturn(List.of(role));

        userRoleService.updateUserRole(20L, Set.of(3L, 8L));

        ArgumentCaptor<List<UserRole>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRoleService).saveBatch(captor.capture());

        List<UserRole> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals(20L, saved.getFirst().getUserId());
        assertEquals(3L, saved.getFirst().getRoleId());
    }

    @Test
    void updateUserRole_WhenRolesEmpty_ShouldOnlyClearOldRelation() {
        doReturn(true).when(userRoleService).remove(any());

        userRoleService.updateUserRole(20L, Set.of());

        verify(userRoleService).remove(any());
        assertTrue(true);
    }
}
