package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 运营看板响应。
 */
@Data
@Schema(description = "运营看板响应")
public class AnalyticsDashboardVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "实时运营总览")
    private AnalyticsRealtimeOverviewVo realtimeOverview;

    @Schema(description = "所选范围汇总")
    private AnalyticsRangeSummaryVo rangeSummary;

    @Schema(description = "订单支付转化汇总")
    private AnalyticsConversionSummaryVo conversionSummary;

    @Schema(description = "履约时效汇总")
    private AnalyticsFulfillmentSummaryVo fulfillmentSummary;

    @Schema(description = "售后处理时效汇总")
    private AnalyticsAfterSaleEfficiencySummaryVo afterSaleEfficiencySummary;

    @Schema(description = "售后状态分布")
    private List<AnalyticsStatusDistributionItemVo> afterSaleStatusDistribution;

    @Schema(description = "售后原因分布")
    private List<AnalyticsReasonDistributionItemVo> afterSaleReasonDistribution;

    @Schema(description = "热销商品")
    private List<AnalyticsTopSellingProductVo> topSellingProducts;

    @Schema(description = "退货退款风险商品")
    private List<AnalyticsReturnRefundRiskProductVo> returnRefundRiskProducts;
}
