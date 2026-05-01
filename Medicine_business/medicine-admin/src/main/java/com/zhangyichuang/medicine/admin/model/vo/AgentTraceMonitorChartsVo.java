package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Agent Trace 监控图表视图对象。
 */
@Data
@Schema(description = "Agent Trace监控图表视图对象")
public class AgentTraceMonitorChartsVo {

    /**
     * 调用次数趋势图表数据。
     */
    @Schema(description = "调用次数趋势图表数据")
    private List<AgentTraceMonitorCallTrendPointVo> callTrend;

    /**
     * 耗时趋势图表数据。
     */
    @Schema(description = "耗时趋势图表数据")
    private List<AgentTraceMonitorDurationTrendPointVo> durationTrend;

    /**
     * Token 与缓存图表数据。
     */
    @Schema(description = "Token与缓存图表数据")
    private List<AgentTraceMonitorTokenCachePointVo> tokenCacheTrend;
}
