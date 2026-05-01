package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Agent Trace 详情视图对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Agent Trace详情视图对象")
public class AgentTraceDetailVo extends AgentTraceRunListVo {

    /**
     * 根 graph span ID。
     */
    @Schema(description = "根graph span ID")
    private String rootSpanId;

    /**
     * 错误载荷。
     */
    @Schema(description = "错误载荷")
    private Object errorPayload;

    /**
     * 顶层 Trace 概览详情。
     */
    @Schema(description = "顶层Trace概览详情")
    private AgentTraceOverviewDetailVo overviewDetail;

    /**
     * Span 明细列表。
     */
    @Schema(description = "Span明细列表")
    private List<AgentTraceSpanVo> spans;

    /**
     * Span 树形展示节点列表。
     */
    @Schema(description = "Span树形展示节点列表")
    private List<AgentTraceSpanTreeNodeVo> spanTree;
}
