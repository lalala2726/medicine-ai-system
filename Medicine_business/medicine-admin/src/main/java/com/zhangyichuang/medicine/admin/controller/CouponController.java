package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.CouponLogVo;
import com.zhangyichuang.medicine.admin.model.vo.CouponTemplateVo;
import com.zhangyichuang.medicine.admin.service.CouponAdminService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.log.annotation.OperationLog;
import com.zhangyichuang.medicine.common.log.enums.OperationType;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.enums.CouponTemplateDeleteModeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端优惠券控制器。
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/mall/coupon")
@Tag(name = "优惠券管理", description = "管理端优惠券接口")
@PreventDuplicateSubmit
public class CouponController extends BaseController {

    /**
     * 管理端优惠券服务。
     */
    private final CouponAdminService couponAdminService;

    /**
     * 查询优惠券模板列表。
     *
     * @param request 查询请求
     * @return 优惠券模板列表
     */
    @GetMapping("/template/list")
    @Operation(summary = "查询优惠券模板列表")
    @PreAuthorize("hasAuthority('mall:coupon:template:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listTemplates(CouponTemplateListRequest request) {
        Page<CouponTemplateVo> page = couponAdminService.listTemplates(request);
        return getTableData(page, page.getRecords());
    }

    /**
     * 查询优惠券模板详情。
     *
     * @param id 模板ID
     * @return 优惠券模板详情
     */
    @GetMapping("/template/{id:\\d+}")
    @Operation(summary = "查询优惠券模板详情")
    @PreAuthorize("hasAuthority('mall:coupon:template:query') or hasRole('super_admin')")
    public AjaxResult<CouponTemplateVo> getTemplate(@PathVariable Long id) {
        return success(couponAdminService.getTemplate(id));
    }

    /**
     * 新增优惠券模板。
     *
     * @param request 新增请求
     * @return 新增结果
     */
    @PostMapping("/template")
    @Operation(summary = "新增优惠券模板")
    @PreAuthorize("hasAuthority('mall:coupon:template:add') or hasRole('super_admin')")
    public AjaxResult<Void> addTemplate(@Validated @RequestBody CouponTemplateAddRequest request) {
        return toAjax(couponAdminService.addTemplate(request));
    }

    /**
     * 修改优惠券模板。
     *
     * @param request 修改请求
     * @return 修改结果
     */
    @PutMapping("/template")
    @Operation(summary = "修改优惠券模板")
    @PreAuthorize("hasAuthority('mall:coupon:template:edit') or hasRole('super_admin')")
    public AjaxResult<Void> updateTemplate(@Validated @RequestBody CouponTemplateUpdateRequest request) {
        return toAjax(couponAdminService.updateTemplate(request));
    }

    /**
     * 删除优惠券模板。
     *
     * @param id         模板ID
     * @param deleteMode 删除模式
     * @return 删除结果
     */
    @DeleteMapping("/template/{id:\\d+}")
    @Operation(summary = "删除优惠券模板")
    @PreAuthorize("hasAuthority('mall:coupon:template:delete') or hasRole('super_admin')")
    @OperationLog(module = "优惠券管理", action = "删除优惠券模板", type = OperationType.DELETE)
    public AjaxResult<Void> deleteTemplate(@PathVariable Long id, @RequestParam CouponTemplateDeleteModeEnum deleteMode) {
        return toAjax(couponAdminService.deleteTemplate(id, deleteMode));
    }

    /**
     * 管理端发券。
     *
     * @param request 发券请求
     * @return 发券结果
     */
    @PostMapping("/issue")
    @Operation(summary = "管理端发券")
    @PreAuthorize("hasAuthority('mall:coupon:issue') or hasRole('super_admin')")
    public AjaxResult<Void> issueCoupon(@Validated @RequestBody CouponIssueRequest request) {
        return toAjax(couponAdminService.issueCoupon(request));
    }

    /**
     * 查询优惠券日志列表。
     *
     * @param request 查询请求
     * @return 优惠券日志列表
     */
    @GetMapping("/log/list")
    @Operation(summary = "查询优惠券日志列表")
    @PreAuthorize("hasAuthority('mall:coupon:log:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listCouponLogs(CouponLogListRequest request) {
        Page<CouponLogVo> page = couponAdminService.listCouponLogs(request);
        return getTableData(page, page.getRecords());
    }
}
