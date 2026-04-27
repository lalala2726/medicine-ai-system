package com.zhangyichuang.medicine.admin.service;

import com.zhangyichuang.medicine.model.vo.analytics.*;

import java.util.List;

/**
 * 商城运营分析服务接口。
 * <p>
 * 负责面向管理端聚合经营结果、履约时效、售后效率和趋势图所需的数据。
 */
public interface AnalyticsService {

    /**
     * 获取实时运营总览。
     *
     * @return 返回累计成交、今日成交、待发货和售后待处理等实时总览指标
     */
    AnalyticsRealtimeOverviewVo realtimeOverview();

    /**
     * 获取指定时间范围内的经营结果汇总。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 返回成交金额、退款金额、净成交额、退款率等汇总指标
     */
    AnalyticsRangeSummaryVo rangeSummary(Integer days);

    /**
     * 获取指定时间范围内的支付转化汇总。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 返回下单数、支付数、待支付数、关闭数和支付转化率
     */
    AnalyticsConversionSummaryVo conversionSummary(Integer days);

    /**
     * 获取指定时间范围内的履约时效汇总。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 返回平均发货时长、平均收货时长和超时履约统计
     */
    AnalyticsFulfillmentSummaryVo fulfillmentSummary(Integer days);

    /**
     * 获取指定时间范围内的售后处理时效汇总。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 返回平均审核时长、平均完结时长和超时售后统计
     */
    AnalyticsAfterSaleEfficiencySummaryVo afterSaleEfficiencySummary(Integer days);

    /**
     * 获取指定时间范围内的售后状态分布。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 返回售后状态及对应数量列表
     */
    List<AnalyticsStatusDistributionItemVo> afterSaleStatusDistribution(Integer days);

    /**
     * 获取指定时间范围内的售后原因分布。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 返回售后原因及对应数量列表
     */
    List<AnalyticsReasonDistributionItemVo> afterSaleReasonDistribution(Integer days);

    /**
     * 获取指定时间范围内的热销商品排行。
     *
     * @param days  最近天数，默认30，范围1-730
     * @param limit 返回数量限制，默认10，最大20
     * @return 返回按销量降序排列的热销商品列表
     */
    List<AnalyticsTopSellingProductVo> topSellingProducts(Integer days, int limit);

    /**
     * 获取指定时间范围内的退货退款风险商品排行。
     *
     * @param days  最近天数，默认30，范围1-730
     * @param limit 返回数量限制，默认10，最大20
     * @return 返回按风险程度排序的退货退款商品列表
     */
    List<AnalyticsReturnRefundRiskProductVo> returnRefundRiskProducts(Integer days, int limit);

    /**
     * 获取指定时间范围内的成交趋势。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 返回仅包含成交相关字段的趋势点位列表
     */
    AnalyticsSalesTrendVo salesTrend(Integer days);

    /**
     * 获取指定时间范围内的售后趋势。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 返回仅包含售后相关字段的趋势点位列表
     */
    AnalyticsAfterSaleTrendVo afterSaleTrend(Integer days);

    /**
     * 获取运营看板聚合数据。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 返回指定时间范围内的运营看板数据，包含总览、汇总、转化、时效、分布和商品排行
     */
    AnalyticsDashboardVo dashboard(Integer days);

    /**
     * 获取运营趋势数据。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 返回指定时间范围内的趋势点位数据，包含成交与售后两类趋势字段
     */
    AnalyticsTrendVo trend(Integer days);
}
