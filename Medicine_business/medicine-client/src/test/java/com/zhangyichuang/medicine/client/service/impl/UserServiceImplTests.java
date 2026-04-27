package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zhangyichuang.medicine.client.mapper.UserMapper;
import com.zhangyichuang.medicine.client.service.MallOrderService;
import com.zhangyichuang.medicine.client.service.UserCouponService;
import com.zhangyichuang.medicine.client.service.UserWalletService;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTests {

    /**
     * 用户钱包服务。
     */
    @Mock
    private UserWalletService userWalletService;

    /**
     * 订单服务。
     */
    @Mock
    private MallOrderService mallOrderService;

    /**
     * 用户优惠券服务。
     */
    @Mock
    private UserCouponService userCouponService;

    /**
     * 用户 Mapper。
     */
    @Mock
    private UserMapper userMapper;

    /**
     * 被测服务。
     */
    @Spy
    @InjectMocks
    private UserServiceImpl userService;

    /**
     * 初始化测试上下文。
     */
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    /**
     * 验证按用户ID查询角色时会过滤空白角色编码。
     */
    @Test
    void getUserRolesByUserId_ShouldFilterBlankAndNullCodes() {
        when(userMapper.listRoleCodesByUserId(1L)).thenReturn(Arrays.asList("admin", " ", null, "user"));

        Set<String> roles = userService.getUserRolesByUserId(1L);

        assertEquals(Set.of("admin", "user"), roles);
        verify(userMapper).listRoleCodesByUserId(1L);
    }

    /**
     * 验证用户名不存在时返回空角色集合。
     */
    @Test
    void getUserRolesByUserName_WhenUserNotExist_ShouldReturnEmptySet() {
        doReturn(null).when(userService).getUserByUsername("ghost");

        Set<String> roles = userService.getUserRolesByUserName("ghost");

        assertTrue(roles.isEmpty());
    }

    /**
     * 验证按用户名查询角色时会委托到用户ID查询。
     */
    @Test
    void getUserRolesByUserName_ShouldDelegateToUserIdQuery() {
        User user = new User();
        user.setId(10L);
        doReturn(user).when(userService).getUserByUsername("alice");
        when(userMapper.listRoleCodesByUserId(10L)).thenReturn(List.of("user"));

        Set<String> roles = userService.getUserRolesByUserName("alice");

        assertEquals(Set.of("user"), roles);
        verify(userMapper).listRoleCodesByUserId(10L);
    }

    /**
     * 验证个人中心摘要会使用客户端展示口径统计优惠券数量。
     */
    @Test
    @SuppressWarnings("unchecked")
    void getUserBriefInfo_ShouldUseDisplayableCouponCount() {
        Long userId = 99L;
        User user = new User();
        user.setId(userId);
        user.setNickname("测试用户");
        user.setAvatar("avatar");
        user.setPhoneNumber("13800000000");
        doReturn(userId).when(userService).getUserId();
        doReturn(user).when(userService).getUserById(userId);
        when(userWalletService.getUserWalletBalance()).thenReturn(new BigDecimal("88.00"));
        when(userCouponService.countDisplayableCoupons(userId)).thenReturn(3L);
        LambdaQueryChainWrapper<MallOrder> mallOrderQueryWrapper = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        when(mallOrderService.lambdaQuery()).thenReturn(mallOrderQueryWrapper);
        when(mallOrderQueryWrapper.eq(any(SFunction.class), eq(userId))).thenReturn(mallOrderQueryWrapper);
        when(mallOrderQueryWrapper.list()).thenReturn(List.of());

        var result = userService.getUserBriefInfo();

        assertEquals(3, result.getCouponCount());
        verify(userCouponService).countDisplayableCoupons(userId);
    }
}
