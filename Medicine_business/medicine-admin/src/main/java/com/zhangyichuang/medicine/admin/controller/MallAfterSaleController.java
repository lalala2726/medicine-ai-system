package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleAuditRequest;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleListRequest;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleProcessRequest;
import com.zhangyichuang.medicine.admin.model.vo.MallAfterSaleListVo;
import com.zhangyichuang.medicine.admin.service.MallAfterSaleService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import com.zhangyichuang.medicine.model.vo.AfterSaleDetailVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 售后管理Controller(管理端)
 *
 * @author Chuang
 * created 2025/11/08
 */
@Slf4j
@RestController
@RequestMapping("/mall/after-sale")
@RequiredArgsConstructor
@Tag(name = "售后管理(管理端)", description = "管理端售后审核、处理、查询接口")
@PreventDuplicateSubmit
public class MallAfterSaleController extends BaseController {

    private final MallAfterSaleService mallAfterSaleService;

    /**
     * 查询售后列表
     * <p>
     * 功能描述：按查询条件分页获取售后申请列表，并在控制层完成 DTO 到 VO 的结构转换。
     *
     * @param request 查询参数对象，包含分页参数与售后筛选条件
     * @return 返回分页结构，rows 为 {@link MallAfterSaleListVo} 集合
     * @throws RuntimeException 异常说明：当下游服务或数据转换出现异常时抛出运行时异常
     */
    @GetMapping("/list")
    @Operation(summary = "查询售后列表", description = "管理员查询所有售后申请列表")
    @PreAuthorize("hasAuthority('mall:after_sale:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> getAfterSaleList(AfterSaleListRequest request) {
        Page<MallAfterSaleListDto> page = mallAfterSaleService.getAfterSaleList(request);
        List<MallAfterSaleListVo> rows = page.getRecords().stream()
                .map(this::toMallAfterSaleListVo)
                .toList();
        return getTableData(page, rows);
    }

    /**
     * 获取售后详情
     *
     * @param afterSaleId 售后申请ID
     * @return 售后详情
     */
    @GetMapping("/detail/{afterSaleId}")
    @Operation(summary = "查询售后详情", description = "管理员查询售后申请详情")
    @PreAuthorize("hasAuthority('mall:after_sale:query') or hasRole('super_admin')")
    public AjaxResult<AfterSaleDetailVo> getAfterSaleDetail(
            @Parameter(description = "售后申请ID", required = true)
            @PathVariable Long afterSaleId) {
        AfterSaleDetailVo detail = mallAfterSaleService.getAfterSaleDetail(afterSaleId);
        return success(detail);
    }

    /**
     * 审核售后申请
     *
     * @param request 审核参数
     * @return 是否成功
     */
    @PostMapping("/audit")
    @Operation(summary = "审核售后申请", description = "管理员审核售后申请(通过/拒绝)")
    @PreAuthorize("hasAuthority('mall:after_sale:audit') or hasRole('super_admin')")
    public AjaxResult<Void> auditAfterSale(@Validated @RequestBody AfterSaleAuditRequest request) {
        boolean result = mallAfterSaleService.auditAfterSale(request);
        return toAjax(result);
    }

    /**
     * 处理退款
     *
     * @param request 处理参数
     * @return 是否成功
     */
    @PostMapping("/process-refund")
    @Operation(summary = "处理退款", description = "管理员处理售后退款(原路退回)")
    @PreAuthorize("hasAuthority('mall:after_sale:refund') or hasRole('super_admin')")
    public AjaxResult<Void> processRefund(@Validated @RequestBody AfterSaleProcessRequest request) {
        boolean result = mallAfterSaleService.processRefund(request);
        return toAjax(result);
    }

    /**
     * 处理换货
     *
     * @param request 处理参数
     * @return 是否成功
     */
    @PostMapping("/process-exchange")
    @Operation(summary = "处理换货", description = "管理员处理售后换货")
    @PreAuthorize("hasAuthority('mall:after_sale:exchange') or hasRole('super_admin')")
    public AjaxResult<Void> processExchange(@Validated @RequestBody AfterSaleProcessRequest request) {
        boolean result = mallAfterSaleService.processExchange(request);
        return toAjax(result);
    }

    /**
     * 功能描述：将售后列表查询 DTO 转换为管理端列表 VO。
     *
     * @param source 售后列表查询 DTO，包含主表字段与联表字段
     * @return 返回管理端列表 VO；当 source 为空时返回 null
     * @throws RuntimeException 异常说明：当字段读取或对象构造过程出现异常时抛出运行时异常
     */
    private MallAfterSaleListVo toMallAfterSaleListVo(MallAfterSaleListDto source) {
        if (source == null) {
            return null;
        }
        MallAfterSaleListVo target = copyProperties(source, MallAfterSaleListVo.class);
        target.setAfterSaleType(source.getAfterSaleType());
        target.setAfterSaleStatus(source.getAfterSaleStatus());
        target.setApplyReason(source.getApplyReason());
        return target;
    }
}
