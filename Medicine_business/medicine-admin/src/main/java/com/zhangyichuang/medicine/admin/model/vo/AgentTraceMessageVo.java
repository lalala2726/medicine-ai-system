package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Agent Trace 可读消息视图对象。
 */
@Data
@Schema(description = "Agent Trace可读消息视图对象")
public class AgentTraceMessageVo {

    /**
     * 消息唯一标识。
     */
    @Schema(description = "消息唯一标识")
    private String id;

    /**
     * 消息角色：user / ai。
     */
    @Schema(description = "消息角色")
    private String role;

    /**
     * 消息文本内容。
     */
    @Schema(description = "消息文本内容")
    private String content;

    /**
     * 消息来源 Span ID。
     */
    @Schema(description = "消息来源Span ID")
    private String sourceSpanId;

    /**
     * 消息来源 Span 顺序号。
     */
    @Schema(description = "消息来源Span顺序号")
    private Long sequence;
}
