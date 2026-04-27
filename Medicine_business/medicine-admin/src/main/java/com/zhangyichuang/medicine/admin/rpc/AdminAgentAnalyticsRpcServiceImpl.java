package com.zhangyichuang.medicine.admin.rpc;

import com.zhangyichuang.medicine.admin.service.AnalyticsService;
import com.zhangyichuang.medicine.model.vo.analytics.*;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentAnalyticsRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 管理端 Agent 运营分析 RPC Provider。
 */
@DubboService(interfaceClass = AdminAgentAnalyticsRpcService.class, group = "medicine-admin", version = "1.0.0")
@RequiredArgsConstructor
public class AdminAgentAnalyticsRpcServiceImpl implements AdminAgentAnalyticsRpcService {

    private final AnalyticsService analyticsService;

    @Override
    public AnalyticsRealtimeOverviewVo realtimeOverview() {
        return analyticsService.realtimeOverview();
    }

    @Override
    public AnalyticsRangeSummaryVo rangeSummary(Integer days) {
        return analyticsService.rangeSummary(days);
    }

    @Override
    public AnalyticsConversionSummaryVo conversionSummary(Integer days) {
        return analyticsService.conversionSummary(days);
    }

    @Override
    public AnalyticsFulfillmentSummaryVo fulfillmentSummary(Integer days) {
        return analyticsService.fulfillmentSummary(days);
    }

    @Override
    public AnalyticsAfterSaleEfficiencySummaryVo afterSaleEfficiencySummary(Integer days) {
        return analyticsService.afterSaleEfficiencySummary(days);
    }

    @Override
    public List<AnalyticsStatusDistributionItemVo> afterSaleStatusDistribution(Integer days) {
        return analyticsService.afterSaleStatusDistribution(days);
    }

    @Override
    public List<AnalyticsReasonDistributionItemVo> afterSaleReasonDistribution(Integer days) {
        return analyticsService.afterSaleReasonDistribution(days);
    }

    @Override
    public List<AnalyticsTopSellingProductVo> topSellingProducts(Integer days, int limit) {
        return analyticsService.topSellingProducts(days, limit);
    }

    @Override
    public List<AnalyticsReturnRefundRiskProductVo> returnRefundRiskProducts(Integer days, int limit) {
        return analyticsService.returnRefundRiskProducts(days, limit);
    }

    @Override
    public AnalyticsSalesTrendVo salesTrend(Integer days) {
        return analyticsService.salesTrend(days);
    }

    @Override
    public AnalyticsAfterSaleTrendVo afterSaleTrend(Integer days) {
        return analyticsService.afterSaleTrend(days);
    }
}
