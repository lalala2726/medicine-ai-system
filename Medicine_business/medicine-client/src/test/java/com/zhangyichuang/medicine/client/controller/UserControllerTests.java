package com.zhangyichuang.medicine.client.controller;

import com.zhangyichuang.medicine.client.model.dto.UserProfileDto;
import com.zhangyichuang.medicine.client.service.UserService;
import com.zhangyichuang.medicine.client.service.UserWalletService;
import com.zhangyichuang.medicine.common.security.entity.AuthUser;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import com.zhangyichuang.medicine.model.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTests {

    @Mock
    private UserService userService;

    @Mock
    private UserWalletService userWalletService;

    @InjectMocks
    private UserController userController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证个人资料查询接口会返回用户服务提供的资料对象。
     */
    @Test
    void getUserProfile_ShouldReturnProfile() {
        UserProfileDto profile = new UserProfileDto();
        profile.setNickname("测试用户");
        when(userService.getUserProfile()).thenReturn(profile);

        var result = userController.getUserProfile();

        assertEquals(200, result.getCode());
        assertEquals("测试用户", result.getData().getNickname());
        verify(userService).getUserProfile();
    }

    /**
     * 验证“当前用户”接口会从认证上下文读取 userId，
     * 并正确组装用户基础信息与角色集合。
     */
    @Test
    void currentUser_ShouldReturnUserWithRoles() {
        User user = new User();
        user.setId(8L);
        user.setUsername("client-user");

        when(userService.getUserById(8L)).thenReturn(user);
        when(userService.getUserRolesByUserId(8L)).thenReturn(Set.of("user"));

        AuthUser authUser = AuthUser.builder()
                .id(8L)
                .username("client-user")
                .roles(Set.of("ROLE_user"))
                .build();
        SysUserDetails userDetails = new SysUserDetails(authUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        var result = userController.currentUser();

        assertEquals(200, result.getCode());
        assertEquals("client-user", result.getData().getUsername());
        assertEquals(Set.of("user"), result.getData().getRoles());

        verify(userService).getUserById(8L);
        verify(userService).getUserRolesByUserId(8L);
    }
}
