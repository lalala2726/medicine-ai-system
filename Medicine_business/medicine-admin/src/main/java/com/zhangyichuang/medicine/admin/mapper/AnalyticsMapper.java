package com.zhangyichuang.medicine.admin.mapper;

import com.zhangyichuang.medicine.model.vo.analytics.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运营分析专用 Mapper，SQL 全部放在 XML 中。
 */
@Mapper
public interface AnalyticsMapper {

    AnalyticsRealtimeOverviewVo selectRealtimeOverview();

    AnalyticsRangeSummaryVo selectRangeSummary(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    AnalyticsConversionSummaryVo selectConversionSummary(@Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime);

    AnalyticsFulfillmentSummaryVo selectFulfillmentSummary(@Param("startTime") LocalDateTime startTime,
                                                           @Param("endTime") LocalDateTime endTime);

    AnalyticsAfterSaleEfficiencySummaryVo selectAfterSaleEfficiencySummary(@Param("startTime") LocalDateTime startTime,
                                                                           @Param("endTime") LocalDateTime endTime);

    List<AnalyticsStatusDistributionItemVo> selectAfterSaleStatusDistribution(@Param("startTime") LocalDateTime startTime,
                                                                              @Param("endTime") LocalDateTime endTime);

    List<AnalyticsReasonDistributionItemVo> selectAfterSaleReasonDistribution(@Param("startTime") LocalDateTime startTime,
                                                                              @Param("endTime") LocalDateTime endTime);

    List<AnalyticsTopSellingProductVo> selectTopSellingProducts(@Param("startTime") LocalDateTime startTime,
                                                                @Param("endTime") LocalDateTime endTime,
                                                                @Param("limit") int limit);

    List<AnalyticsReturnRefundRiskProductVo> selectReturnRefundRiskProducts(@Param("startTime") LocalDateTime startTime,
                                                                            @Param("endTime") LocalDateTime endTime,
                                                                            @Param("limit") int limit);

    List<AnalyticsTrendPointVo> selectPaidTrend(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                @Param("groupFormat") String groupFormat);

    List<AnalyticsTrendPointVo> selectAfterSaleTrend(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime,
                                                     @Param("groupFormat") String groupFormat);
}
