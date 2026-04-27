package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.ActivationCodeDetailVo;
import com.zhangyichuang.medicine.admin.model.vo.ActivationCodeGenerateResultVo;
import com.zhangyichuang.medicine.admin.model.vo.ActivationCodeGeneratedItemVo;
import com.zhangyichuang.medicine.admin.model.vo.ActivationCodeVo;
import com.zhangyichuang.medicine.admin.service.CouponActivationAdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端激活码控制器测试。
 */
@ExtendWith(MockitoExtension.class)
class CouponActivationCodeControllerTests {

    /**
     * 管理端激活码服务。
     */
    @Mock
    private CouponActivationAdminService couponActivationAdminService;

    /**
     * 被测控制器。
     */
    @InjectMocks
    private CouponActivationCodeController controller;

    /**
     * 验证生成批次接口会返回统一响应结构。
     */
    @Test
    void generateActivationCodes_ShouldReturnUnifiedResult() {
        ActivationCodeGenerateRequest request = new ActivationCodeGenerateRequest();
        ActivationCodeGenerateResultVo resultVo = ActivationCodeGenerateResultVo.builder()
                .batchNo("ACT202604091200000001")
                .generatedCount(1)
                .codes(List.of(ActivationCodeGeneratedItemVo.builder()
                        .id(1001L)
                        .plainCode("ABCD1234EFGH5678")
                        .status("ACTIVE")
                        .build()))
                .build();
        when(couponActivationAdminService.generateActivationCodes(request)).thenReturn(resultVo);

        var result = controller.generateActivationCodes(request);

        assertEquals(200, result.getCode());
        assertEquals("ACT202604091200000001", result.getData().getBatchNo());
        verify(couponActivationAdminService).generateActivationCodes(request);
    }

    /**
     * 验证批次列表接口会透传分页查询。
     */
    @Test
    void listActivationCodes_ShouldReturnPagedResult() {
        ActivationCodeListRequest request = new ActivationCodeListRequest();
        Page<ActivationCodeVo> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(ActivationCodeVo.builder().id(9001L).batchNo("ACT202604091200000001").build()));
        when(couponActivationAdminService.listActivationCodes(request)).thenReturn(page);

        var result = controller.listActivationCodes(request);

        assertEquals(200, result.getCode());
        verify(couponActivationAdminService).listActivationCodes(request);
    }

    /**
     * 验证批次详情接口会返回批次级信息。
     */
    @Test
    void getActivationCodeDetail_ShouldReturnBatchDetail() {
        ActivationCodeDetailVo resultVo = ActivationCodeDetailVo.builder()
                .id(9001L)
                .batchNo("ACT202604091200000001")
                .generateCount(20)
                .build();
        when(couponActivationAdminService.getActivationCodeDetail(9001L)).thenReturn(resultVo);

        var result = controller.getActivationCodeDetail(9001L);

        assertEquals(200, result.getCode());
        assertEquals(20, result.getData().getGenerateCount());
        verify(couponActivationAdminService).getActivationCodeDetail(9001L);
    }

    /**
     * 验证批次明细接口会返回全部激活码。
     */
    @Test
    void listActivationBatchCodes_ShouldReturnCodes() {
        ActivationBatchCodeListRequest request = new ActivationBatchCodeListRequest();
        Page<ActivationCodeGeneratedItemVo> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(ActivationCodeGeneratedItemVo.builder().id(1001L).plainCode("ABCD1234EFGH5678").build()));
        when(couponActivationAdminService.listActivationBatchCodes(9001L, request)).thenReturn(page);

        var result = controller.listActivationBatchCodes(9001L, request);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getRows().size());
        verify(couponActivationAdminService).listActivationBatchCodes(9001L, request);
    }

    /**
     * 验证状态更新接口会委托服务执行状态切换。
     */
    @Test
    void updateActivationCodeStatus_ShouldDelegateToService() {
        ActivationCodeStatusUpdateRequest request = new ActivationCodeStatusUpdateRequest();
        when(couponActivationAdminService.updateActivationCodeStatus(request)).thenReturn(true);

        var result = controller.updateActivationCodeStatus(request);

        assertEquals(200, result.getCode());
        verify(couponActivationAdminService).updateActivationCodeStatus(request);
    }

    /**
     * 验证激活码控制器接口都声明了权限注解。
     */
    @Test
    void methods_ShouldHavePreAuthorizeAnnotations() throws NoSuchMethodException {
        Method generateMethod = CouponActivationCodeController.class.getMethod(
                "generateActivationCodes",
                ActivationCodeGenerateRequest.class
        );
        Method listMethod = CouponActivationCodeController.class.getMethod(
                "listActivationCodes",
                ActivationCodeListRequest.class
        );
        Method detailMethod = CouponActivationCodeController.class.getMethod(
                "getActivationCodeDetail",
                Long.class
        );
        Method codesMethod = CouponActivationCodeController.class.getMethod(
                "listActivationBatchCodes",
                Long.class,
                ActivationBatchCodeListRequest.class
        );
        Method statusMethod = CouponActivationCodeController.class.getMethod(
                "updateActivationCodeStatus",
                ActivationCodeStatusUpdateRequest.class
        );
        Method redeemLogMethod = CouponActivationCodeController.class.getMethod(
                "listActivationLogs",
                ActivationLogListRequest.class
        );

        assertTrue(generateMethod.isAnnotationPresent(PreAuthorize.class));
        assertTrue(listMethod.isAnnotationPresent(PreAuthorize.class));
        assertTrue(detailMethod.isAnnotationPresent(PreAuthorize.class));
        assertTrue(codesMethod.isAnnotationPresent(PreAuthorize.class));
        assertTrue(statusMethod.isAnnotationPresent(PreAuthorize.class));
        assertTrue(redeemLogMethod.isAnnotationPresent(PreAuthorize.class));
    }
}
