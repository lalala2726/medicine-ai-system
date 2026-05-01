package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Agent Trace 可读消息视图容器。
 */
@Data
@Schema(description = "Agent Trace可读消息视图容器")
public class AgentTraceMessageViewVo {

    /**
     * 消息视图标题。
     */
    @Schema(description = "消息视图标题")
    private String title;

    /**
     * 用户与 AI 可读消息列表。
     */
    @Schema(description = "用户与AI可读消息列表")
    private List<AgentTraceMessageVo> messages;
}
