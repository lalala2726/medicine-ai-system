package com.zhangyichuang.medicine.rpc.admin;

import com.zhangyichuang.medicine.model.vo.analytics.*;

import java.util.List;

/**
 * 管理端 Agent 运营分析只读 RPC。
 */
public interface AdminAgentAnalyticsRpcService {

    /**
     * 获取实时运营总览。
     *
     * @return 实时总览数据
     */
    AnalyticsRealtimeOverviewVo realtimeOverview();

    /**
     * 获取指定时间范围内的经营结果汇总。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 经营结果汇总
     */
    AnalyticsRangeSummaryVo rangeSummary(Integer days);

    /**
     * 获取指定时间范围内的支付转化汇总。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 支付转化汇总
     */
    AnalyticsConversionSummaryVo conversionSummary(Integer days);

    /**
     * 获取指定时间范围内的履约时效汇总。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 履约时效汇总
     */
    AnalyticsFulfillmentSummaryVo fulfillmentSummary(Integer days);

    /**
     * 获取指定时间范围内的售后处理时效汇总。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 售后处理时效汇总
     */
    AnalyticsAfterSaleEfficiencySummaryVo afterSaleEfficiencySummary(Integer days);

    /**
     * 获取指定时间范围内的售后状态分布。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 售后状态分布
     */
    List<AnalyticsStatusDistributionItemVo> afterSaleStatusDistribution(Integer days);

    /**
     * 获取指定时间范围内的售后原因分布。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 售后原因分布
     */
    List<AnalyticsReasonDistributionItemVo> afterSaleReasonDistribution(Integer days);

    /**
     * 获取指定时间范围内的热销商品排行。
     *
     * @param days  最近天数，默认30，范围1-730
     * @param limit 返回数量限制
     * @return 热销商品排行
     */
    List<AnalyticsTopSellingProductVo> topSellingProducts(Integer days, int limit);

    /**
     * 获取指定时间范围内的退货退款风险商品排行。
     *
     * @param days  最近天数，默认30，范围1-730
     * @param limit 返回数量限制
     * @return 风险商品排行
     */
    List<AnalyticsReturnRefundRiskProductVo> returnRefundRiskProducts(Integer days, int limit);

    /**
     * 获取指定时间范围内的成交趋势。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 成交趋势
     */
    AnalyticsSalesTrendVo salesTrend(Integer days);

    /**
     * 获取指定时间范围内的售后趋势。
     *
     * @param days 最近天数，默认30，范围1-730
     * @return 售后趋势
     */
    AnalyticsAfterSaleTrendVo afterSaleTrend(Integer days);
}
