package com.zhangyichuang.medicine.admin.service.impl;

import com.zhangyichuang.medicine.admin.mapper.AnalyticsMapper;
import com.zhangyichuang.medicine.model.vo.analytics.AnalyticsTrendPointVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTests {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-03-15T04:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Mock
    private AnalyticsMapper analyticsMapper;

    private AnalyticsServiceImpl analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsServiceImpl(analyticsMapper, FIXED_CLOCK);
    }

    @Test
    void salesTrend_WhenDailyRange_ShouldFillMissingPointsAndUseDeterministicWindow() {
        AnalyticsTrendPointVo trendPoint = new AnalyticsTrendPointVo();
        trendPoint.setLabel("2026-03-14");
        trendPoint.setPaidOrderCount(5L);
        trendPoint.setPaidAmount(new BigDecimal("128.50"));
        when(analyticsMapper.selectPaidTrend(any(), any(), anyString())).thenReturn(List.of(trendPoint));

        var salesTrend = analyticsService.salesTrend(3);

        assertEquals(3, salesTrend.getDays());
        assertEquals("DAY", salesTrend.getGranularity());
        assertEquals(List.of("2026-03-13", "2026-03-14", "2026-03-15"),
                salesTrend.getPoints().stream().map(point -> point.getLabel()).toList());
        assertEquals(0L, salesTrend.getPoints().get(0).getPaidOrderCount());
        assertEquals(5L, salesTrend.getPoints().get(1).getPaidOrderCount());
        assertEquals(new BigDecimal("128.50"), salesTrend.getPoints().get(1).getPaidAmount());
        assertEquals(BigDecimal.ZERO, salesTrend.getPoints().get(2).getPaidAmount());

        ArgumentCaptor<LocalDateTime> startTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> groupFormatCaptor = ArgumentCaptor.forClass(String.class);
        verify(analyticsMapper).selectPaidTrend(
                startTimeCaptor.capture(),
                endTimeCaptor.capture(),
                groupFormatCaptor.capture()
        );
        assertEquals(LocalDateTime.of(2026, 3, 13, 0, 0), startTimeCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 3, 16, 0, 0), endTimeCaptor.getValue());
        assertEquals("%Y-%m-%d", groupFormatCaptor.getValue());
    }

    @Test
    void salesTrend_WhenWeeklyRange_ShouldUseIsoWeekLabels() {
        AnalyticsTrendPointVo trendPoint = new AnalyticsTrendPointVo();
        trendPoint.setLabel("2026-W10");
        trendPoint.setPaidOrderCount(8L);
        trendPoint.setPaidAmount(new BigDecimal("256.00"));
        when(analyticsMapper.selectPaidTrend(any(), any(), anyString())).thenReturn(List.of(trendPoint));

        var salesTrend = analyticsService.salesTrend(60);

        assertEquals(60, salesTrend.getDays());
        assertEquals("WEEK", salesTrend.getGranularity());
        assertEquals(List.of(
                        "2026-W03",
                        "2026-W04",
                        "2026-W05",
                        "2026-W06",
                        "2026-W07",
                        "2026-W08",
                        "2026-W09",
                        "2026-W10",
                        "2026-W11"
                ),
                salesTrend.getPoints().stream().map(point -> point.getLabel()).toList());
        assertEquals(8L, salesTrend.getPoints().get(7).getPaidOrderCount());

        ArgumentCaptor<LocalDateTime> startTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> groupFormatCaptor = ArgumentCaptor.forClass(String.class);
        verify(analyticsMapper).selectPaidTrend(
                startTimeCaptor.capture(),
                endTimeCaptor.capture(),
                groupFormatCaptor.capture()
        );
        assertEquals(LocalDateTime.of(2026, 1, 15, 0, 0), startTimeCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 3, 16, 0, 0), endTimeCaptor.getValue());
        assertEquals("%x-W%v", groupFormatCaptor.getValue());
    }

    @Test
    void trend_ShouldMergeSalesAndAfterSaleSeriesByLabel() {
        AnalyticsTrendPointVo salesPoint = new AnalyticsTrendPointVo();
        salesPoint.setLabel("2026-03-14");
        salesPoint.setPaidOrderCount(2L);
        salesPoint.setPaidAmount(new BigDecimal("99.90"));
        when(analyticsMapper.selectPaidTrend(any(), any(), anyString())).thenReturn(List.of(salesPoint));

        AnalyticsTrendPointVo afterSalePoint = new AnalyticsTrendPointVo();
        afterSalePoint.setLabel("2026-03-15");
        afterSalePoint.setAfterSaleApplyCount(1L);
        afterSalePoint.setRefundAmount(new BigDecimal("12.50"));
        when(analyticsMapper.selectAfterSaleTrend(any(), any(), anyString())).thenReturn(List.of(afterSalePoint));

        var trend = analyticsService.trend(2);

        assertEquals(2, trend.getDays());
        assertEquals("DAY", trend.getGranularity());
        assertNotNull(trend.getPoints());
        assertEquals(2, trend.getPoints().size());
        assertEquals("2026-03-14", trend.getPoints().get(0).getLabel());
        assertEquals(2L, trend.getPoints().get(0).getPaidOrderCount());
        assertEquals(BigDecimal.ZERO, trend.getPoints().get(0).getRefundAmount());
        assertEquals("2026-03-15", trend.getPoints().get(1).getLabel());
        assertEquals(0L, trend.getPoints().get(1).getPaidOrderCount());
        assertEquals(1L, trend.getPoints().get(1).getAfterSaleApplyCount());
        assertEquals(new BigDecimal("12.50"), trend.getPoints().get(1).getRefundAmount());
    }
}
