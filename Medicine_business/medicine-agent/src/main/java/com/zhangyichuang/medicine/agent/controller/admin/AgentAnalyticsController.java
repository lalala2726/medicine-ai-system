package com.zhangyichuang.medicine.agent.controller.admin;

import com.zhangyichuang.medicine.agent.service.AnalyticsService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.vo.analytics.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端智能体运营分析工具控制器。
 *
 * @author Chuang
 */
@RestController
@RequestMapping("/agent/admin/analytics")
@Tag(name = "管理端智能体运营分析工具", description = "用于管理端智能体运营分析查询接口")
@RequiredArgsConstructor
@PreAuthorize("hasRole('super_admin')")
@Validated
public class AgentAnalyticsController extends BaseController {

    private static final String DAYS_DESCRIPTION = "最近天数，默认30，范围1-730";

    private final AnalyticsService agentAnalyticsService;

    /**
     * 获取实时运营总览。
     *
     * @return 实时总览数据
     */
    @GetMapping("/realtime-overview")
    @Operation(summary = "实时运营总览", description = "返回累计成交、今日成交、待发货和售后待处理等实时指标")
    public AjaxResult<AnalyticsRealtimeOverviewVo> realtimeOverview() {
        return success(agentAnalyticsService.realtimeOverview());
    }

    /**
     * 获取指定时间范围内的经营结果汇总。
     *
     * @param days 最近天数
     * @return 经营结果汇总
     */
    @GetMapping("/range-summary")
    @Operation(summary = "经营结果汇总", description = DAYS_DESCRIPTION)
    public AjaxResult<AnalyticsRangeSummaryVo> rangeSummary(
            @Parameter(description = DAYS_DESCRIPTION)
            @RequestParam(defaultValue = "30") @Min(1) @Max(730) Integer days
    ) {
        return success(agentAnalyticsService.rangeSummary(days));
    }

    /**
     * 获取指定时间范围内的支付转化汇总。
     *
     * @param days 最近天数
     * @return 支付转化汇总
     */
    @GetMapping("/conversion-summary")
    @Operation(summary = "支付转化汇总", description = DAYS_DESCRIPTION)
    public AjaxResult<AnalyticsConversionSummaryVo> conversionSummary(
            @Parameter(description = DAYS_DESCRIPTION)
            @RequestParam(defaultValue = "30") @Min(1) @Max(730) Integer days
    ) {
        return success(agentAnalyticsService.conversionSummary(days));
    }

    /**
     * 获取指定时间范围内的履约时效汇总。
     *
     * @param days 最近天数
     * @return 履约时效汇总
     */
    @GetMapping("/fulfillment-summary")
    @Operation(summary = "履约时效汇总", description = DAYS_DESCRIPTION)
    public AjaxResult<AnalyticsFulfillmentSummaryVo> fulfillmentSummary(
            @Parameter(description = DAYS_DESCRIPTION)
            @RequestParam(defaultValue = "30") @Min(1) @Max(730) Integer days
    ) {
        return success(agentAnalyticsService.fulfillmentSummary(days));
    }

    /**
     * 获取指定时间范围内的售后处理时效汇总。
     *
     * @param days 最近天数
     * @return 售后处理时效汇总
     */
    @GetMapping("/after-sale-efficiency-summary")
    @Operation(summary = "售后处理时效汇总", description = DAYS_DESCRIPTION)
    public AjaxResult<AnalyticsAfterSaleEfficiencySummaryVo> afterSaleEfficiencySummary(
            @Parameter(description = DAYS_DESCRIPTION)
            @RequestParam(defaultValue = "30") @Min(1) @Max(730) Integer days
    ) {
        return success(agentAnalyticsService.afterSaleEfficiencySummary(days));
    }

