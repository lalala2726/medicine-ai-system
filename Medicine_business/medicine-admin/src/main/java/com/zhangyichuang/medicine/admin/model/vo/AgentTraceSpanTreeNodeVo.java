package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Agent Trace Span 树形展示节点视图对象。
 */
@Data
@Schema(description = "Agent Trace Span树形展示节点视图对象")
public class AgentTraceSpanTreeNodeVo {

    /**
     * 前端树节点唯一标识。
     */
    @Schema(description = "前端树节点唯一标识")
    private String nodeId;

    /**
     * 真实 Span 唯一标识。
     */
    @Schema(description = "真实Span唯一标识")
    private String sourceSpanId;

    /**
     * 父级树节点唯一标识。
     */
    @Schema(description = "父级树节点唯一标识")
    private String parentNodeId;

    /**
     * 树节点类型。
     */
    @Schema(description = "树节点类型：span_group/span/model_call")
    private String nodeType;

    /**
     * Span 类型。
     */
    @Schema(description = "Span类型")
    private String spanType;

    /**
     * 原始名称。
     */
    @Schema(description = "原始名称")
    private String name;

    /**
     * 前端展示名称。
     */
    @Schema(description = "前端展示名称")
    private String displayName;

    /**
     * 模型名称。
     */
    @Schema(description = "模型名称")
    private String modelName;

    /**
     * 运行状态。
     */
    @Schema(description = "运行状态")
    private String status;

    /**
     * 总耗时毫秒。
     */
    @Schema(description = "总耗时毫秒")
    private Long durationMs;

    /**
     * Token 展示文本。
     */
    @Schema(description = "Token展示文本")
    private String tokenText;

    /**
     * Trace 内顺序号。
     */
    @Schema(description = "Trace内顺序号")
    private Long sequence;

    /**
     * 子树节点列表。
     */
    @Schema(description = "子树节点列表")
    private List<AgentTraceSpanTreeNodeVo> children;
}
