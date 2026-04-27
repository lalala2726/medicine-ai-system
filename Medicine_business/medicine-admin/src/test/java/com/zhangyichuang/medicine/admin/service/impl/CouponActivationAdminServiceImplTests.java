package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.mapper.CouponTemplateMapper;
import com.zhangyichuang.medicine.admin.mapper.UserMapper;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.model.coupon.ActivationBatchRowDto;
import com.zhangyichuang.medicine.model.coupon.ActivationCodeRowDto;
import com.zhangyichuang.medicine.model.coupon.ActivationRedeemLogRowDto;
import com.zhangyichuang.medicine.model.entity.CouponActivationBatch;
import com.zhangyichuang.medicine.model.entity.CouponTemplate;
import com.zhangyichuang.medicine.model.enums.ActivationCodeStatusEnum;
import com.zhangyichuang.medicine.model.enums.CouponTemplateStatusEnum;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationBatchMapper;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationCodeMapper;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 管理端激活码服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class CouponActivationAdminServiceImplTests {

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
     * 优惠券模板 Mapper。
     */
    @Mock
    private CouponTemplateMapper couponTemplateMapper;

    /**
     * 用户 Mapper。
     */
    @Mock
    private UserMapper userMapper;

    /**
     * 滑块验证码服务。
     */
    @Mock
    private CaptchaService captchaService;

    /**
     * 被测服务。
     */
    private CouponActivationAdminServiceImpl service;

    /**
     * 初始化被测服务。
     */
    @BeforeEach
    void setUp() {
        service = spy(new CouponActivationAdminServiceImpl(
                activationBatchMapper,
                activationCodeMapper,
                activationLogMapper,
                couponTemplateMapper,
                userMapper,
                captchaService
        ));
        lenient().doReturn("admin").when(service).getUsername();
    }

    /**
     * 验证共享码生成时会创建一个批次和一条单码明细。
     */
    @Test
    void generateActivationCodes_WhenSharedMode_ShouldGenerateSingleCode() {
        ActivationCodeGenerateRequest request = new ActivationCodeGenerateRequest();
        request.setTemplateId(1L);
        request.setRedeemRuleType("SHARED_PER_USER_ONCE");
        request.setGenerateCount(20);
        request.setValidityType("ONCE");
        request.setFixedEffectiveTime(new Date(System.currentTimeMillis() - 60_000L));
        request.setFixedExpireTime(new Date(System.currentTimeMillis() + 86_400_000L));

        when(couponTemplateMapper.selectById(1L)).thenReturn(buildActiveTemplate());
        when(activationBatchMapper.insert(any(CouponActivationBatch.class))).thenAnswer(invocation -> {
            CouponActivationBatch batch = invocation.getArgument(0);
            batch.setId(9001L);
            return 1;
        });
        when(activationCodeMapper.batchInsertIgnore(any())).thenAnswer(invocation -> {
            List<?> codes = invocation.getArgument(0);
            return codes.size();
        });
        when(activationCodeMapper.selectCodesByBatchId(9001L))
                .thenReturn(List.of(buildCodeRow(1001L, "ABCD1234EFGH5678")));

        var result = service.generateActivationCodes(request);

        assertEquals(1, result.getGeneratedCount());
        assertEquals(1, result.getCodes().size());
        verify(activationBatchMapper, times(1)).insert(any(CouponActivationBatch.class));
        verify(activationCodeMapper, times(1)).batchInsertIgnore(any());
        verify(activationLogMapper, never()).insert(any(com.zhangyichuang.medicine.model.entity.CouponActivationLog.class));
    }

    /**
     * 验证唯一码生成时会按请求数量创建多条单码明细。
     */
    @Test
    void generateActivationCodes_WhenUniqueMode_ShouldGenerateRequestedCount() {
        ActivationCodeGenerateRequest request = new ActivationCodeGenerateRequest();
        request.setTemplateId(1L);
        request.setRedeemRuleType("UNIQUE_SINGLE_USE");
        request.setGenerateCount(2);
        request.setValidityType("AFTER_ACTIVATION");
        request.setRelativeValidDays(15);

        when(couponTemplateMapper.selectById(1L)).thenReturn(buildActiveTemplate());
        when(activationBatchMapper.insert(any(CouponActivationBatch.class))).thenAnswer(invocation -> {
            CouponActivationBatch batch = invocation.getArgument(0);
            batch.setId(9002L);
            return 1;
        });
        when(activationCodeMapper.batchInsertIgnore(any())).thenAnswer(invocation -> {
            List<?> codes = invocation.getArgument(0);
            return codes.size();
        });
        when(activationCodeMapper.selectCodesByBatchId(9002L)).thenReturn(List.of(
                buildCodeRow(1001L, "ABCD1234EFGH5678"),
                buildCodeRow(1002L, "IJKL1234MNOP5678")
        ));

        var result = service.generateActivationCodes(request);

        assertEquals(2, result.getGeneratedCount());
        assertEquals(2, result.getCodes().size());
        verify(activationBatchMapper, times(1)).insert(any(CouponActivationBatch.class));
        verify(activationCodeMapper, times(1)).batchInsertIgnore(any());
    }

    /**
     * 验证批次列表查询会透传 XML 分页结果。
     */
    @Test
    void listActivationCodes_ShouldReturnBatchPage() {
        ActivationCodeListRequest request = new ActivationCodeListRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        ActivationBatchRowDto batchRow = buildBatchRow();
        Page<ActivationBatchRowDto> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(batchRow));
        when(activationBatchMapper.selectBatchPage(any(), any())).thenReturn(page);

        var result = service.listActivationCodes(request);

        assertEquals(1, result.getTotal());
        assertEquals("ACT202604091200000001", result.getRecords().getFirst().getBatchNo());
        assertEquals(20, result.getRecords().getFirst().getGenerateCount());
    }

    /**
     * 验证批次详情查询会返回批次级信息。
     */
    @Test
    void getActivationCodeDetail_ShouldReturnBatchDetail() {
        when(activationBatchMapper.selectBatchDetail(9001L)).thenReturn(buildBatchRow());

        var result = service.getActivationCodeDetail(9001L);

        assertEquals("ACT202604091200000001", result.getBatchNo());
        assertEquals(20, result.getGenerateCount());
    }

    /**
     * 验证查看批次激活码会返回当前批次下全部明文码。
     */
    @Test
    void listActivationBatchCodes_ShouldReturnCodes() {
        when(activationBatchMapper.selectBatchDetail(9001L)).thenReturn(buildBatchRow());
        Page<ActivationCodeRowDto> page = new Page<>(1, 10, 2);
        page.setRecords(List.of(
                buildCodeRow(1001L, "ABCD1234EFGH5678"),
                buildCodeRow(1002L, "IJKL1234MNOP5678")
        ));
        when(activationCodeMapper.selectCodePageByBatchId(any(), eq(9001L))).thenReturn(page);

        ActivationBatchCodeListRequest request = new ActivationBatchCodeListRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        var result = service.listActivationBatchCodes(9001L, request);

        assertEquals(2, result.getTotal());
        assertEquals("ABCD1234EFGH5678", result.getRecords().getFirst().getPlainCode());
    }

    /**
     * 验证批次状态更新会同步更新批次状态并下发单码状态批量更新。
     */
    @Test
    void updateActivationCodeStatus_ShouldUpdateBatchAndCodes() {
        ActivationCodeStatusUpdateRequest request = new ActivationCodeStatusUpdateRequest();
        request.setId(9001L);
        request.setStatus(ActivationCodeStatusEnum.DISABLED.getType());
        when(activationBatchMapper.selectById(9001L)).thenReturn(CouponActivationBatch.builder()
                .id(9001L)
                .status(ActivationCodeStatusEnum.ACTIVE.getType())
                .isDeleted(0)
                .build());
        when(activationBatchMapper.updateBatchStatus(eq(9001L), eq("DISABLED"), any(), any())).thenReturn(1);
        when(activationCodeMapper.updateCodeStatusByBatchId(eq(9001L), eq("ACTIVE"), eq("DISABLED"), any(), any()))
                .thenReturn(2);

        boolean result = service.updateActivationCodeStatus(request);

        assertTrue(result);
        verify(activationBatchMapper).updateBatchStatus(eq(9001L), eq("DISABLED"), any(), any());
        verify(activationCodeMapper).updateCodeStatusByBatchId(eq(9001L), eq("ACTIVE"), eq("DISABLED"), any(), any());
    }

    /**
     * 验证兑换日志查询会透传 XML 分页结果。
     */
    @Test
    void listActivationLogs_ShouldReturnRedeemLogPage() {
        ActivationLogListRequest request = new ActivationLogListRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        Page<ActivationRedeemLogRowDto> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(buildRedeemLogRow()));
        when(activationLogMapper.selectRedeemLogPage(any(), any())).thenReturn(page);

        var result = service.listActivationLogs(request);

        assertEquals(1, result.getTotal());
        assertEquals("ACT202604091200000001", result.getRecords().getFirst().getBatchNo());
        assertEquals("SUCCESS", result.getRecords().getFirst().getResultStatus());
    }

    /**
     * 构建启用中的优惠券模板。
     *
     * @return 优惠券模板实体
     */
    private CouponTemplate buildActiveTemplate() {
        return CouponTemplate.builder()
                .id(1L)
                .name("新人100元券")
                .thresholdAmount(new BigDecimal("100.00"))
                .faceAmount(new BigDecimal("100.00"))
                .continueUseEnabled(1)
                .stackableEnabled(0)
                .status(CouponTemplateStatusEnum.ACTIVE.getType())
                .build();
    }

    /**
     * 构建批次查询结果。
     *
     * @return 批次查询结果
     */
    private ActivationBatchRowDto buildBatchRow() {
        ActivationBatchRowDto row = new ActivationBatchRowDto();
        row.setId(9001L);
        row.setBatchNo("ACT202604091200000001");
        row.setTemplateId(1L);
        row.setTemplateName("新人100元券");
        row.setRedeemRuleType("UNIQUE_SINGLE_USE");
        row.setValidityType("AFTER_ACTIVATION");
        row.setRelativeValidDays(15);
        row.setStatus("ACTIVE");
        row.setGenerateCount(20);
        row.setSuccessUseCount(3);
        row.setCreateBy("admin");
        row.setCreateTime(new Date());
        return row;
    }

    /**
     * 构建单码查询结果。
     *
     * @param codeId    激活码ID
     * @param plainCode 激活码明文
     * @return 单码查询结果
     */
    private ActivationCodeRowDto buildCodeRow(Long codeId, String plainCode) {
        ActivationCodeRowDto row = new ActivationCodeRowDto();
        row.setId(codeId);
        row.setBatchId(9001L);
        row.setPlainCode(plainCode);
        row.setStatus("ACTIVE");
        row.setSuccessUseCount(0);
        row.setCreateTime(new Date());
        return row;
    }

    /**
     * 构建兑换日志查询结果。
     *
     * @return 兑换日志查询结果
     */
    private ActivationRedeemLogRowDto buildRedeemLogRow() {
        ActivationRedeemLogRowDto row = new ActivationRedeemLogRowDto();
        row.setId(3001L);
        row.setBatchId(9001L);
        row.setActivationCodeId(1001L);
        row.setBatchNo("ACT202604091200000001");
        row.setTemplateId(1L);
        row.setTemplateName("新人100元券");
        row.setPlainCodeSnapshot("ABCD1234EFGH5678");
        row.setUserId(88L);
        row.setCouponId(7001L);
        row.setResultStatus("SUCCESS");
        row.setCreateTime(new Date());
        return row;
    }
}
