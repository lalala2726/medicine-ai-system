package com.zhangyichuang.medicine.client.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.client.model.request.*;
import com.zhangyichuang.medicine.client.model.vo.AfterSaleApplyResultVo;
import com.zhangyichuang.medicine.client.model.vo.AfterSaleEligibilityVo;
import com.zhangyichuang.medicine.client.service.MallAfterSaleService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.vo.AfterSaleDetailVo;
import com.zhangyichuang.medicine.model.vo.AfterSaleListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 售后管理
 *
 * @author Chuang
 * created 2025/11/08
 */
@Slf4j
@RestController
@RequestMapping("/mall/order/after_sale")
@RequiredArgsConstructor
@Tag(name = "售后管理", description = "用户端售后申请、取消、查询接口")
@PreventDuplicateSubmit
public class MallAfterSaleController extends BaseController {

    private final MallAfterSaleService mallAfterSaleService;

    /**
     * 申请售后
     *
     * @param request 申请售后参数
     * @return 售后申请编号
     */
    @PostMapping("/apply")
    @Operation(summary = "申请售后", description = "统一售后申请入口，支持整单退款与单商品售后申请")
    public AjaxResult<AfterSaleApplyResultVo> applyAfterSale(@Validated @RequestBody AfterSaleApplyRequest request) {
        AfterSaleApplyResultVo result = mallAfterSaleService.applyAfterSale(request);
        return success(result);
    }

    /**
     * 售后资格校验
     */
    @GetMapping("/eligibility")
    @Operation(summary = "售后资格校验", description = "返回订单商品售后资格与可退金额信息")
    public AjaxResult<AfterSaleEligibilityVo> getAfterSaleEligibility(@Validated AfterSaleEligibilityRequest request) {
        AfterSaleEligibilityVo result = mallAfterSaleService.getAfterSaleEligibility(request);
        return success(result);
    }

    /**
     * 取消售后
     *
     * @param request 取消售后参数
     * @return 响应结果
     */
    @PostMapping("/cancel")
    @Operation(summary = "取消售后", description = "用户取消售后申请(仅待审核状态可取消)")
    public AjaxResult<Void> cancelAfterSale(@Validated @RequestBody AfterSaleCancelRequest request) {
        boolean result = mallAfterSaleService.cancelAfterSale(request);
        return toAjax(result);
    }

    /**
     * 再次申请售后
     */
    @PostMapping("/reapply")
    @Operation(summary = "再次申请售后", description = "售后被拒绝后，用户重新发起申请")
    public AjaxResult<Void> reapplyAfterSale(@Validated @RequestBody AfterSaleReapplyRequest request) {
        String afterSaleNo = mallAfterSaleService.reapplyAfterSale(request);
        return success(afterSaleNo);
    }

    /**
     * 查询售后列表
     *
     * @param request 查询参数
     * @return 售后列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询售后列表", description = "查询当前用户的售后申请列表")
    public AjaxResult<TableDataResult> getAfterSaleList(@Validated AfterSaleListRequest request) {
        Page<AfterSaleListVo> page = mallAfterSaleService.getAfterSaleList(request);
        return getTableData(page);
    }

    /**
     * 售后详情
     *
     * @param afterSaleNo 售后单号
     * @return 售后详情
     */
    @GetMapping("/detail/{afterSaleNo}")
    @Operation(summary = "查询售后详情", description = "根据售后单号查询售后申请详情和时间线")
    public AjaxResult<AfterSaleDetailVo> getAfterSaleDetail(
            @Parameter(description = "售后单号", required = true)
            @PathVariable String afterSaleNo) {
        AfterSaleDetailVo detail = mallAfterSaleService.getAfterSaleDetail(afterSaleNo, getUserId());
        return success(detail);
    }
}
