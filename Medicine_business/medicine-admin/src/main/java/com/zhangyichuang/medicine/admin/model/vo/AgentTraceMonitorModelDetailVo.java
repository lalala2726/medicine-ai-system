package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Agent Trace 单模型监控详情视图对象。
 */
@Data
@Schema(description = "Agent Trace单模型监控详情视图对象")
public class AgentTraceMonitorModelDetailVo {

    /**
     * 模型供应商。
     */
    @Schema(description = "模型供应商")
    private String provider;

    /**
     * 真实模型名称。
     */
    @Schema(description = "真实模型名称")
    private String modelName;

    /**
     * 模型汇总指标。
     */
    @Schema(description = "模型汇总指标")
    private AgentTraceMonitorSummaryVo summary;

    /**
     * 模型趋势数据。
     */
    @Schema(description = "模型趋势数据")
    private List<AgentTraceMonitorTimelinePointVo> timeline;
}
