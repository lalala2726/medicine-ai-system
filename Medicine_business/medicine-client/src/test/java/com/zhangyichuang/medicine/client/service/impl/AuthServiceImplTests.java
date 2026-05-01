package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zhangyichuang.medicine.client.mapper.UserMapper;
import com.zhangyichuang.medicine.client.publisher.LoginLogPublisher;
import com.zhangyichuang.medicine.client.service.UserService;
import com.zhangyichuang.medicine.client.task.AsyncUserLogService;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.common.core.constants.RolesConstant;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
    private UserService userService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private RedisTokenStore redisTokenStore;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AsyncUserLogService asyncUserLogService;

    @Mock
    private UserMapper userMapper;

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
     * 初始化登录安全服务默认桩行为，避免安全策略分支干扰当前测试目标。
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
     * 验证注册时会为新用户绑定默认 user 角色，
     * 并通过 user_role 关系表完成角色归属。
     */
    @SuppressWarnings("unchecked")
    @Test
    void register_ShouldInsertDefaultUserRole() {
        LambdaQueryChainWrapper<User> query = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        when(userService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), eq("alice"))).thenReturn(query);
        when(query.one()).thenReturn(null);

        when(passwordEncoder.encode("123456")).thenReturn("ENC");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(66L);
            return true;
        }).when(userService).save(any(User.class));

        when(userMapper.selectRoleIdByRoleCode(RolesConstant.USER)).thenReturn(3L);

        Long userId = authService.register("alice", "123456");

        assertEquals(66L, userId);
        verify(userMapper).insertUserRole(66L, 3L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        assertEquals("alice", userCaptor.getValue().getUsername());
        assertEquals("ENC", userCaptor.getValue().getPassword());
    }

    /**
     * 验证默认角色缺失时注册会失败并抛出业务异常，
     * 防止创建没有角色关联的无权限用户。
     */
    @SuppressWarnings("unchecked")
    @Test
    void register_WhenDefaultRoleMissing_ShouldThrowException() {
        LambdaQueryChainWrapper<User> query = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        when(userService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), eq("bob"))).thenReturn(query);
        when(query.one()).thenReturn(null);

        when(passwordEncoder.encode("123456")).thenReturn("ENC");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(77L);
            return true;
        }).when(userService).save(any(User.class));

        when(userMapper.selectRoleIdByRoleCode(RolesConstant.USER)).thenReturn(null);

        assertThrows(ServiceException.class, () -> authService.register("bob", "123456"));
    }

    /**
     * 验证客户端登录成功后会发布 client 来源的登录日志，
     * 且保留异步更新最近登录信息的行为。
     */
    @Test
    void login_ShouldPublishClientLoginLog() {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(9L);
        userDetails.setUsername("alice");
        userDetails.setAuthorities(Set.of(new SimpleGrantedAuthority("ROLE_user")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        LoginSessionDTO session = LoginSessionDTO.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(tokenService.createToken(authentication)).thenReturn(session);

        var result = authService.login("alice", "123456", "captcha-verification-id");

        assertEquals("access", result.getAccessToken());
        assertEquals("refresh", result.getRefreshToken());
        verify(asyncUserLogService).recordUserLoginLog(eq(9L), nullable(String.class));
        ArgumentCaptor<LoginLogMessage> captor = ArgumentCaptor.forClass(LoginLogMessage.class);
        verify(loginLogPublisher).publish(captor.capture());
        LoginLogMessage message = captor.getValue();
        assertEquals("client", message.getLoginSource());
        assertEquals(1, message.getLoginStatus());
        assertEquals("password", message.getLoginType());
    }

    /**
     * 验证客户端修改密码成功时会更新加密密码并清理当前用户全部会话。
     */
    @Test
    void changePassword_WhenValid_ShouldUpdatePasswordAndDeleteUserSessions() {
        User user = new User();
        user.setId(10L);
        user.setPassword("encoded-old-password");

        doReturn(10L).when(authService).getUserId();
        when(userService.getUserById(10L)).thenReturn(user);
        when(passwordEncoder.matches("oldPassword123", "encoded-old-password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-new-password");
        when(userService.updateById(any(User.class))).thenReturn(true);

        authService.changePassword(" oldPassword123 ", " newPassword123 ", " captcha-verification-id ");

        verify(captchaService).validateLoginCaptcha("captcha-verification-id");
        verify(passwordEncoder).matches("oldPassword123", "encoded-old-password");
        verify(passwordEncoder).encode("newPassword123");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(captor.capture());
        assertEquals(10L, captor.getValue().getId());
        assertEquals("encoded-new-password", captor.getValue().getPassword());
        verify(redisTokenStore).deleteSessionsByUserIds(Set.of(10L));
    }

    /**
     * 验证客户端原密码错误时拒绝修改密码且不清理会话。
     */
    @Test
    void changePassword_WhenOldPasswordIncorrect_ShouldThrowServiceException() {
        User user = new User();
        user.setId(10L);
        user.setPassword("encoded-old-password");

        doReturn(10L).when(authService).getUserId();
        when(userService.getUserById(10L)).thenReturn(user);
        when(passwordEncoder.matches("wrongPassword", "encoded-old-password")).thenReturn(false);

        ServiceException exception = assertThrows(
                ServiceException.class,
                () -> authService.changePassword("wrongPassword", "newPassword123", "captcha-verification-id")
        );

        assertEquals("原密码错误", exception.getMessage());
        verify(userService, never()).updateById(any(User.class));
        verify(redisTokenStore, never()).deleteSessionsByUserIds(any());
    }

    /**
     * 验证客户端新旧密码相同时拒绝修改密码且不清理会话。
     */
    @Test
    void changePassword_WhenSamePassword_ShouldThrowServiceException() {
        User user = new User();
        user.setId(10L);
        user.setPassword("encoded-old-password");

        doReturn(10L).when(authService).getUserId();
        when(userService.getUserById(10L)).thenReturn(user);
        when(passwordEncoder.matches("samePassword", "encoded-old-password")).thenReturn(true);

        ServiceException exception = assertThrows(
                ServiceException.class,
                () -> authService.changePassword("samePassword", "samePassword", "captcha-verification-id")
        );

        assertEquals("新密码不能与原密码相同", exception.getMessage());
        verify(userService, never()).updateById(any(User.class));
        verify(redisTokenStore, never()).deleteSessionsByUserIds(any());
    }

    /**
     * 验证客户端当前用户不存在时拒绝修改密码且不清理会话。
     */
    @Test
    void changePassword_WhenCurrentUserMissing_ShouldThrowServiceException() {
        doReturn(10L).when(authService).getUserId();
        when(userService.getUserById(10L)).thenReturn(null);

        ServiceException exception = assertThrows(
                ServiceException.class,
                () -> authService.changePassword("oldPassword123", "newPassword123", "captcha-verification-id")
        );

        assertEquals("当前用户不存在", exception.getMessage());
        verify(userService, never()).updateById(any(User.class));
        verify(redisTokenStore, never()).deleteSessionsByUserIds(any());
    }

    /**
     * 验证客户端密码更新失败时返回业务错误且不清理会话。
     */
    @Test
    void changePassword_WhenUpdateFailed_ShouldThrowServiceException() {
        User user = new User();
        user.setId(10L);
        user.setPassword("encoded-old-password");

        doReturn(10L).when(authService).getUserId();
        when(userService.getUserById(10L)).thenReturn(user);
        when(passwordEncoder.matches("oldPassword123", "encoded-old-password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-new-password");
        when(userService.updateById(any(User.class))).thenReturn(false);

        ServiceException exception = assertThrows(
                ServiceException.class,
                () -> authService.changePassword("oldPassword123", "newPassword123", "captcha-verification-id")
        );

        assertEquals("密码修改失败，请稍后重试", exception.getMessage());
        verify(redisTokenStore, never()).deleteSessionsByUserIds(any());
    }
}
