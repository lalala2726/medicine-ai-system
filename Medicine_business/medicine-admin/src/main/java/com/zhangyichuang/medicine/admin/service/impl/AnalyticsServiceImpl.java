package com.zhangyichuang.medicine.admin.service.impl;

import com.zhangyichuang.medicine.admin.mapper.AnalyticsMapper;
import com.zhangyichuang.medicine.admin.service.AnalyticsService;
import com.zhangyichuang.medicine.model.enums.AfterSaleReasonEnum;
import com.zhangyichuang.medicine.model.enums.AfterSaleStatusEnum;
import com.zhangyichuang.medicine.model.enums.AnalyticsTrendGranularityEnum;
import com.zhangyichuang.medicine.model.vo.analytics.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商城运营分析服务实现类。
 * <p>
 * 负责组装运营看板和趋势图数据，并统一处理默认值、比率计算、时间范围解析与趋势补零逻辑。
 */
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final int DEFAULT_TOP_LIMIT = 10;
    private static final int MAX_TOP_LIMIT = 20;
    private static final int DEFAULT_DAYS = 30;
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 730;
    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final WeekFields WEEK_FIELDS = WeekFields.ISO;

    private final AnalyticsMapper analyticsMapper;
    private final Clock clock;

    /**
     * 获取实时运营总览。
     *
     * @return 返回累计成交、今日成交、待发货和售后待处理等实时总览指标
     */
    @Override
    public AnalyticsRealtimeOverviewVo realtimeOverview() {
        return normalizeRealtimeOverview(analyticsMapper.selectRealtimeOverview());
    }

    /**
     * 获取指定时间范围内的经营结果汇总。
     *
     * @param days 最近天数
     * @return 返回成交金额、退款金额、净成交额、退款率等汇总指标
     */
    @Override
    public AnalyticsRangeSummaryVo rangeSummary(Integer days) {
        RangeWindow rangeWindow = resolveRangeWindow(days);
        return normalizeRangeSummary(analyticsMapper.selectRangeSummary(rangeWindow.startTime(), rangeWindow.endTime()));
    }

    /**
     * 获取指定时间范围内的支付转化汇总。
     *
     * @param days 最近天数
     * @return 返回下单数、支付数、待支付数、关闭数和支付转化率
     */
    @Override
    public AnalyticsConversionSummaryVo conversionSummary(Integer days) {
        RangeWindow rangeWindow = resolveRangeWindow(days);
        return normalizeConversionSummary(
                analyticsMapper.selectConversionSummary(rangeWindow.startTime(), rangeWindow.endTime()));
    }

    /**
     * 获取指定时间范围内的履约时效汇总。
     *
     * @param days 最近天数
     * @return 返回平均发货时长、平均收货时长和超时履约统计
     */
    @Override
    public AnalyticsFulfillmentSummaryVo fulfillmentSummary(Integer days) {
        RangeWindow rangeWindow = resolveRangeWindow(days);
        return normalizeFulfillmentSummary(
                analyticsMapper.selectFulfillmentSummary(rangeWindow.startTime(), rangeWindow.endTime()));
    }

    /**
     * 获取指定时间范围内的售后处理时效汇总。
     *
     * @param days 最近天数
     * @return 返回平均审核时长、平均完结时长和超时售后统计
     */
    @Override
    public AnalyticsAfterSaleEfficiencySummaryVo afterSaleEfficiencySummary(Integer days) {
        RangeWindow rangeWindow = resolveRangeWindow(days);
        return normalizeAfterSaleEfficiencySummary(
                analyticsMapper.selectAfterSaleEfficiencySummary(rangeWindow.startTime(), rangeWindow.endTime()));
    }

    /**
     * 获取指定时间范围内的售后状态分布。
     *
     * @param days 最近天数
     * @return 返回售后状态及对应数量列表
     */
    @Override
    public List<AnalyticsStatusDistributionItemVo> afterSaleStatusDistribution(Integer days) {
        RangeWindow rangeWindow = resolveRangeWindow(days);
        return normalizeAfterSaleStatusDistribution(
                analyticsMapper.selectAfterSaleStatusDistribution(rangeWindow.startTime(), rangeWindow.endTime()));
    }

    /**
     * 获取指定时间范围内的售后原因分布。
     *
     * @param days 最近天数
     * @return 返回售后原因及对应数量列表
     */
    @Override
    public List<AnalyticsReasonDistributionItemVo> afterSaleReasonDistribution(Integer days) {
        RangeWindow rangeWindow = resolveRangeWindow(days);
        return normalizeAfterSaleReasonDistribution(
                analyticsMapper.selectAfterSaleReasonDistribution(rangeWindow.startTime(), rangeWindow.endTime()));
    }

    /**
     * 获取指定时间范围内的热销商品排行。
     *
     * @param days  最近天数
     * @param limit 返回数量限制
     * @return 返回按销量降序排列的热销商品列表
     */
    @Override
    public List<AnalyticsTopSellingProductVo> topSellingProducts(Integer days, int limit) {
        RangeWindow rangeWindow = resolveRangeWindow(days);
        return normalizeTopSellingProducts(analyticsMapper.selectTopSellingProducts(
                rangeWindow.startTime(), rangeWindow.endTime(), normalizeTopLimit(limit)));
    }

    /**
     * 获取指定时间范围内的退货退款风险商品排行。
     *
     * @param days  最近天数
     * @param limit 返回数量限制
     * @return 返回按风险程度排序的退货退款商品列表
     */
    @Override
    public List<AnalyticsReturnRefundRiskProductVo> returnRefundRiskProducts(Integer days, int limit) {
        RangeWindow rangeWindow = resolveRangeWindow(days);
        return normalizeRiskProducts(analyticsMapper.selectReturnRefundRiskProducts(
                rangeWindow.startTime(), rangeWindow.endTime(), normalizeTopLimit(limit)));
    }

    /**
     * 获取指定时间范围内的成交趋势。
     *
     * @param days 最近天数
     * @return 返回仅包含成交相关字段的趋势点位列表
     */
    @Override
    public AnalyticsSalesTrendVo salesTrend(Integer days) {
        RangeWindow rangeWindow = resolveRangeWindow(days);
        LinkedHashMap<String, AnalyticsTrendPointVo> pointMap = buildEmptyTrendMap(rangeWindow.labels());
        mergePaidTrend(pointMap, analyticsMapper.selectPaidTrend(
                rangeWindow.startTime(), rangeWindow.endTime(), rangeWindow.groupFormat()));
        return buildSalesTrend(rangeWindow, pointMap);
    }

    /**
     * 获取指定时间范围内的售后趋势。
     *
     * @param days 最近天数
     * @return 返回仅包含售后相关字段的趋势点位列表
     */
    @Override
    public AnalyticsAfterSaleTrendVo afterSaleTrend(Integer days) {
        RangeWindow rangeWindow = resolveRangeWindow(days);
        LinkedHashMap<String, AnalyticsTrendPointVo> pointMap = buildEmptyTrendMap(rangeWindow.labels());
        mergeAfterSaleTrend(pointMap, analyticsMapper.selectAfterSaleTrend(
                rangeWindow.startTime(), rangeWindow.endTime(), rangeWindow.groupFormat()));
        return buildAfterSaleTrend(rangeWindow, pointMap);
    }

    /**
     * 获取运营看板聚合数据。
     *
     * @param days 最近天数
     * @return 返回指定时间范围内的运营看板聚合结果
     */
    @Override
    public AnalyticsDashboardVo dashboard(Integer days) {
        AnalyticsDashboardVo dashboardVo = new AnalyticsDashboardVo();
        dashboardVo.setRealtimeOverview(realtimeOverview());
        dashboardVo.setRangeSummary(rangeSummary(days));
        dashboardVo.setConversionSummary(conversionSummary(days));
        dashboardVo.setFulfillmentSummary(fulfillmentSummary(days));
        dashboardVo.setAfterSaleEfficiencySummary(afterSaleEfficiencySummary(days));
        dashboardVo.setAfterSaleStatusDistribution(afterSaleStatusDistribution(days));
        dashboardVo.setAfterSaleReasonDistribution(afterSaleReasonDistribution(days));
        dashboardVo.setTopSellingProducts(topSellingProducts(days, DEFAULT_TOP_LIMIT));
        dashboardVo.setReturnRefundRiskProducts(returnRefundRiskProducts(days, DEFAULT_TOP_LIMIT));
        return dashboardVo;
    }

    /**
     * 获取运营趋势图数据。
     *
     * @param days 最近天数
     * @return 返回指定时间范围内的趋势点位列表，缺失点位会自动补零
     */
    @Override
    public AnalyticsTrendVo trend(Integer days) {
        AnalyticsSalesTrendVo salesTrendVo = salesTrend(days);
        AnalyticsAfterSaleTrendVo afterSaleTrendVo = afterSaleTrend(days);
        AnalyticsTrendVo trendVo = new AnalyticsTrendVo();
        trendVo.setDays(salesTrendVo.getDays());
        trendVo.setGranularity(salesTrendVo.getGranularity());
        trendVo.setPoints(mergeTrendPoints(salesTrendVo, afterSaleTrendVo));
        return trendVo;
    }

    /**
     * 归一化实时总览数据，确保数值字段不会出现空值。
     *
     * @param source Mapper 查询返回的实时总览数据
     * @return 返回补齐默认值后的实时总览对象
     */
    private AnalyticsRealtimeOverviewVo normalizeRealtimeOverview(AnalyticsRealtimeOverviewVo source) {
        AnalyticsRealtimeOverviewVo target = source == null ? new AnalyticsRealtimeOverviewVo() : source;
        target.setCumulativePaidAmount(defaultBigDecimal(target.getCumulativePaidAmount()));
        target.setCumulativePaidOrderCount(defaultLong(target.getCumulativePaidOrderCount()));
        target.setTodayPaidAmount(defaultBigDecimal(target.getTodayPaidAmount()));
        target.setTodayPaidOrderCount(defaultLong(target.getTodayPaidOrderCount()));
        target.setPendingShipmentOrderCount(defaultLong(target.getPendingShipmentOrderCount()));
        target.setPendingReceiptOrderCount(defaultLong(target.getPendingReceiptOrderCount()));
        target.setPendingAfterSaleCount(defaultLong(target.getPendingAfterSaleCount()));
        target.setProcessingAfterSaleCount(defaultLong(target.getProcessingAfterSaleCount()));
        return target;
    }

    /**
     * 归一化范围汇总数据，并补充净成交额和退款率等派生指标。
     *
     * @param source Mapper 查询返回的范围汇总数据
     * @return 返回补齐默认值并计算派生字段后的范围汇总对象
     */
    private AnalyticsRangeSummaryVo normalizeRangeSummary(AnalyticsRangeSummaryVo source) {
        AnalyticsRangeSummaryVo target = source == null ? new AnalyticsRangeSummaryVo() : source;
        target.setPaidAmount(defaultBigDecimal(target.getPaidAmount()));
        target.setPaidOrderCount(defaultLong(target.getPaidOrderCount()));
        target.setAverageOrderAmount(defaultBigDecimal(target.getAverageOrderAmount()));
        target.setRefundAmount(defaultBigDecimal(target.getRefundAmount()));
        target.setNetPaidAmount(target.getPaidAmount().subtract(target.getRefundAmount()));
        target.setRefundRate(calculateAmountRate(target.getRefundAmount(), target.getPaidAmount()));
        target.setAfterSaleApplyCount(defaultLong(target.getAfterSaleApplyCount()));
        target.setReturnRefundQuantity(defaultLong(target.getReturnRefundQuantity()));
        return target;
    }

    /**
     * 归一化支付转化汇总数据，并计算支付转化率。
     *
     * @param source Mapper 查询返回的支付转化数据
     * @return 返回补齐默认值并计算转化率后的支付转化汇总对象
     */
    private AnalyticsConversionSummaryVo normalizeConversionSummary(AnalyticsConversionSummaryVo source) {
        AnalyticsConversionSummaryVo target = source == null ? new AnalyticsConversionSummaryVo() : source;
        target.setCreatedOrderCount(defaultLong(target.getCreatedOrderCount()));
        target.setPaidOrderCount(defaultLong(target.getPaidOrderCount()));
        target.setPendingPaymentOrderCount(defaultLong(target.getPendingPaymentOrderCount()));
        target.setClosedOrderCount(defaultLong(target.getClosedOrderCount()));
        target.setPaymentConversionRate(calculateCountRate(target.getPaidOrderCount(), target.getCreatedOrderCount()));
        return target;
    }

    /**
     * 归一化履约时效汇总数据。
     *
     * @param source Mapper 查询返回的履约时效数据
     * @return 返回补齐默认值后的履约时效汇总对象
     */
    private AnalyticsFulfillmentSummaryVo normalizeFulfillmentSummary(AnalyticsFulfillmentSummaryVo source) {
        AnalyticsFulfillmentSummaryVo target = source == null ? new AnalyticsFulfillmentSummaryVo() : source;
        target.setAverageShipmentHours(defaultBigDecimal(target.getAverageShipmentHours()));
        target.setAverageReceiptHours(defaultBigDecimal(target.getAverageReceiptHours()));
        target.setOverdueShipmentOrderCount(defaultLong(target.getOverdueShipmentOrderCount()));
        target.setOverdueReceiptOrderCount(defaultLong(target.getOverdueReceiptOrderCount()));
        return target;
    }

    /**
     * 归一化售后处理时效数据。
     *
     * @param source Mapper 查询返回的售后时效数据
     * @return 返回补齐默认值后的售后处理时效汇总对象
     */
    private AnalyticsAfterSaleEfficiencySummaryVo normalizeAfterSaleEfficiencySummary(
            AnalyticsAfterSaleEfficiencySummaryVo source
    ) {
        AnalyticsAfterSaleEfficiencySummaryVo target =
                source == null ? new AnalyticsAfterSaleEfficiencySummaryVo() : source;
        target.setAverageAuditHours(defaultBigDecimal(target.getAverageAuditHours()));
        target.setAverageCompleteHours(defaultBigDecimal(target.getAverageCompleteHours()));
        target.setOverdueAuditCount(defaultLong(target.getOverdueAuditCount()));
        target.setOverdueCompleteCount(defaultLong(target.getOverdueCompleteCount()));
        return target;
    }

    /**
     * 归一化售后状态分布数据，并补充状态中文名称。
     *
     * @param source Mapper 查询返回的售后状态分布列表
     * @return 返回补齐默认值并补充中文名称后的售后状态分布列表
     */
    private List<AnalyticsStatusDistributionItemVo> normalizeAfterSaleStatusDistribution(
            List<AnalyticsStatusDistributionItemVo> source
    ) {
        if (CollectionUtils.isEmpty(source)) {
            return List.of();
        }
        source.forEach(item -> {
            item.setCount(defaultLong(item.getCount()));
            AfterSaleStatusEnum statusEnum = AfterSaleStatusEnum.fromCode(item.getStatus());
            item.setStatusName(statusEnum == null ? "未知" : statusEnum.getName());
        });
        return source;
    }

    /**
     * 归一化售后原因分布数据，并补充原因中文名称。
     *
     * @param source Mapper 查询返回的售后原因分布列表
     * @return 返回补齐默认值并补充中文名称后的售后原因分布列表
     */
    private List<AnalyticsReasonDistributionItemVo> normalizeAfterSaleReasonDistribution(
            List<AnalyticsReasonDistributionItemVo> source
    ) {
        if (CollectionUtils.isEmpty(source)) {
            return List.of();
        }
        source.forEach(item -> {
            item.setCount(defaultLong(item.getCount()));
            AfterSaleReasonEnum reasonEnum = AfterSaleReasonEnum.fromCode(item.getReason());
            item.setReasonName(reasonEnum == null ? "未知" : reasonEnum.getName());
        });
        return source;
    }

    /**
     * 归一化热销商品排行数据。
     *
     * @param source Mapper 查询返回的热销商品列表
     * @return 返回补齐默认值后的热销商品列表
     */
    private List<AnalyticsTopSellingProductVo> normalizeTopSellingProducts(List<AnalyticsTopSellingProductVo> source) {
        if (CollectionUtils.isEmpty(source)) {
            return List.of();
        }
        source.forEach(item -> {
            item.setSoldQuantity(defaultLong(item.getSoldQuantity()));
            item.setPaidAmount(defaultBigDecimal(item.getPaidAmount()));
        });
        return source;
    }

    /**
     * 归一化退货退款风险商品数据，并计算退货退款率。
     *
     * @param source Mapper 查询返回的风险商品列表
     * @return 返回补齐默认值并计算风险率后的商品列表
     */
    private List<AnalyticsReturnRefundRiskProductVo> normalizeRiskProducts(
            List<AnalyticsReturnRefundRiskProductVo> source
    ) {
        if (CollectionUtils.isEmpty(source)) {
            return List.of();
        }
        source.forEach(item -> {
            long soldQuantity = defaultLong(item.getSoldQuantity());
            long returnRefundQuantity = defaultLong(item.getReturnRefundQuantity());
            item.setSoldQuantity(soldQuantity);
            item.setReturnRefundQuantity(returnRefundQuantity);
            item.setRefundAmount(defaultBigDecimal(item.getRefundAmount()));
            if (soldQuantity <= 0L) {
                item.setReturnRefundRate(BigDecimal.ZERO);
            } else {
                item.setReturnRefundRate(BigDecimal.valueOf(returnRefundQuantity)
                        .divide(BigDecimal.valueOf(soldQuantity), 4, RoundingMode.HALF_UP));
            }
        });
        return source;
    }

    /**
     * 根据完整标签列表构建空趋势点位映射，用于后续补零。
     *
     * @param labels 已按时间顺序生成的趋势标签列表
     * @return 返回按标签顺序初始化完成的趋势点位映射
     */
    private LinkedHashMap<String, AnalyticsTrendPointVo> buildEmptyTrendMap(List<String> labels) {
        LinkedHashMap<String, AnalyticsTrendPointVo> pointMap = new LinkedHashMap<>();
        for (String label : labels) {
            AnalyticsTrendPointVo point = new AnalyticsTrendPointVo();
            point.setLabel(label);
            point.setPaidOrderCount(0L);
            point.setPaidAmount(BigDecimal.ZERO);
            point.setRefundAmount(BigDecimal.ZERO);
            point.setAfterSaleApplyCount(0L);
            pointMap.put(label, point);
        }
        return pointMap;
    }

    /**
     * 将成交趋势数据合并到趋势点位映射中。
     *
     * @param pointMap  预先构建好的趋势点位映射
     * @param paidTrend Mapper 查询返回的成交趋势列表
     */
    private void mergePaidTrend(Map<String, AnalyticsTrendPointVo> pointMap, List<AnalyticsTrendPointVo> paidTrend) {
        if (CollectionUtils.isEmpty(paidTrend)) {
            return;
        }
        paidTrend.forEach(item -> {
            AnalyticsTrendPointVo point = pointMap.get(item.getLabel());
            if (point == null) {
                return;
            }
            point.setPaidOrderCount(defaultLong(item.getPaidOrderCount()));
            point.setPaidAmount(defaultBigDecimal(item.getPaidAmount()));
        });
    }

    /**
     * 将售后趋势数据合并到趋势点位映射中。
     *
     * @param pointMap       预先构建好的趋势点位映射
     * @param afterSaleTrend Mapper 查询返回的售后趋势列表
     */
    private void mergeAfterSaleTrend(Map<String, AnalyticsTrendPointVo> pointMap, List<AnalyticsTrendPointVo> afterSaleTrend) {
        if (CollectionUtils.isEmpty(afterSaleTrend)) {
            return;
        }
        afterSaleTrend.forEach(item -> {
            AnalyticsTrendPointVo point = pointMap.get(item.getLabel());
            if (point == null) {
                return;
            }
            point.setRefundAmount(defaultBigDecimal(item.getRefundAmount()));
            point.setAfterSaleApplyCount(defaultLong(item.getAfterSaleApplyCount()));
        });
    }

    /**
     * 构建仅包含成交字段的 AI 趋势响应。
     *
     * @param rangeWindow 当前时间窗口
     * @param pointMap    已补零并合并成交数据的趋势点位映射
     * @return 返回成交趋势响应
     */
    private AnalyticsSalesTrendVo buildSalesTrend(
            RangeWindow rangeWindow,
            Map<String, AnalyticsTrendPointVo> pointMap
    ) {
        List<AnalyticsSalesTrendPointVo> points = new ArrayList<>(pointMap.size());
        pointMap.values().forEach(point -> {
            AnalyticsSalesTrendPointVo salesPoint = new AnalyticsSalesTrendPointVo();
            salesPoint.setLabel(point.getLabel());
            salesPoint.setPaidOrderCount(defaultLong(point.getPaidOrderCount()));
            salesPoint.setPaidAmount(defaultBigDecimal(point.getPaidAmount()));
            points.add(salesPoint);
        });
        AnalyticsSalesTrendVo trendVo = new AnalyticsSalesTrendVo();
        trendVo.setDays(rangeWindow.days());
        trendVo.setGranularity(rangeWindow.granularity().getCode());
        trendVo.setPoints(points);
        return trendVo;
    }

    /**
     * 构建仅包含售后字段的 AI 趋势响应。
     *
     * @param rangeWindow 当前时间窗口
     * @param pointMap    已补零并合并售后数据的趋势点位映射
     * @return 返回售后趋势响应
     */
    private AnalyticsAfterSaleTrendVo buildAfterSaleTrend(
            RangeWindow rangeWindow,
            Map<String, AnalyticsTrendPointVo> pointMap
    ) {
        List<AnalyticsAfterSaleTrendPointVo> points = new ArrayList<>(pointMap.size());
        pointMap.values().forEach(point -> {
            AnalyticsAfterSaleTrendPointVo afterSalePoint = new AnalyticsAfterSaleTrendPointVo();
            afterSalePoint.setLabel(point.getLabel());
            afterSalePoint.setRefundAmount(defaultBigDecimal(point.getRefundAmount()));
            afterSalePoint.setAfterSaleApplyCount(defaultLong(point.getAfterSaleApplyCount()));
            points.add(afterSalePoint);
        });
        AnalyticsAfterSaleTrendVo trendVo = new AnalyticsAfterSaleTrendVo();
        trendVo.setDays(rangeWindow.days());
        trendVo.setGranularity(rangeWindow.granularity().getCode());
        trendVo.setPoints(points);
        return trendVo;
    }

    /**
     * 合并成交趋势与售后趋势，供管理端看板复用。
     *
     * @param salesTrendVo     成交趋势响应
     * @param afterSaleTrendVo 售后趋势响应
     * @return 返回包含成交与售后字段的完整趋势点位列表
     */
    private List<AnalyticsTrendPointVo> mergeTrendPoints(
            AnalyticsSalesTrendVo salesTrendVo,
            AnalyticsAfterSaleTrendVo afterSaleTrendVo
    ) {
        List<AnalyticsTrendPointVo> mergedPoints = new ArrayList<>(salesTrendVo.getPoints().size());
        for (int index = 0; index < salesTrendVo.getPoints().size(); index++) {
            AnalyticsSalesTrendPointVo salesPoint = salesTrendVo.getPoints().get(index);
            AnalyticsAfterSaleTrendPointVo afterSalePoint = afterSaleTrendVo.getPoints().get(index);
            AnalyticsTrendPointVo mergedPoint = new AnalyticsTrendPointVo();
            mergedPoint.setLabel(salesPoint.getLabel());
            mergedPoint.setPaidOrderCount(defaultLong(salesPoint.getPaidOrderCount()));
            mergedPoint.setPaidAmount(defaultBigDecimal(salesPoint.getPaidAmount()));
            mergedPoint.setRefundAmount(defaultBigDecimal(afterSalePoint.getRefundAmount()));
            mergedPoint.setAfterSaleApplyCount(defaultLong(afterSalePoint.getAfterSaleApplyCount()));
            mergedPoints.add(mergedPoint);
        }
        return mergedPoints;
    }

    /**
     * 解析最近天数并构建对应的时间窗口配置。
     *
     * @param days 最近天数
     * @return 返回包含起止时间、粒度、分组格式和完整标签的时间窗口对象
     */
    private RangeWindow resolveRangeWindow(Integer days) {
        int resolvedDays = normalizeDays(days);
        if (resolvedDays <= 31) {
            return buildDailyWindow(resolvedDays);
        }
        if (resolvedDays <= 120) {
            return buildWeeklyWindow(resolvedDays);
        }
        return buildMonthlyWindow(resolvedDays);
    }

    /**
     * 构建按日统计的时间窗口。
     *
     * @param days 需要覆盖的自然日数量
     * @return 返回按日聚合使用的时间窗口对象
     */
    private RangeWindow buildDailyWindow(int days) {
        LocalDate today = LocalDate.now(clock);
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1L).atStartOfDay();
        List<String> labels = new ArrayList<>(days);
        for (int index = 0; index < days; index++) {
            labels.add(startDate.plusDays(index).format(DAY_LABEL_FORMATTER));
        }
        return new RangeWindow(days, AnalyticsTrendGranularityEnum.DAY, startTime, endTime, "%Y-%m-%d", labels);
    }

    /**
     * 构建按周统计的时间窗口。
     *
     * @param days 需要覆盖的自然日数量
     * @return 返回按周聚合使用的时间窗口对象
     */
    private RangeWindow buildWeeklyWindow(int days) {
        LocalDate today = LocalDate.now(clock);
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1L).atStartOfDay();
        List<String> labels = new ArrayList<>();
        LocalDate cursor = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!cursor.isAfter(lastWeekStart)) {
            labels.add(formatWeekLabel(cursor));
            cursor = cursor.plusWeeks(1L);
        }
        return new RangeWindow(days, AnalyticsTrendGranularityEnum.WEEK, startTime, endTime, "%x-W%v", labels);
    }

    /**
     * 构建按月统计的时间窗口。
     *
     * @param days 需要覆盖的自然日数量
     * @return 返回按月聚合使用的时间窗口对象
     */
    private RangeWindow buildMonthlyWindow(int days) {
        LocalDate today = LocalDate.now(clock);
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1L).atStartOfDay();
        List<String> labels = new ArrayList<>();
        LocalDate cursor = startDate.withDayOfMonth(1);
        LocalDate lastMonth = today.withDayOfMonth(1);
        while (!cursor.isAfter(lastMonth)) {
            labels.add(cursor.format(MONTH_LABEL_FORMATTER));
            cursor = cursor.plusMonths(1L);
        }
        return new RangeWindow(days, AnalyticsTrendGranularityEnum.MONTH, startTime, endTime, "%Y-%m", labels);
    }

    /**
     * 将指定日期格式化为 ISO 周标签。
     *
     * @param date 需要格式化的日期
     * @return 返回形如 {@code 2026-W11} 的周标签
     */
    private String formatWeekLabel(LocalDate date) {
        int weekBasedYear = date.get(WEEK_FIELDS.weekBasedYear());
        int weekOfYear = date.get(WEEK_FIELDS.weekOfWeekBasedYear());
        return String.format("%d-W%02d", weekBasedYear, weekOfYear);
    }

    /**
     * 将 Long 类型数值转换为非空 long，空值时返回 0。
     *
     * @param value 待处理的 Long 数值
     * @return 返回非空的 long 数值
     */
    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 将 BigDecimal 类型数值转换为非空对象，空值时返回 0。
     *
     * @param value 待处理的金额或比率数值
     * @return 返回非空的 BigDecimal 数值
     */
    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 计算金额类指标比率。
     *
     * @param numerator   分子金额
     * @param denominator 分母金额
     * @return 返回四位小数的金额比率；当分母小于等于 0 时返回 0
     */
    private BigDecimal calculateAmountRate(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal normalizedDenominator = defaultBigDecimal(denominator);
        if (normalizedDenominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return defaultBigDecimal(numerator).divide(normalizedDenominator, 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算数量类指标比率。
     *
     * @param numerator   分子数量
     * @param denominator 分母数量
     * @return 返回四位小数的数量比率；当分母小于等于 0 时返回 0
     */
    private BigDecimal calculateCountRate(Long numerator, Long denominator) {
        long denominatorValue = denominator == null ? 0L : denominator;
        if (denominatorValue <= 0L) {
            return BigDecimal.ZERO;
        }
        long numeratorValue = numerator == null ? 0L : numerator;
        return BigDecimal.valueOf(numeratorValue)
                .divide(BigDecimal.valueOf(denominatorValue), 4, RoundingMode.HALF_UP);
    }

    /**
     * 归一化排行榜返回数量，避免 AI 拉取过大数据集。
     *
     * @param limit 原始请求数量
     * @return 返回最终生效的数量限制
     */
    private int normalizeTopLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_TOP_LIMIT;
        }
        return Math.min(limit, MAX_TOP_LIMIT);
    }

    /**
     * 归一化最近天数参数。
     *
     * @param days 原始最近天数
     * @return 返回最终生效的最近天数
     */
    private int normalizeDays(Integer days) {
        if (days == null) {
            return DEFAULT_DAYS;
        }
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new IllegalArgumentException("days must be between 1 and 730");
        }
        return days;
    }

    private record RangeWindow(
            int days,
            AnalyticsTrendGranularityEnum granularity,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String groupFormat,
            List<String> labels
    ) {
    }
}
