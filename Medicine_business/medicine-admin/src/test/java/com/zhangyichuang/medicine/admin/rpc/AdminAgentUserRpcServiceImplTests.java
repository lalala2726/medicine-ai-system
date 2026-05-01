package com.zhangyichuang.medicine.admin.rpc;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zhangyichuang.medicine.admin.service.UserService;
import com.zhangyichuang.medicine.admin.service.UserWalletService;
import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.model.dto.UserContextDto;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.entity.UserWallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAgentUserRpcServiceImplTests {

    @Mock
    private UserService userService;

    @Mock
    private UserWalletService userWalletService;

    @InjectMocks
    private AdminAgentUserRpcServiceImpl rpcService;

    /**
     * 测试目的：验证用户 context 聚合会组合用户详情和钱包摘要。
     * 预期结果：返回 map key 为用户 ID，并正确生成风险标记和 AI 提示。
     */
    @SuppressWarnings("unchecked")
    @Test
    void getUserContextsByIds_ShouldBuildContext() {
        LambdaQueryChainWrapper<User> userQuery = mock(LambdaQueryChainWrapper.class);
        when(userService.lambdaQuery()).thenReturn(userQuery);
        when(userQuery.in(any(), anyCollection())).thenReturn(userQuery);
        when(userQuery.list()).thenReturn(List.of(createUser()));

        LambdaQueryChainWrapper<UserWallet> walletQuery = mock(LambdaQueryChainWrapper.class);
        when(userWalletService.lambdaQuery()).thenReturn(walletQuery);
        when(walletQuery.in(any(), anyCollection())).thenReturn(walletQuery);
        when(walletQuery.list()).thenReturn(List.of(createUserWallet()));

        Map<Long, UserContextDto> result = rpcService.getUserContextsByIds(List.of(1L));

        assertNotNull(result);
        assertTrue(result.containsKey(1L));
        UserContextDto context = result.get(1L);
        assertEquals("张三", context.getBasicSummary().getNickName());
        assertEquals(new BigDecimal("1000.00"), context.getWalletSummary().getBalance());
        assertNull(context.getOrderSummary());
        assertTrue(context.getRiskSummary().getAccountDisabled());
        assertTrue(context.getRiskSummary().getWalletFrozen());
        assertFalse(context.getRiskSummary().getMissingBasicProfile());
        assertFalse(context.getAiHints().getCanOperateWallet());
        verify(userService, never()).getUserById(anyLong());
        verify(userService, never()).getUserDetailById(anyLong());
        verify(userService, never()).getUserWallet(anyLong());
    }

    /**
     * 测试目的：验证用户 context 批量上限按参数异常处理。
     * 预期结果：超过 20 个用户 ID 时直接抛出 ParamException。
     */
    @Test
    void getUserContextsByIds_WhenOverLimit_ShouldThrowParamException() {
        List<Long> userIds = IntStream.rangeClosed(1, 21)
                .mapToObj(Long::valueOf)
                .toList();

        assertThrows(ParamException.class, () -> rpcService.getUserContextsByIds(userIds));
        verifyNoInteractions(userService, userWalletService);
    }

    /**
     * 功能描述：构造用户实体模拟数据，供用户存在性校验测试复用。
     *
     * @return 返回用户实体
     */
    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setNickname("张三");
        user.setPhoneNumber("13800000000");
        user.setStatus(1);
        user.setCreateTime(new Date());
        user.setLastLoginTime(new Date());
        return user;
    }

    /**
     * 功能描述：构造用户钱包模拟数据，供钱包摘要和风险标记测试复用。
     *
     * @return 返回用户钱包实体
     */
    private UserWallet createUserWallet() {
        return UserWallet.builder()
                .userId(1L)
                .balance(new BigDecimal("1000.00"))
                .status(1)
                .freezeReason("风控冻结")
                .freezeTime(new Date())
                .totalIncome(new BigDecimal("5000.00"))
                .totalExpend(new BigDecimal("4000.00"))
                .build();
    }
}
