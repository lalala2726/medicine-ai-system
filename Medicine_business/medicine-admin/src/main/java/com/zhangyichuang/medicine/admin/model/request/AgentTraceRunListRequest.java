package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Agent Trace 运行列表查询参数。
 */
@Data
@Schema(description = "Agent Trace运行列表查询参数")
@EqualsAndHashCode(callSuper = true)
public class AgentTraceRunListRequest extends PageRequest {

    /**
     * Trace 唯一标识。
     */
    @Schema(description = "Trace唯一标识", example = "trace_abc")
    private String traceId;

    /**
     * 会话 UUID。
     */
    @Schema(description = "会话UUID", example = "conversation-uuid")
    private String conversationUuid;

    /**
     * AI 消息 UUID。
     */
    @Schema(description = "AI消息UUID", example = "assistant-message-uuid")
    private String assistantMessageUuid;

    /**
     * 用户 ID。
     */
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 会话类型。
     */
    @Schema(description = "会话类型：admin/client", example = "admin")
    private String conversationType;

    /**
     * 运行状态。
     */
    @Schema(description = "运行状态：running/success/error/cancelled", example = "success")
    private String status;

    /**
     * Graph 名称。
     */
    @Schema(description = "Graph名称", example = "admin_assistant_graph")
    private String graphName;

    /**
     * 开始时间范围起点。
     */
    @Schema(description = "开始时间范围起点", example = "2026-04-28 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 开始时间范围终点。
     */
    @Schema(description = "开始时间范围终点", example = "2026-04-28 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
