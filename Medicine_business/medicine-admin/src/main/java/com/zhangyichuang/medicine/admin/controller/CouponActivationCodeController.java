package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.*;
import com.zhangyichuang.medicine.admin.service.CouponActivationAdminService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.log.annotation.OperationLog;
import com.zhangyichuang.medicine.common.log.enums.OperationType;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 管理端激活码批次控制器。
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/mall/coupon")
@PreventDuplicateSubmit
@Tag(name = "激活码管理", description = "管理端激活码批次接口")
public class CouponActivationCodeController extends BaseController {

    /**
     * 管理端激活码服务。
     */
    private final CouponActivationAdminService couponActivationAdminService;

    /**
     * 生成激活码批次。
     *
     * @param request 生成请求
     * @return 生成结果
     */
    @PostMapping("/activation-batch/generate")
    @Operation(summary = "生成激活码批次")
    @PreAuthorize("hasAuthority('mall:coupon:activation-batch:generate') or hasRole('super_admin')")
    @OperationLog(module = "激活码管理", action = "生成激活码批次", type = OperationType.ADD)
    public AjaxResult<ActivationCodeGenerateResultVo> generateActivationCodes(
            @Validated @RequestBody ActivationCodeGenerateRequest request) {
        return success(couponActivationAdminService.generateActivationCodes(request));
    }

    /**
     * 查询激活码批次列表。
     *
     * @param request 查询请求
     * @return 激活码批次列表
     */
    @GetMapping("/activation-batch/list")
    @Operation(summary = "查询激活码批次列表")
    @PreAuthorize("hasAuthority('mall:coupon:activation-batch:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listActivationCodes(ActivationCodeListRequest request) {
        Page<ActivationCodeVo> page = couponActivationAdminService.listActivationCodes(request);
        return getTableData(page, page.getRecords());
    }

    /**
     * 查询激活码批次详情。
     *
     * @param id 激活码批次ID
     * @return 激活码批次详情
     */
    @GetMapping("/activation-batch/{id:\\d+}")
    @Operation(summary = "查询激活码批次详情")
    @PreAuthorize("hasAuthority('mall:coupon:activation-batch:query') or hasRole('super_admin')")
    public AjaxResult<ActivationCodeDetailVo> getActivationCodeDetail(@PathVariable Long id) {
        return success(couponActivationAdminService.getActivationCodeDetail(id));
    }

    /**
     * 查询批次下的全部激活码。
     *
     * @param id      激活码批次ID
     * @param request 分页查询请求
     * @return 激活码明细分页列表
     */
    @GetMapping("/activation-batch/{id:\\d+}/codes")
    @Operation(summary = "查询批次下的全部激活码")
    @PreAuthorize("hasAuthority('mall:coupon:activation-batch:query') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listActivationBatchCodes(@PathVariable Long id,
                                                                ActivationBatchCodeListRequest request) {
        Page<ActivationCodeGeneratedItemVo> page = couponActivationAdminService.listActivationBatchCodes(id, request);
        return getTableData(page, page.getRecords());
    }

    /**
     * 导出批次下的全部激活码。
     *
     * @param id       激活码批次ID
     * @param response Http 响应对象
     * @return 无返回值
     * @throws IOException IO 异常
     */
    @GetMapping("/activation-batch/{id:\\d+}/codes/export")
    @Operation(summary = "导出批次下的全部激活码")
    @PreAuthorize("hasAuthority('mall:coupon:activation-batch:query') or hasRole('super_admin')")
    @OperationLog(module = "激活码管理", action = "导出激活码列表", type = OperationType.OTHER)
    public void exportActivationBatchCodes(@PathVariable Long id,
                                           HttpServletResponse response) throws IOException {
        couponActivationAdminService.exportActivationBatchCodes(id, response);
    }

    /**
     * 更新激活码批次状态。
     *
     * @param request 状态更新请求
     * @return 更新结果
     */
    @PutMapping("/activation-batch/status")

    @Operation(summary = "更新激活码批次状态")
    @PreAuthorize("hasAuthority('mall:coupon:activation-batch:status') or hasRole('super_admin')")
    @OperationLog(module = "激活码管理", action = "更新激活码批次状态", type = OperationType.UPDATE)
    public AjaxResult<Void> updateActivationCodeStatus(
            @Validated @RequestBody ActivationCodeStatusUpdateRequest request) {
        return toAjax(couponActivationAdminService.updateActivationCodeStatus(request));
    }

    /**
     * 删除激活码批次。
     *
     * @param id 激活码批次ID
     * @return 删除结果
     */
    @DeleteMapping("/activation-batch/{id:\\d+}")
    @Operation(summary = "删除激活码批次")
    @PreAuthorize("hasAuthority('mall:coupon:activation-batch:status') or hasRole('super_admin')")
    @OperationLog(module = "激活码管理", action = "删除激活码批次", type = OperationType.DELETE)
    public AjaxResult<Void> deleteActivationBatch(@PathVariable Long id) {
        return toAjax(couponActivationAdminService.deleteActivationBatch(id));
    }

    /**
     * 更新激活码单码状态。
     *
     * @param request 状态更新请求
     * @return 更新结果
     */
    @PutMapping("/activation-code/status")
    @Operation(summary = "更新激活码单码状态")
    @PreAuthorize("hasAuthority('mall:coupon:activation-batch:status') or hasRole('super_admin')")
    @OperationLog(module = "激活码管理", action = "更新激活码单码状态", type = OperationType.UPDATE)
    public AjaxResult<Void> updateActivationCodeItemStatus(
            @Validated @RequestBody ActivationCodeItemStatusUpdateRequest request) {
        return toAjax(couponActivationAdminService.updateActivationCodeItemStatus(request));
    }

    /**
     * 删除激活码单码。
     *
     * @param id 激活码ID
     * @return 删除结果
     */
    @DeleteMapping("/activation-code/{id:\\d+}")
    @Operation(summary = "删除激活码单码")
    @PreAuthorize("hasAuthority('mall:coupon:activation-batch:status') or hasRole('super_admin')")
    @OperationLog(module = "激活码管理", action = "删除激活码单码", type = OperationType.DELETE)
    public AjaxResult<Void> deleteActivationCodeItem(@PathVariable Long id) {
        return toAjax(couponActivationAdminService.deleteActivationCodeItem(id));
    }

    /**
     * 查询激活码兑换日志列表。
     *
     * @param request 查询请求
     * @return 激活码兑换日志列表
     */
    @GetMapping("/activation-log/redeem/list")
    @Operation(summary = "查询激活码兑换日志列表")
    @PreAuthorize("hasAuthority('mall:coupon:activation-log:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listActivationLogs(ActivationLogListRequest request) {
        Page<ActivationLogVo> page = couponActivationAdminService.listActivationLogs(request);
        return getTableData(page, page.getRecords());
    }
}
