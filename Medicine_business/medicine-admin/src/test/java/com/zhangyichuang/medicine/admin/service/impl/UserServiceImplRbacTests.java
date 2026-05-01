package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.mapper.UserMapper;
import com.zhangyichuang.medicine.admin.service.*;
import com.zhangyichuang.medicine.common.core.constants.RolesConstant;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.security.token.RedisTokenStore;
import com.zhangyichuang.medicine.model.dto.UserListDto;
import com.zhangyichuang.medicine.model.entity.Role;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.entity.UserRole;
import com.zhangyichuang.medicine.model.request.UserAddRequest;
import com.zhangyichuang.medicine.model.request.UserListQueryRequest;
import com.zhangyichuang.medicine.model.request.UserUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplRbacTests {

    @Mock
    private UserWalletLogService userWalletLogService;

    @Mock
    private MallOrderService mallOrderService;

    @Mock
    private UserWalletService userWalletService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private RedisTokenStore redisTokenStore;

    @Mock
    private UserMapper userMapper;

    @Spy
    @InjectMocks
    private UserServiceImpl userService;

    /**
     * 验证新增用户成功后会写入 user_role 关联并自动开通钱包。
     */
    @Test
    void addUser_ShouldWriteUserRoleAndOpenWallet() {
        UserAddRequest request = new UserAddRequest();
        request.setUsername("new-user");
        request.setPassword("123456");
        request.setRoles(Set.of(2L, 3L));

        doReturn("ENC-PWD").when(userService).encryptPassword("123456");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(88L);
            return true;
        }).when(userService).save(any(User.class));

        boolean result = userService.addUser(request);

        assertTrue(result);
        verify(roleService).isRoleExistById(Set.of(2L, 3L));
        verify(userRoleService).updateUserRole(88L, Set.of(2L, 3L));
        verify(userWalletService).openWallet(88L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(captor.capture());
        assertEquals("ENC-PWD", captor.getValue().getPassword());
    }

    /**
     * 验证更新用户且携带角色ID时，会同步更新 user_role 关联关系。
     */
    @Test
    void updateUser_WhenContainsRoles_ShouldUpdateUserRoleRelation() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setId(9L);
        request.setPassword("abc");
        request.setRoles(Set.of(2L));

        // 模拟用户存在，避免纯 Mockito 单测触发 MyBatis-Plus 真实 baseMapper 查询。
        doReturn(new User()).when(userService).getById(9L);
        // 模拟操作者不是被修改用户，覆盖角色变更权限校验所需的登录人ID。
        doReturn(1L).when(userService).getUserId();
        doReturn("ENC-ABC").when(userService).encryptPassword("abc");
        doReturn(true).when(userService).updateById(any(User.class));

        boolean updated = userService.updateUser(request);

        assertTrue(updated);
        verify(roleService).isRoleExistById(Set.of(2L));
        verify(userRoleService).updateUserRole(9L, Set.of(2L));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(captor.capture());
        assertEquals("ENC-ABC", captor.getValue().getPassword());
    }

    /**
     * 验证超级管理员账号删除保护：包含超级管理员ID时直接拒绝。
     */
    @Test
    void deleteUser_WhenContainsSuperAdmin_ShouldThrowException() {
        assertThrows(ServiceException.class,
                () -> userService.deleteUser(List.of(RolesConstant.SUPER_ADMIN_USER_ID)));
    }

    /**
     * 验证按用户ID查询角色时会委托 RBAC 角色服务返回角色码集合。
     */
    @Test
    void getUserRolesByUserId_ShouldDelegateToRoleService() {
        when(roleService.getUserRoleByUserId(100L)).thenReturn(Set.of("admin"));

        Set<String> roles = userService.getUserRolesByUserId(100L);

        assertEquals(Set.of("admin"), roles);
        verify(roleService).getUserRoleByUserId(100L);
    }

    /**
     * 验证用户列表会按当前页用户批量预取角色，避免逐用户查询角色名称。
     */
    @SuppressWarnings("unchecked")
    @Test
    void listUser_ShouldBatchLoadRoleNamesForCurrentPage() {
        UserListQueryRequest request = new UserListQueryRequest();
        Page<User> page = new Page<>(1, 10, 2);
        User alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        page.setRecords(List.of(alice, bob));
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        when(userMapper.listUser(any(Page.class), eq(request))).thenReturn(page);

        LambdaQueryChainWrapper<UserRole> userRoleQuery = mock(LambdaQueryChainWrapper.class);
        when(userRoleService.lambdaQuery()).thenReturn(userRoleQuery);
        when(userRoleQuery.in(any(), anyCollection())).thenReturn(userRoleQuery);
        when(userRoleQuery.list()).thenReturn(List.of(
                createUserRole(1L, 2L),
                createUserRole(1L, 3L),
                createUserRole(2L, 3L)
        ));

        LambdaQueryChainWrapper<Role> roleQuery = mock(LambdaQueryChainWrapper.class);
        when(roleService.lambdaQuery()).thenReturn(roleQuery);
        when(roleQuery.in(any(), anyCollection())).thenReturn(roleQuery);
        when(roleQuery.eq(any(), any())).thenReturn(roleQuery);
        when(roleQuery.list()).thenReturn(List.of(createRole(2L, "运营"), createRole(3L, "客服")));

        Page<UserListDto> result = userService.listUser(request);

        assertEquals("客服,运营", result.getRecords().get(0).getRoles());
        assertEquals("客服", result.getRecords().get(1).getRoles());
        verify(userRoleService, never()).getUserRoleByUserId(anyLong());
    }

    /**
     * 构造用户角色关联测试数据。
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 用户角色关联实体
     */
    private UserRole createUserRole(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }

    /**
     * 构造角色测试数据。
     *
     * @param id       角色ID
     * @param roleName 角色名称
     * @return 角色实体
     */
    private Role createRole(Long id, String roleName) {
        Role role = new Role();
        role.setId(id);
        role.setRoleName(roleName);
        return role;
    }
}
