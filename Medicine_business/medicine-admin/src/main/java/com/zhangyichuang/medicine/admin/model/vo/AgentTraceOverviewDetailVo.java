package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Agent Trace 顶层概览详情视图对象。
 */
@Data
@Schema(description = "Agent Trace顶层概览详情视图对象")
public class AgentTraceOverviewDetailVo {

    /**
     * Root Span 唯一标识。
     */
    @Schema(description = "Root Span唯一标识")
    private String spanId;

    /**
     * Trace 展示名称。
     */
    @Schema(description = "Trace展示名称")
    private String name;

    /**
     * Trace 状态。
     */
    @Schema(description = "Trace状态")
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
     * Token 用量。
     */
    @Schema(description = "Token用量")
    private Map<String, Object> tokenUsage;

    /**
     * 顶层输入详情。
     */
    @Schema(description = "顶层输入详情")
    private InputVo input;

    /**
     * 顶层输出详情。
     */
    @Schema(description = "顶层输出详情")
    private OutputVo output;

    /**
     * 顶层属性详情。
     */
    @Schema(description = "顶层属性详情")
    private Map<String, Object> attributes;

    /**
     * 错误载荷。
     */
    @Schema(description = "错误载荷")
    private Object errorPayload;

    /**
     * 顶层 Trace 可读消息视图。
     */
    @Schema(description = "顶层Trace可读消息视图")
    private AgentTraceMessageViewVo messageView;

    /**
     * 顶层输入视图对象。
     */
    @Data
    @Schema(description = "Agent Trace顶层输入视图对象")
    public static class InputVo {

        /**
         * 系统提示词。
         */
        @Schema(description = "系统提示词")
        private AgentTraceModelDetailVo.SystemPromptVo systemPrompt;

        /**
         * 用户视角输入消息列表。
         */
        @Schema(description = "用户视角输入消息列表")
        private List<AgentTraceModelDetailVo.MessageVo> messages;
    }

    /**
     * 顶层输出视图对象。
     */
    @Data
    @Schema(description = "Agent Trace顶层输出视图对象")
    public static class OutputVo {

        /**
         * 最终回复文本。
         */
        @Schema(description = "最终回复文本")
        private String finalText;

        /**
         * 整轮 Trace 关联的工具调用列表。
         */
        @Schema(description = "整轮Trace关联的工具调用列表")
        private List<AgentTraceModelDetailVo.ToolCallVo> toolCalls;
    }
}
