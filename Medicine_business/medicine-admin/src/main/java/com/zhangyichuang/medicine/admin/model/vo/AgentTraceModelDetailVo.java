package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Agent Trace 模型调用详情视图对象。
 */
@Data
@Schema(description = "Agent Trace模型调用详情视图对象")
public class AgentTraceModelDetailVo {

    /**
     * 模型名称。
     */
    @Schema(description = "模型名称")
    private String modelName;

    /**
     * 模型类名。
     */
    @Schema(description = "模型类名")
    private String modelClass;

    /**
     * 业务模型槽位。
     */
    @Schema(description = "业务模型槽位")
    private String slot;

    /**
     * 模型调用设置。
     */
    @Schema(description = "模型调用设置")
    private Object settings;

    /**
     * 模型结束原因。
     */
    @Schema(description = "模型结束原因")
    private String finishReason;

    /**
     * Token 用量。
     */
    @Schema(description = "Token用量")
    private Map<String, Object> tokenUsage;

    /**
     * 系统提示词。
     */
    @Schema(description = "系统提示词")
    private SystemPromptVo systemPrompt;

    /**
     * 当前模型可见工具列表。
     */
    @Schema(description = "当前模型可见工具列表")
    private List<ToolVo> availableTools;

    /**
     * 模型输入消息列表。
     */
    @Schema(description = "模型输入消息列表")
    private List<MessageVo> inputMessages;

    /**
     * 模型输出消息列表。
     */
    @Schema(description = "模型输出消息列表")
    private List<MessageVo> outputMessages;

    /**
     * 模型发起的工具调用列表。
     */
    @Schema(description = "模型发起的工具调用列表")
    private List<ToolCallVo> toolCalls;

    /**
     * 模型最终文本。
     */
    @Schema(description = "模型最终文本")
    private String finalText;

    /**
     * 模型可读消息视图。
     */
    @Schema(description = "模型可读消息视图")
    private AgentTraceMessageViewVo messageView;

    /**
     * 系统提示词视图对象。
     */
    @Data
    @Schema(description = "系统提示词视图对象")
    public static class SystemPromptVo {

        /**
         * 提示词内容。
         */
        @Schema(description = "提示词内容")
        private String content;

        /**
         * 渲染模式。
         */
        @Schema(description = "渲染模式")
        private String renderMode;
    }

    /**
     * 模型可见工具视图对象。
     */
    @Data
    @Schema(description = "模型可见工具视图对象")
    public static class ToolVo {

        /**
         * 工具注册名称。
         */
        @Schema(description = "工具注册名称")
        private String name;

        /**
         * 工具展示名称。
         */
        @Schema(description = "工具展示名称")
        private String displayName;

        /**
         * 工具描述。
         */
        @Schema(description = "工具描述")
        private String description;

        /**
         * 工具参数 JSON Schema。
         */
        @Schema(description = "工具参数JSON Schema")
        private Object argsSchema;

        /**
         * 本轮模型是否调用过该工具。
         */
        @Schema(description = "本轮模型是否调用过该工具")
        private Boolean called;

        /**
         * 工具调用记录。
         */
        @Schema(description = "工具调用记录")
        private List<ToolCallVo> calls;
    }

    /**
     * 模型消息视图对象。
     */
    @Data
    @Schema(description = "模型消息视图对象")
    public static class MessageVo {

        /**
         * 消息类型。
         */
        @Schema(description = "消息类型")
        private String type;

        /**
         * 消息内容。
         */
        @Schema(description = "消息内容")
        private Object content;

        /**
         * 消息名称。
         */
        @Schema(description = "消息名称")
        private String name;

        /**
         * 工具调用 ID。
         */
        @Schema(description = "工具调用ID")
        private String toolCallId;

        /**
         * 消息携带的工具调用列表。
         */
        @Schema(description = "消息携带的工具调用列表")
        private List<ToolCallVo> toolCalls;

        /**
         * 响应元数据。
         */
        @Schema(description = "响应元数据")
        private Object responseMetadata;
    }

    /**
     * 工具调用视图对象。
     */
    @Data
    @Schema(description = "工具调用视图对象")
    public static class ToolCallVo {

        /**
         * 工具调用 ID。
         */
        @Schema(description = "工具调用ID")
        private String id;

        /**
         * 工具注册名称。
         */
        @Schema(description = "工具注册名称")
        private String name;

        /**
         * 工具展示名称。
         */
        @Schema(description = "工具展示名称")
        private String displayName;

        /**
         * 工具调用参数。
         */
        @Schema(description = "工具调用参数")
        private Object arguments;

        /**
         * 工具执行状态。
         */
        @Schema(description = "工具执行状态")
        private String status;

        /**
         * 工具执行耗时毫秒。
         */
        @Schema(description = "工具执行耗时毫秒")
        private Long durationMs;

        /**
         * 工具返回结果。
         */
        @Schema(description = "工具返回结果")
        private Object outputPayload;

        /**
         * 工具错误载荷。
         */
        @Schema(description = "工具错误载荷")
        private Object errorPayload;
    }
}
