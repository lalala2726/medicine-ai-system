package com.zhangyichuang.medicine.admin.rpc;

import com.zhangyichuang.medicine.admin.service.UserService;
import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.model.dto.UserContextDto;
import com.zhangyichuang.medicine.model.dto.UserDetailDto;
import com.zhangyichuang.medicine.model.dto.UserWalletDto;
import com.zhangyichuang.medicine.model.entity.User;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAgentUserRpcServiceImplTests {

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminAgentUserRpcServiceImpl rpcService;

    /**
     * 测试目的：验证用户 context 聚合会组合用户详情和钱包摘要。
     * 预期结果：返回 map key 为用户 ID，并正确生成风险标记和 AI 提示。
     */
    @Test
    void getUserContextsByIds_ShouldBuildContext() {
        when(userService.getUserById(1L)).thenReturn(createUser());
        when(userService.getUserDetailById(1L)).thenReturn(createUserDetail());
        when(userService.getUserWallet(1L)).thenReturn(createUserWallet());

        Map<Long, UserContextDto> result = rpcService.getUserContextsByIds(List.of(1L));

        assertNotNull(result);
        assertTrue(result.containsKey(1L));
        UserContextDto context = result.get(1L);
        assertEquals("张三", context.getBasicSummary().getNickName());
        assertEquals(new BigDecimal("1000.00"), context.getWalletSummary().getBalance());
        assertTrue(context.getRiskSummary().getAccountDisabled());
        assertTrue(context.getRiskSummary().getWalletFrozen());
        assertFalse(context.getRiskSummary().getMissingBasicProfile());
        assertFalse(context.getAiHints().getCanOperateWallet());
        verify(userService).getUserById(1L);
        verify(userService).getUserDetailById(1L);
        verify(userService).getUserWallet(1L);
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
        verifyNoInteractions(userService);
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
        user.setStatus(1);
        return user;
    }

    /**
     * 功能描述：构造用户详情模拟数据，供 context 聚合测试复用。
     *
     * @return 返回用户详情 DTO
     */
    private UserDetailDto createUserDetail() {
        UserDetailDto detail = new UserDetailDto();
        detail.setNickName("张三");
        detail.setTotalOrders(10);
        detail.setTotalConsume(new BigDecimal("5000.00"));

        UserDetailDto.BasicInfo basicInfo = new UserDetailDto.BasicInfo();
        basicInfo.setUserId(1L);
        basicInfo.setPhoneNumber("13800000000");
        detail.setBasicInfo(basicInfo);

        UserDetailDto.SecurityInfo securityInfo = new UserDetailDto.SecurityInfo();
        securityInfo.setStatus(1);
        securityInfo.setRegisterTime(new Date());
        securityInfo.setLastLoginTime(new Date());
        detail.setSecurityInfo(securityInfo);
        return detail;
    }

    /**
     * 功能描述：构造用户钱包模拟数据，供钱包摘要和风险标记测试复用。
     *
     * @return 返回用户钱包 DTO
     */
    private UserWalletDto createUserWallet() {
        UserWalletDto wallet = new UserWalletDto();
        wallet.setUserId(1L);
        wallet.setBalance(new BigDecimal("1000.00"));
        wallet.setStatus(1);
        wallet.setFreezeReason("风控冻结");
        wallet.setFreezeTime(new Date());
        wallet.setTotalIncome(new BigDecimal("5000.00"));
        wallet.setTotalExpend(new BigDecimal("4000.00"));
        return wallet;
    }
}
