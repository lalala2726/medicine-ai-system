package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Agent Trace 节点详情视图对象。
 */
@Data
@Schema(description = "Agent Trace节点详情视图对象")
public class AgentTraceNodeDetailVo {

    /**
     * 节点名称。
     */
    @Schema(description = "节点名称")
    private String nodeName;

    /**
     * 节点状态。
     */
    @Schema(description = "节点状态")
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
     * 当前节点子树汇总 Token 用量。
     */
    @Schema(description = "当前节点子树汇总Token用量")
    private Map<String, Object> tokenUsage;

    /**
     * 当前节点输入载荷。
     */
    @Schema(description = "当前节点输入载荷")
    private Object inputPayload;

    /**
     * 当前节点输出载荷。
     */
    @Schema(description = "当前节点输出载荷")
    private Object outputPayload;

    /**
     * 当前节点错误载荷或子节点错误摘要。
     */
    @Schema(description = "当前节点错误载荷或子节点错误摘要")
    private Object errorPayload;

    /**
     * 当前节点子树摘要。
     */
    @Schema(description = "当前节点子树摘要")
    private ChildSummaryVo childSummary;

    /**
     * 当前节点内部执行步骤。
     */
    @Schema(description = "当前节点内部执行步骤")
    private List<ExecutionStepVo> executionSteps;

    /**
     * 节点可读消息视图。
     */
    @Schema(description = "节点可读消息视图")
    private AgentTraceMessageViewVo messageView;

    /**
     * 节点子树摘要视图对象。
     */
    @Data
    @Schema(description = "Agent Trace节点子树摘要视图对象")
    public static class ChildSummaryVo {

        /**
         * 模型调用数量。
         */
        @Schema(description = "模型调用数量")
        private Integer modelCount;

        /**
         * 工具调用数量。
         */
        @Schema(description = "工具调用数量")
        private Integer toolCount;

        /**
         * 中间件调用数量。
         */
        @Schema(description = "中间件调用数量")
        private Integer middlewareCount;

        /**
         * 异常步骤数量。
         */
        @Schema(description = "异常步骤数量")
        private Integer errorCount;
    }

    /**
     * 节点内部执行步骤视图对象。
     */
    @Data
    @Schema(description = "Agent Trace节点内部执行步骤视图对象")
    public static class ExecutionStepVo {

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
         * 原始名称。
         */
        @Schema(description = "原始名称")
        private String name;

        /**
         * 展示名称。
         */
        @Schema(description = "展示名称")
        private String displayName;

        /**
         * 执行状态。
         */
        @Schema(description = "执行状态")
        private String status;

        /**
         * 耗时毫秒。
         */
        @Schema(description = "耗时毫秒")
        private Long durationMs;

        /**
         * Token 展示文本。
         */
        @Schema(description = "Token展示文本")
        private String tokenText;

        /**
         * Token 用量。
         */
        @Schema(description = "Token用量")
        private Map<String, Object> tokenUsage;

        /**
         * Trace 内顺序号。
         */
        @Schema(description = "Trace内顺序号")
        private Long sequence;
    }
}
