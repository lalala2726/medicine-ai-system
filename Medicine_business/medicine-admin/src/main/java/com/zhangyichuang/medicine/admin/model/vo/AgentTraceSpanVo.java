package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * Agent Trace Span 明细视图对象。
 */
@Data
@Schema(description = "Agent Trace Span明细视图对象")
public class AgentTraceSpanVo {

    /**
     * Span 唯一标识。
     */
    @Schema(description = "Span唯一标识")
    private String spanId;

    /**
     * 父 Span 唯一标识。
     */
    @Schema(description = "父Span唯一标识")
    private String parentSpanId;

    /**
     * Span 类型。
     */
    @Schema(description = "Span类型")
    private String spanType;

    /**
     * Span 名称。
     */
    @Schema(description = "Span名称")
    private String name;

    /**
     * Span 状态。
     */
    @Schema(description = "Span状态")
    private String status;

    /**
     * 开始时间。
     */
    @Schema(description = "开始时间")
    private Date startedAt;

    /**
     * 结束时间。
     */
    @Schema(description = "结束时间")
    private Date endedAt;

    /**
     * 耗时毫秒。
     */
    @Schema(description = "耗时毫秒")
    private Long durationMs;

    /**
     * 输入载荷。
     */
    @Schema(description = "输入载荷")
    private Object inputPayload;

    /**
     * 输出载荷。
     */
    @Schema(description = "输出载荷")
    private Object outputPayload;

    /**
     * 附加属性。
     */
    @Schema(description = "附加属性")
    private Map<String, Object> attributes;

    /**
     * Token 用量。
     */
    @Schema(description = "Token用量")
    private Map<String, Object> tokenUsage;

    /**
     * 模型调用详情。
     */
    @Schema(description = "模型调用详情")
    private AgentTraceModelDetailVo modelDetail;

    /**
     * 节点执行详情。
     */
    @Schema(description = "节点执行详情")
    private AgentTraceNodeDetailVo nodeDetail;

    /**
     * 错误载荷。
     */
    @Schema(description = "错误载荷")
    private Object errorPayload;

    /**
     * Trace 内顺序号。
     */
    @Schema(description = "Trace内顺序号")
    private Long sequence;
}
