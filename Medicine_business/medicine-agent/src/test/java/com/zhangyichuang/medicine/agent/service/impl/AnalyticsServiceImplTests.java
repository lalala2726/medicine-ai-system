package com.zhangyichuang.medicine.agent.service.impl;

import com.zhangyichuang.medicine.model.vo.analytics.AnalyticsAfterSaleTrendVo;
import com.zhangyichuang.medicine.model.vo.analytics.AnalyticsSalesTrendVo;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentAnalyticsRpcService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTests {

    @Mock
    private AdminAgentAnalyticsRpcService adminAgentAnalyticsRpcService;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    void salesTrend_WhenRpcReturnsNull_ShouldReturnEmptyPointsList() {
        when(adminAgentAnalyticsRpcService.salesTrend(anyInt())).thenReturn(null);

        AnalyticsSalesTrendVo salesTrend = analyticsService.salesTrend(30);

        assertNotNull(salesTrend);
        assertNotNull(salesTrend.getPoints());
        assertEquals(List.of(), salesTrend.getPoints());
    }

    @Test
    void afterSaleTrend_WhenRpcReturnsNull_ShouldReturnEmptyPointsList() {
        when(adminAgentAnalyticsRpcService.afterSaleTrend(anyInt())).thenReturn(null);

        AnalyticsAfterSaleTrendVo afterSaleTrend = analyticsService.afterSaleTrend(30);

        assertNotNull(afterSaleTrend);
        assertNotNull(afterSaleTrend.getPoints());
        assertEquals(List.of(), afterSaleTrend.getPoints());
    }
}
