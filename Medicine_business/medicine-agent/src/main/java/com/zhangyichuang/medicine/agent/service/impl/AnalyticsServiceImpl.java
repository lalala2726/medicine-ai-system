package com.zhangyichuang.medicine.agent.service.impl;

import com.zhangyichuang.medicine.agent.service.AnalyticsService;
import com.zhangyichuang.medicine.model.vo.analytics.*;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentAnalyticsRpcService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Agent 运营分析 Dubbo Consumer 实现。
 */
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final int DEFAULT_DAYS = 30;

    @DubboReference(group = "medicine-admin", version = "1.0.0", check = false, timeout = 3000, retries = 0,
            url = "${dubbo.references.medicine-admin.url:}")
    private AdminAgentAnalyticsRpcService adminAgentAnalyticsRpcService;

    @Override
    public AnalyticsRealtimeOverviewVo realtimeOverview() {
        AnalyticsRealtimeOverviewVo realtimeOverview = adminAgentAnalyticsRpcService.realtimeOverview();
        return realtimeOverview == null ? new AnalyticsRealtimeOverviewVo() : realtimeOverview;
    }

    @Override
    public AnalyticsRangeSummaryVo rangeSummary(Integer days) {
        AnalyticsRangeSummaryVo rangeSummary = adminAgentAnalyticsRpcService.rangeSummary(normalizeDays(days));
        return rangeSummary == null ? new AnalyticsRangeSummaryVo() : rangeSummary;
    }

    @Override
    public AnalyticsConversionSummaryVo conversionSummary(Integer days) {
        AnalyticsConversionSummaryVo conversionSummary = adminAgentAnalyticsRpcService.conversionSummary(normalizeDays(days));
        return conversionSummary == null ? new AnalyticsConversionSummaryVo() : conversionSummary;
    }

    @Override
    public AnalyticsFulfillmentSummaryVo fulfillmentSummary(Integer days) {
        AnalyticsFulfillmentSummaryVo fulfillmentSummary =
                adminAgentAnalyticsRpcService.fulfillmentSummary(normalizeDays(days));
        return fulfillmentSummary == null ? new AnalyticsFulfillmentSummaryVo() : fulfillmentSummary;
    }

    @Override
    public AnalyticsAfterSaleEfficiencySummaryVo afterSaleEfficiencySummary(Integer days) {
        AnalyticsAfterSaleEfficiencySummaryVo summary =
                adminAgentAnalyticsRpcService.afterSaleEfficiencySummary(normalizeDays(days));
        return summary == null ? new AnalyticsAfterSaleEfficiencySummaryVo() : summary;
    }

    @Override
    public List<AnalyticsStatusDistributionItemVo> afterSaleStatusDistribution(Integer days) {
        return safeList(adminAgentAnalyticsRpcService.afterSaleStatusDistribution(normalizeDays(days)));
    }

    @Override
    public List<AnalyticsReasonDistributionItemVo> afterSaleReasonDistribution(Integer days) {
        return safeList(adminAgentAnalyticsRpcService.afterSaleReasonDistribution(normalizeDays(days)));
    }

    @Override
    public List<AnalyticsTopSellingProductVo> topSellingProducts(Integer days, int limit) {
        return safeList(adminAgentAnalyticsRpcService.topSellingProducts(normalizeDays(days), limit));
    }

    @Override
    public List<AnalyticsReturnRefundRiskProductVo> returnRefundRiskProducts(Integer days, int limit) {
        return safeList(adminAgentAnalyticsRpcService.returnRefundRiskProducts(normalizeDays(days), limit));
    }

    @Override
    public AnalyticsSalesTrendVo salesTrend(Integer days) {
        AnalyticsSalesTrendVo salesTrendVo = adminAgentAnalyticsRpcService.salesTrend(normalizeDays(days));
        if (salesTrendVo != null) {
            return salesTrendVo;
        }
        AnalyticsSalesTrendVo emptyTrend = new AnalyticsSalesTrendVo();
        emptyTrend.setPoints(Collections.emptyList());
        return emptyTrend;
    }

    @Override
    public AnalyticsAfterSaleTrendVo afterSaleTrend(Integer days) {
        AnalyticsAfterSaleTrendVo afterSaleTrendVo = adminAgentAnalyticsRpcService.afterSaleTrend(normalizeDays(days));
        if (afterSaleTrendVo != null) {
            return afterSaleTrendVo;
        }
        AnalyticsAfterSaleTrendVo emptyTrend = new AnalyticsAfterSaleTrendVo();
        emptyTrend.setPoints(Collections.emptyList());
        return emptyTrend;
    }

    private int normalizeDays(Integer days) {
        return days == null ? DEFAULT_DAYS : days;
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }
}
