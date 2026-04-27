package com.zhangyichuang.medicine.client.service.impl;

import com.zhangyichuang.medicine.client.model.request.ActivationCodeRedeemRequest;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.model.coupon.ActivationRedeemCodeDto;
import com.zhangyichuang.medicine.model.coupon.CouponGrantResultDto;
import com.zhangyichuang.medicine.model.entity.CouponActivationLog;
import com.zhangyichuang.medicine.model.enums.ActivationCodeItemStatusEnum;
import com.zhangyichuang.medicine.model.enums.ActivationCodeStatusEnum;
import com.zhangyichuang.medicine.model.enums.ActivationCodeValidityTypeEnum;
import com.zhangyichuang.medicine.model.enums.ActivationRedeemRuleTypeEnum;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationBatchMapper;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationCodeMapper;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationGrantLogMapper;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationLogMapper;
import com.zhangyichuang.medicine.shared.service.CouponGrantCoreService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 客户端激活码服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class CouponActivationCodeServiceImplTests {

    /**
     * 基础激活码批次 Mapper。
     */
    @Mock
    private BasicCouponActivationBatchMapper activationBatchMapper;

    /**
     * 基础激活码 Mapper。
     */
    @Mock
    private BasicCouponActivationCodeMapper activationCodeMapper;

    /**
     * 基础激活码日志 Mapper。
     */
    @Mock
    private BasicCouponActivationLogMapper activationLogMapper;

    /**
     * 基础激活码发券日志 Mapper。
     */
    @Mock
    private BasicCouponActivationGrantLogMapper activationGrantLogMapper;

    /**
     * 优惠券共享发放核心服务。
     */
    @Mock
    private CouponGrantCoreService couponGrantCoreService;

    /**
     * 验证码服务。
     */
    @Mock
    private CaptchaService captchaService;

    /**
     * 事务模板。
     */
    @Mock
    private TransactionTemplate transactionTemplate;

    /**
     * 被测服务。
     */
    private CouponActivationCodeServiceImpl service;

    /**
     * 初始化测试上下文。
     */
    @BeforeEach
    void setUp() {
        service = spy(new CouponActivationCodeServiceImpl(
                activationBatchMapper,
                activationCodeMapper,
                activationLogMapper,
                activationGrantLogMapper,
                couponGrantCoreService,
                captchaService,
                transactionTemplate
        ));
        doReturn(88L).when(service).getUserId();
        lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("localhost");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    /**
     * 清理请求上下文。
     */
    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 验证非法激活码会写失败日志且不发券。
     */
    @Test
    void redeemCurrentUserCode_WhenCodeNotFound_ShouldWriteFailureLog() {
        when(activationCodeMapper.selectRedeemCodeByHash(any())).thenReturn(null);
        when(activationLogMapper.insert(any(CouponActivationLog.class))).thenReturn(1);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.redeemCurrentUserCode(buildRedeemRequest()));

        assertEquals("激活码不存在", exception.getMessage());
        verify(activationLogMapper).insert(any(CouponActivationLog.class));
        verify(couponGrantCoreService, never()).grantCoupon(any());
    }

    /**
     * 验证停用批次会阻断兑换并记录失败日志。
     */
    @Test
    void redeemCurrentUserCode_WhenBatchDisabled_ShouldWriteFailureLog() {
        when(activationCodeMapper.selectRedeemCodeByHash(any()))
                .thenReturn(buildRedeemCode(
                        ActivationRedeemRuleTypeEnum.UNIQUE_SINGLE_USE.getType(),
                        ActivationCodeValidityTypeEnum.ONCE.getType(),
                        ActivationCodeStatusEnum.DISABLED.getType(),
                        ActivationCodeItemStatusEnum.ACTIVE.getType()));
        when(activationLogMapper.insert(any(CouponActivationLog.class))).thenReturn(1);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.redeemCurrentUserCode(buildRedeemRequest()));

        assertEquals("激活码已停用", exception.getMessage());
        verify(activationLogMapper).insert(any(CouponActivationLog.class));
    }

    /**
     * 验证过期激活码会阻断兑换并记录失败日志。
     */
    @Test
    void redeemCurrentUserCode_WhenFixedCodeExpired_ShouldWriteFailureLog() {
        ActivationRedeemCodeDto redeemCode = buildRedeemCode(
                ActivationRedeemRuleTypeEnum.UNIQUE_SINGLE_USE.getType(),
                ActivationCodeValidityTypeEnum.ONCE.getType(),
                ActivationCodeStatusEnum.ACTIVE.getType(),
                ActivationCodeItemStatusEnum.ACTIVE.getType()
        );
        redeemCode.setFixedExpireTime(new Date(System.currentTimeMillis() - 1_000L));
        when(activationCodeMapper.selectRedeemCodeByHash(any())).thenReturn(redeemCode);
        when(activationLogMapper.insert(any(CouponActivationLog.class))).thenReturn(1);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.redeemCurrentUserCode(buildRedeemRequest()));

        assertEquals("激活码已过期", exception.getMessage());
        verify(activationLogMapper).insert(any(CouponActivationLog.class));
        verify(activationCodeMapper, never()).markUniqueCodeUsed(any(), any(), any());
    }

    /**
     * 验证唯一码首次兑换成功时会同步更新单码、批次和兑换日志。
     */
    @Test
    void redeemCurrentUserCode_WhenUniqueCodeFirstUsed_ShouldGrantCouponAndWriteLogs() {
        when(activationCodeMapper.selectRedeemCodeByHash(any()))
                .thenReturn(buildRedeemCode(
                        ActivationRedeemRuleTypeEnum.UNIQUE_SINGLE_USE.getType(),
                        ActivationCodeValidityTypeEnum.ONCE.getType(),
                        ActivationCodeStatusEnum.ACTIVE.getType(),
                        ActivationCodeItemStatusEnum.ACTIVE.getType()));
        when(activationCodeMapper.markUniqueCodeUsed(any(), any(), any())).thenReturn(1);
        when(activationBatchMapper.increaseSuccessUseCount(any(), any(), any())).thenReturn(1);
        when(couponGrantCoreService.grantCoupon(any())).thenReturn(buildGrantResult(7001L));
        when(activationLogMapper.insert(any(CouponActivationLog.class))).thenReturn(1);

        var result = service.redeemCurrentUserCode(buildRedeemRequest());

        assertEquals(7001L, result.getCouponId());
        verify(activationCodeMapper).markUniqueCodeUsed(any(), any(), any());
        verify(activationBatchMapper).increaseSuccessUseCount(any(), any(), any());
        verify(activationLogMapper, times(1)).insert(any(CouponActivationLog.class));
    }

    /**
     * 验证唯一码重复兑换会被拦截并记录失败日志。
     */
    @Test
    void redeemCurrentUserCode_WhenUniqueCodeRepeated_ShouldWriteFailureLog() {
        when(activationCodeMapper.selectRedeemCodeByHash(any()))
                .thenReturn(buildRedeemCode(
                        ActivationRedeemRuleTypeEnum.UNIQUE_SINGLE_USE.getType(),
                        ActivationCodeValidityTypeEnum.ONCE.getType(),
                        ActivationCodeStatusEnum.ACTIVE.getType(),
                        ActivationCodeItemStatusEnum.ACTIVE.getType()));
        when(activationCodeMapper.markUniqueCodeUsed(any(), any(), any())).thenReturn(0);
        when(activationLogMapper.insert(any(CouponActivationLog.class))).thenReturn(1);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.redeemCurrentUserCode(buildRedeemRequest()));

        assertEquals("激活码已被使用", exception.getMessage());
        verify(activationLogMapper, times(2)).insert(any(CouponActivationLog.class));
    }

    /**
     * 验证共享码首次兑换成功时会预占成功日志并按相对天数发券。
     */
    @Test
    void redeemCurrentUserCode_WhenSharedCodeFirstUsed_ShouldGrantCouponWithRelativeWindow() {
        ActivationRedeemCodeDto redeemCode = buildRedeemCode(
                ActivationRedeemRuleTypeEnum.SHARED_PER_USER_ONCE.getType(),
                ActivationCodeValidityTypeEnum.AFTER_ACTIVATION.getType(),
                ActivationCodeStatusEnum.ACTIVE.getType(),
                ActivationCodeItemStatusEnum.ACTIVE.getType()
        );
        redeemCode.setRelativeValidDays(3);
        when(activationCodeMapper.selectRedeemCodeByHash(any())).thenReturn(redeemCode);
        when(activationLogMapper.insert(any(CouponActivationLog.class))).thenAnswer(invocation -> {
            CouponActivationLog log = invocation.getArgument(0);
            log.setId(4001L);
            return 1;
        });
        when(activationCodeMapper.increaseSuccessUseCount(any(), any(), any())).thenReturn(1);
        when(activationBatchMapper.increaseSuccessUseCount(any(), any(), any())).thenReturn(1);
        when(couponGrantCoreService.grantCoupon(any())).thenReturn(buildGrantResult(7002L));
        when(activationLogMapper.updateById(any(CouponActivationLog.class))).thenReturn(1);

        var result = service.redeemCurrentUserCode(buildRedeemRequest());

        assertEquals(7002L, result.getCouponId());
        verify(activationCodeMapper).increaseSuccessUseCount(any(), any(), any());
        verify(activationBatchMapper).increaseSuccessUseCount(any(), any(), any());
        verify(activationLogMapper).updateById(any(CouponActivationLog.class));
    }

    /**
     * 验证共享码同一用户重复兑换会被唯一约束拦截并记录失败日志。
     */
    @Test
    void redeemCurrentUserCode_WhenSharedCodeUsedBySameUserTwice_ShouldWriteFailureLog() {
        when(activationCodeMapper.selectRedeemCodeByHash(any()))
                .thenReturn(buildRedeemCode(
                        ActivationRedeemRuleTypeEnum.SHARED_PER_USER_ONCE.getType(),
                        ActivationCodeValidityTypeEnum.AFTER_ACTIVATION.getType(),
                        ActivationCodeStatusEnum.ACTIVE.getType(),
                        ActivationCodeItemStatusEnum.ACTIVE.getType()));
        when(activationLogMapper.insert(any(CouponActivationLog.class)))
                .thenThrow(new DuplicateKeyException("duplicate success user"))
                .thenReturn(1);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.redeemCurrentUserCode(buildRedeemRequest()));

        assertEquals("您已使用过该激活码", exception.getMessage());
        verify(activationLogMapper, times(2)).insert(any(CouponActivationLog.class));
    }

    /**
     * 验证同一用户可连续兑换多个不同唯一码。
     */
    @Test
    void redeemCurrentUserCode_WhenUserRedeemsDifferentUniqueCodes_ShouldSucceedTwice() {
        when(activationCodeMapper.selectRedeemCodeByHash(any()))
                .thenReturn(buildRedeemCode(
                        ActivationRedeemRuleTypeEnum.UNIQUE_SINGLE_USE.getType(),
                        ActivationCodeValidityTypeEnum.ONCE.getType(),
                        ActivationCodeStatusEnum.ACTIVE.getType(),
                        ActivationCodeItemStatusEnum.ACTIVE.getType()))
                .thenReturn(buildRedeemCode(
                        ActivationRedeemRuleTypeEnum.UNIQUE_SINGLE_USE.getType(),
                        ActivationCodeValidityTypeEnum.ONCE.getType(),
                        ActivationCodeStatusEnum.ACTIVE.getType(),
                        ActivationCodeItemStatusEnum.ACTIVE.getType()));
        when(activationCodeMapper.markUniqueCodeUsed(any(), any(), any())).thenReturn(1, 1);
        when(activationBatchMapper.increaseSuccessUseCount(any(), any(), any())).thenReturn(1, 1);
        when(couponGrantCoreService.grantCoupon(any()))
                .thenReturn(buildGrantResult(8001L))
                .thenReturn(buildGrantResult(8002L));
        when(activationLogMapper.insert(any(CouponActivationLog.class))).thenReturn(1);

        var firstResult = service.redeemCurrentUserCode(buildRedeemRequest());
        var secondResult = service.redeemCurrentUserCode(buildRedeemRequest());

        assertEquals(8001L, firstResult.getCouponId());
        assertEquals(8002L, secondResult.getCouponId());
        verify(activationCodeMapper, times(2)).markUniqueCodeUsed(any(), any(), any());
    }

    /**
     * 构建兑换请求。
     *
     * @return 兑换请求
     */
    private ActivationCodeRedeemRequest buildRedeemRequest() {
        ActivationCodeRedeemRequest request = new ActivationCodeRedeemRequest();
        request.setCode("ABCD1234EFGH5678");
        request.setCaptchaVerificationId("captcha-verification-id");
        return request;
    }

    /**
     * 构建兑换用激活码信息。
     *
     * @param codeMode     激活码模式
     * @param validityType 有效期类型
     * @param batchStatus  批次状态
     * @param codeStatus   单码状态
     * @return 兑换用激活码信息
     */
    private ActivationRedeemCodeDto buildRedeemCode(String codeMode,
                                                    String validityType,
                                                    String batchStatus,
                                                    String codeStatus) {
        ActivationRedeemCodeDto redeemCode = new ActivationRedeemCodeDto();
        redeemCode.setBatchId(9001L);
        redeemCode.setCodeId(1001L);
        redeemCode.setBatchNo("ACT202604091200000001");
        redeemCode.setTemplateId(1L);
        redeemCode.setRedeemRuleType(codeMode);
        redeemCode.setValidityType(validityType);
        redeemCode.setFixedEffectiveTime(new Date(System.currentTimeMillis() - 60_000L));
        redeemCode.setFixedExpireTime(new Date(System.currentTimeMillis() + 86_400_000L));
        redeemCode.setRelativeValidDays(7);
        redeemCode.setBatchStatus(batchStatus);
        redeemCode.setCodeStatus(codeStatus);
        redeemCode.setPlainCode("ABCD1234EFGH5678");
        return redeemCode;
    }

    /**
     * 构建发券结果。
     *
     * @param couponId 用户优惠券ID
     * @return 发券结果
     */
    private CouponGrantResultDto buildGrantResult(Long couponId) {
        return CouponGrantResultDto.builder()
                .couponId(couponId)
                .templateId(1L)
                .couponName("新人100元券")
                .thresholdAmount(new BigDecimal("100.00"))
                .totalAmount(new BigDecimal("100.00"))
                .availableAmount(new BigDecimal("100.00"))
                .effectiveTime(new Date())
                .expireTime(new Date(System.currentTimeMillis() + 86_400_000L))
                .couponStatus("AVAILABLE")
                .build();
    }
}
