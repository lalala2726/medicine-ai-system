package com.zhangyichuang.medicine.admin.service;

import com.zhangyichuang.medicine.admin.model.request.AgentTraceMonitorRequest;
import com.zhangyichuang.medicine.admin.model.vo.*;

import java.util.List;

/**
 * Agent Trace 监控服务。
 */
public interface AgentTraceMonitorService {

    /**
     * 查询 Agent Trace 监控概览。
     *
     * @param request 监控查询参数。
     * @return 监控概览指标。
     */
    AgentTraceMonitorSummaryVo getMonitorSummary(AgentTraceMonitorRequest request);

    /**
     * 查询 Agent Trace 监控趋势。
     *
     * @param request 监控查询参数。
     * @return 趋势时间点列表。
     */
    List<AgentTraceMonitorTimelinePointVo> getMonitorTimeline(AgentTraceMonitorRequest request);

    /**
     * 查询 Agent Trace 监控图表数据。
     *
     * @param request 监控查询参数。
     * @return 监控图表数据。
     */
    AgentTraceMonitorChartsVo getMonitorCharts(AgentTraceMonitorRequest request);

    /**
     * 查询 Agent Trace 模型排行。
     *
     * @param request 监控查询参数。
     * @return 模型排行列表。
     */
    List<AgentTraceMonitorModelRankingVo> getMonitorModelRanking(AgentTraceMonitorRequest request);

    /**
     * 查询 Agent Trace 单模型详情。
     *
     * @param request 监控查询参数。
     * @return 单模型监控详情。
     */
    AgentTraceMonitorModelDetailVo getMonitorModelDetail(AgentTraceMonitorRequest request);
}
