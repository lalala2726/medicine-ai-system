package com.zhangyichuang.medicine.admin.service.impl;

import com.zhangyichuang.medicine.admin.publisher.LoginLogPublisher;
import com.zhangyichuang.medicine.admin.service.PermissionService;
import com.zhangyichuang.medicine.admin.service.UserService;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.common.core.constants.RolesConstant;
import com.zhangyichuang.medicine.common.core.exception.LoginException;
import com.zhangyichuang.medicine.common.security.entity.AuthTokenVo;
import com.zhangyichuang.medicine.common.security.login.LoginFailureResult;
import com.zhangyichuang.medicine.common.security.login.LoginLockStatus;
import com.zhangyichuang.medicine.common.security.login.LoginSecurityService;
import com.zhangyichuang.medicine.common.security.token.JwtTokenProvider;
import com.zhangyichuang.medicine.common.security.token.RedisTokenStore;
import com.zhangyichuang.medicine.common.security.token.TokenService;
import com.zhangyichuang.medicine.model.dto.LoginSessionDTO;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.mq.LoginLogMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTests {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private RedisTokenStore redisTokenStore;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserService userService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private LoginLogPublisher loginLogPublisher;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private LoginSecurityService loginSecurityService;

    @Spy
    @InjectMocks
    private AuthServiceImpl authService;

    /**
     * 初始化登录安全服务默认桩行为，避免与当前测试目标无关的安全策略影响断言。
     */
    @BeforeEach
    void setUp() {
        lenient().when(loginSecurityService.checkLockStatus(anyString(), anyString()))
                .thenReturn(new LoginLockStatus(false, 0L));
        lenient().when(loginSecurityService.recordLoginFailure(anyString(), anyString()))
                .thenReturn(new LoginFailureResult(false, 1, 0L));
        lenient().doNothing().when(loginSecurityService).clearLoginState(anyString(), anyString());
        lenient().when(loginSecurityService.buildLockMessage(anyLong())).thenReturn("账号已锁定，请1分钟后重试");
    }

    /**
     * 验证具备 admin 角色的用户可以登录成功，
     * 并且会发布 admin 来源的登录成功日志。
     */
    @Test
    void login_WhenHasAdminRole_ShouldReturnToken() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                Set.of(new SimpleGrantedAuthority("ROLE_" + RolesConstant.ADMIN))
        );
        LoginSessionDTO loginSessionDTO = LoginSessionDTO.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(tokenService.createToken(authentication)).thenReturn(loginSessionDTO);

        AuthTokenVo token = authService.login("admin", "123456", "captcha-verification-id");

        assertEquals("access", token.getAccessToken());
        assertEquals("refresh", token.getRefreshToken());
        verify(tokenService).createToken(authentication);

        ArgumentCaptor<LoginLogMessage> captor = ArgumentCaptor.forClass(LoginLogMessage.class);
        verify(loginLogPublisher).publish(captor.capture());
        LoginLogMessage message = captor.getValue();
        assertEquals("admin", message.getLoginSource());
        assertEquals(1, message.getLoginStatus());
        assertEquals("password", message.getLoginType());
    }

    /**
     * 验证非管理员角色登录 admin 端会被拒绝，
     * 同时仍会发布登录失败日志用于审计。
     */
    @Test
    void login_WhenNoAdminRole_ShouldThrowLoginException() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user",
                null,
                Set.of(new SimpleGrantedAuthority("ROLE_user"))
        );
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        assertThrows(LoginException.class, () -> authService.login("user", "123456", "captcha-verification-id"));

        ArgumentCaptor<LoginLogMessage> captor = ArgumentCaptor.forClass(LoginLogMessage.class);
        verify(loginLogPublisher).publish(captor.capture());
        LoginLogMessage message = captor.getValue();
        assertEquals("admin", message.getLoginSource());
        assertEquals(0, message.getLoginStatus());
    }

    /**
     * 验证当前用户信息接口会聚合用户基础信息与角色集合返回。
     */
    @Test
    void currentUserInfo_ShouldReturnUserWithRoles() {
        User user = new User();
        user.setId(10L);
        user.setUsername("admin");

        doReturn(10L).when(authService).getUserId();
        when(userService.getUserById(10L)).thenReturn(user);
        when(userService.getUserRolesByUserId(10L)).thenReturn(Set.of("admin", "super_admin"));

        var vo = authService.currentUserInfo();

        assertEquals(10L, vo.getId());
        assertEquals(Set.of("admin", "super_admin"), vo.getRoles());
        verify(userService).getUserById(10L);
        verify(userService).getUserRolesByUserId(10L);
    }

    /**
     * 验证当前用户权限查询接口会委托权限服务按用户 ID 获取权限码。
     */
    @Test
    void currentUserPermissions_ShouldDelegateToPermissionService() {
        doReturn(7L).when(authService).getUserId();
        when(permissionService.getPermissionCodesByUserId(7L)).thenReturn(Set.of("system:user:list"));

        var permissions = authService.currentUserPermissions();

        assertEquals(Set.of("system:user:list"), permissions);
        verify(permissionService).getPermissionCodesByUserId(7L);
    }
}
