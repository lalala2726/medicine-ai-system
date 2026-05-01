package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.admin.model.request.AgentTraceMonitorRequest;
import com.zhangyichuang.medicine.admin.model.vo.*;
import com.zhangyichuang.medicine.admin.service.AgentTraceMonitorService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent Trace 监控控制器。
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/agent/trace/monitor")
@Tag(name = "Agent Trace监控", description = "管理端智能体模型调用监控接口")
public class AgentTraceMonitorController extends BaseController {

    /**
     * Agent Trace 监控服务。
     */
    private final AgentTraceMonitorService agentTraceMonitorService;

    /**
     * 查询 Agent Trace 监控概览。
     *
     * @param request 监控查询参数。
     * @return 监控概览指标。
     */
    @GetMapping("/summary")
    @Operation(summary = "Agent Trace监控概览")
    @PreAuthorize("hasAuthority('system:agent_trace:monitor') or hasRole('super_admin')")
    public AjaxResult<AgentTraceMonitorSummaryVo> getMonitorSummary(AgentTraceMonitorRequest request) {
        return success(agentTraceMonitorService.getMonitorSummary(request));
    }

    /**
     * 查询 Agent Trace 监控趋势。
     *
     * @param request 监控查询参数。
     * @return 监控趋势数据。
     */
    @GetMapping("/timeline")
    @Operation(summary = "Agent Trace监控趋势")
    @PreAuthorize("hasAuthority('system:agent_trace:monitor') or hasRole('super_admin')")
    public AjaxResult<List<AgentTraceMonitorTimelinePointVo>> getMonitorTimeline(AgentTraceMonitorRequest request) {
        return success(agentTraceMonitorService.getMonitorTimeline(request));
    }

    /**
     * 查询 Agent Trace 监控图表数据。
     *
     * @param request 监控查询参数。
     * @return 监控图表数据。
     */
    @GetMapping("/charts")
    @Operation(summary = "Agent Trace监控图表")
    @PreAuthorize("hasAuthority('system:agent_trace:monitor') or hasRole('super_admin')")
    public AjaxResult<AgentTraceMonitorChartsVo> getMonitorCharts(AgentTraceMonitorRequest request) {
        return success(agentTraceMonitorService.getMonitorCharts(request));
    }

    /**
     * 查询 Agent Trace 模型排行。
     *
     * @param request 监控查询参数。
     * @return 模型排行数据。
     */
    @GetMapping("/model-ranking")
    @Operation(summary = "Agent Trace模型排行")
    @PreAuthorize("hasAuthority('system:agent_trace:monitor') or hasRole('super_admin')")
    public AjaxResult<List<AgentTraceMonitorModelRankingVo>> getMonitorModelRanking(AgentTraceMonitorRequest request) {
        return success(agentTraceMonitorService.getMonitorModelRanking(request));
    }

    /**
     * 查询 Agent Trace 单模型监控详情。
     *
     * @param request 监控查询参数。
     * @return 单模型监控详情。
     */
    @GetMapping("/model-detail")
    @Operation(summary = "Agent Trace单模型监控详情")
    @PreAuthorize("hasAuthority('system:agent_trace:monitor') or hasRole('super_admin')")
    public AjaxResult<AgentTraceMonitorModelDetailVo> getMonitorModelDetail(AgentTraceMonitorRequest request) {
        return success(agentTraceMonitorService.getMonitorModelDetail(request));
    }
}
