package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * Agent Trace 运行列表视图对象。
 */
@Data
@Schema(description = "Agent Trace运行列表视图对象")
public class AgentTraceRunListVo {

    /**
     * Trace 唯一标识。
     */
    @Schema(description = "Trace唯一标识")
    private String traceId;

    /**
     * 会话 UUID。
     */
    @Schema(description = "会话UUID")
    private String conversationUuid;

    /**
     * AI 消息 UUID。
     */
    @Schema(description = "AI消息UUID")
    private String assistantMessageUuid;

    /**
     * 用户 ID。
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 会话类型。
     */
    @Schema(description = "会话类型：admin/client")
    private String conversationType;

    /**
     * Graph 名称。
     */
    @Schema(description = "Graph名称")
    private String graphName;

    /**
     * 入口标识。
     */
    @Schema(description = "入口标识")
    private String entrypoint;

    /**
     * 运行状态。
     */
    @Schema(description = "运行状态")
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
     * 总耗时毫秒。
     */
    @Schema(description = "总耗时毫秒")
    private Long durationMs;

    /**
     * 输入 token 数。
     */
    @Schema(description = "输入token数")
    private Long inputTokens;

    /**
     * 输出 token 数。
     */
    @Schema(description = "输出token数")
    private Long outputTokens;

    /**
     * 总 token 数。
     */
    @Schema(description = "总token数")
    private Long totalTokens;

    /**
     * 用户最新输入文本。
     */
    @Schema(description = "用户最新输入文本")
    private String inputText;

    /**
     * AI 最终输出文本。
     */
    @Schema(description = "AI最终输出文本")
    private String outputText;
}