    /**
     * 获取指定时间范围内的售后状态分布。
     *
     * @param days 最近天数
     * @return 售后状态分布
     */
    @GetMapping("/after-sale-status-distribution")
    @Operation(summary = "售后状态分布", description = DAYS_DESCRIPTION)
    public AjaxResult<List<AnalyticsStatusDistributionItemVo>> afterSaleStatusDistribution(
            @Parameter(description = DAYS_DESCRIPTION)
            @RequestParam(defaultValue = "30") @Min(1) @Max(730) Integer days
    ) {
        return success(agentAnalyticsService.afterSaleStatusDistribution(days));
    }

    /**
     * 获取指定时间范围内的售后原因分布。
     *
     * @param days 最近天数
     * @return 售后原因分布
     */
    @GetMapping("/after-sale-reason-distribution")
    @Operation(summary = "售后原因分布", description = DAYS_DESCRIPTION)
    public AjaxResult<List<AnalyticsReasonDistributionItemVo>> afterSaleReasonDistribution(
            @Parameter(description = DAYS_DESCRIPTION)
            @RequestParam(defaultValue = "30") @Min(1) @Max(730) Integer days
    ) {
        return success(agentAnalyticsService.afterSaleReasonDistribution(days));
    }

    /**
     * 获取指定时间范围内的热销商品排行。
     *
     * @param days  最近天数
     * @param limit 返回数量限制
     * @return 热销商品排行
     */
    @GetMapping("/top-selling-products")
    @Operation(summary = "热销商品排行", description = "支持 days 和 limit，days 默认30范围1-730，limit 默认10最大20")
    public AjaxResult<List<AnalyticsTopSellingProductVo>> topSellingProducts(
            @Parameter(description = DAYS_DESCRIPTION)
            @RequestParam(defaultValue = "30") @Min(1) @Max(730) Integer days,
            @Parameter(description = "返回数量限制，默认10，最大20")
            @RequestParam(defaultValue = "10") int limit
    ) {
        return success(agentAnalyticsService.topSellingProducts(days, limit));
    }

    /**
     * 获取指定时间范围内的退货退款风险商品排行。
     *
     * @param days  最近天数
     * @param limit 返回数量限制
     * @return 风险商品排行
     */
    @GetMapping("/return-refund-risk-products")
    @Operation(summary = "退货退款风险商品排行", description = "支持 days 和 limit，days 默认30范围1-730，limit 默认10最大20")
    public AjaxResult<List<AnalyticsReturnRefundRiskProductVo>> returnRefundRiskProducts(
            @Parameter(description = DAYS_DESCRIPTION)
            @RequestParam(defaultValue = "30") @Min(1) @Max(730) Integer days,
            @Parameter(description = "返回数量限制，默认10，最大20")
            @RequestParam(defaultValue = "10") int limit
    ) {
        return success(agentAnalyticsService.returnRefundRiskProducts(days, limit));
    }

    /**
     * 获取指定时间范围内的成交趋势。
     *
     * @param days 最近天数
     * @return 成交趋势
     */
    @GetMapping("/sales-trend")
    @Operation(summary = "成交趋势", description = DAYS_DESCRIPTION)
    public AjaxResult<AnalyticsSalesTrendVo> salesTrend(
            @Parameter(description = DAYS_DESCRIPTION)
            @RequestParam(defaultValue = "30") @Min(1) @Max(730) Integer days
    ) {
        return success(agentAnalyticsService.salesTrend(days));
    }

    /**
     * 获取指定时间范围内的售后趋势。
     *
     * @param days 最近天数
     * @return 售后趋势
     */
    @GetMapping("/after-sale-trend")
    @Operation(summary = "售后趋势", description = DAYS_DESCRIPTION)
    public AjaxResult<AnalyticsAfterSaleTrendVo> afterSaleTrend(
            @Parameter(description = DAYS_DESCRIPTION)
            @RequestParam(defaultValue = "30") @Min(1) @Max(730) Integer days
    ) {
        return success(agentAnalyticsService.afterSaleTrend(days));
    }
}
