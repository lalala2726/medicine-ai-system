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
}
