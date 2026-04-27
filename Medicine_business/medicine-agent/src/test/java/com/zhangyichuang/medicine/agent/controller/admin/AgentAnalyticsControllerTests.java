package com.zhangyichuang.medicine.agent.controller.admin;

import com.zhangyichuang.medicine.agent.service.AnalyticsService;
import com.zhangyichuang.medicine.model.vo.analytics.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentAnalyticsControllerTests {

    private final StubAnalyticsService analyticsService = new StubAnalyticsService();
    private final AgentAnalyticsController controller = new AgentAnalyticsController(analyticsService);

    @Test
    void realtimeOverview_ShouldReturnServiceResult() {
        AnalyticsRealtimeOverviewVo overview = new AnalyticsRealtimeOverviewVo();
        overview.setTodayPaidOrderCount(3L);
        analyticsService.realtimeOverview = overview;

        var result = controller.realtimeOverview();

        assertEquals(200, result.getCode());
        assertSame(overview, result.getData());
    }

    @Test
    void rangeSummary_ShouldPassDaysToService() {
        AnalyticsRangeSummaryVo summary = new AnalyticsRangeSummaryVo();
        summary.setPaidOrderCount(12L);
        analyticsService.rangeSummary = summary;

        var result = controller.rangeSummary(45);

        assertEquals(200, result.getCode());
        assertEquals(45, analyticsService.lastDays);
        assertSame(summary, result.getData());
    }

    @Test
    void afterSaleStatusDistribution_ShouldReturnListFromService() {
        AnalyticsStatusDistributionItemVo item = new AnalyticsStatusDistributionItemVo();
        item.setStatus("PENDING");
        item.setCount(4L);
        analyticsService.statusDistribution = List.of(item);

        var result = controller.afterSaleStatusDistribution(30);

        assertEquals(200, result.getCode());
        assertEquals(30, analyticsService.lastDays);
        assertEquals(1, result.getData().size());
        assertEquals("PENDING", result.getData().getFirst().getStatus());
    }

    @Test
    void topSellingProducts_ShouldPassDaysAndLimitToService() {
        AnalyticsTopSellingProductVo product = new AnalyticsTopSellingProductVo();
        product.setProductName("维生素C片");
        product.setSoldQuantity(18L);
        analyticsService.topSellingProducts = List.of(product);

        var result = controller.topSellingProducts(15, 6);

        assertEquals(200, result.getCode());
        assertEquals(15, analyticsService.lastDays);
        assertEquals(6, analyticsService.lastLimit);
        assertEquals("维生素C片", result.getData().getFirst().getProductName());
    }

    @Test
    void returnRefundRiskProducts_ShouldPassDaysAndLimitToService() {
        AnalyticsReturnRefundRiskProductVo product = new AnalyticsReturnRefundRiskProductVo();
        product.setProductName("阿莫西林胶囊");
        product.setReturnRefundRate(new BigDecimal("0.2500"));
        analyticsService.returnRefundRiskProducts = List.of(product);

        var result = controller.returnRefundRiskProducts(60, 8);

        assertEquals(200, result.getCode());
        assertEquals(60, analyticsService.lastDays);
        assertEquals(8, analyticsService.lastLimit);
        assertEquals(new BigDecimal("0.2500"), result.getData().getFirst().getReturnRefundRate());
    }

    @Test
    void salesTrend_ShouldReturnTrendFromService() {
        AnalyticsSalesTrendPointVo point = new AnalyticsSalesTrendPointVo();
        point.setLabel("2026-03-15");
        point.setPaidOrderCount(5L);
        AnalyticsSalesTrendVo trendVo = new AnalyticsSalesTrendVo();
        trendVo.setPoints(List.of(point));
        analyticsService.salesTrend = trendVo;

        var result = controller.salesTrend(7);

        assertEquals(200, result.getCode());
        assertEquals(7, analyticsService.lastDays);
        assertNotNull(result.getData());
        assertEquals("2026-03-15", result.getData().getPoints().getFirst().getLabel());
    }

    @Test
    void afterSaleTrend_ShouldReturnTrendFromService() {
        AnalyticsAfterSaleTrendPointVo point = new AnalyticsAfterSaleTrendPointVo();
        point.setLabel("2026-03-15");
        point.setAfterSaleApplyCount(2L);
        AnalyticsAfterSaleTrendVo trendVo = new AnalyticsAfterSaleTrendVo();
        trendVo.setPoints(List.of(point));
        analyticsService.afterSaleTrend = trendVo;

        var result = controller.afterSaleTrend(14);

        assertEquals(200, result.getCode());
        assertEquals(14, analyticsService.lastDays);
        assertNotNull(result.getData());
        assertEquals(2L, result.getData().getPoints().getFirst().getAfterSaleApplyCount());
    }

    private static final class StubAnalyticsService implements AnalyticsService {

        private int lastDays;
        private int lastLimit;
        private AnalyticsRealtimeOverviewVo realtimeOverview = new AnalyticsRealtimeOverviewVo();
        private AnalyticsRangeSummaryVo rangeSummary = new AnalyticsRangeSummaryVo();
        private AnalyticsConversionSummaryVo conversionSummary = new AnalyticsConversionSummaryVo();
        private AnalyticsFulfillmentSummaryVo fulfillmentSummary = new AnalyticsFulfillmentSummaryVo();
        private AnalyticsAfterSaleEfficiencySummaryVo afterSaleEfficiencySummary =
                new AnalyticsAfterSaleEfficiencySummaryVo();
        private List<AnalyticsStatusDistributionItemVo> statusDistribution = List.of();
        private List<AnalyticsReasonDistributionItemVo> reasonDistribution = List.of();
        private List<AnalyticsTopSellingProductVo> topSellingProducts = List.of();
        private List<AnalyticsReturnRefundRiskProductVo> returnRefundRiskProducts = List.of();
        private AnalyticsSalesTrendVo salesTrend = new AnalyticsSalesTrendVo();
        private AnalyticsAfterSaleTrendVo afterSaleTrend = new AnalyticsAfterSaleTrendVo();

        @Override
        public AnalyticsRealtimeOverviewVo realtimeOverview() {
            return realtimeOverview;
        }

        @Override
        public AnalyticsRangeSummaryVo rangeSummary(Integer days) {
            lastDays = days;
            return rangeSummary;
        }

        @Override
        public AnalyticsConversionSummaryVo conversionSummary(Integer days) {
            lastDays = days;
            return conversionSummary;
        }

        @Override
        public AnalyticsFulfillmentSummaryVo fulfillmentSummary(Integer days) {
            lastDays = days;
            return fulfillmentSummary;
        }

        @Override
        public AnalyticsAfterSaleEfficiencySummaryVo afterSaleEfficiencySummary(Integer days) {
            lastDays = days;
            return afterSaleEfficiencySummary;
        }

        @Override
        public List<AnalyticsStatusDistributionItemVo> afterSaleStatusDistribution(Integer days) {
            lastDays = days;
            return statusDistribution;
        }

        @Override
        public List<AnalyticsReasonDistributionItemVo> afterSaleReasonDistribution(Integer days) {
            lastDays = days;
            return reasonDistribution;
        }

        @Override
        public List<AnalyticsTopSellingProductVo> topSellingProducts(Integer days, int limit) {
            lastDays = days;
            lastLimit = limit;
            return topSellingProducts;
        }

        @Override
        public List<AnalyticsReturnRefundRiskProductVo> returnRefundRiskProducts(Integer days, int limit) {
            lastDays = days;
            lastLimit = limit;
            return returnRefundRiskProducts;
        }

        @Override
        public AnalyticsSalesTrendVo salesTrend(Integer days) {
            lastDays = days;
            return salesTrend;
        }

        @Override
        public AnalyticsAfterSaleTrendVo afterSaleTrend(Integer days) {
            lastDays = days;
            return afterSaleTrend;
        }
    }
}
