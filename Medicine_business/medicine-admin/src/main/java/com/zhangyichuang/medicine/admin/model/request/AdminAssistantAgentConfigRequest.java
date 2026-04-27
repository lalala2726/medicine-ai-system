package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 管理端助手 Agent 配置请求对象。
 */
@Data
@Schema(description = "管理端助手Agent配置请求对象")
public class AdminAssistantAgentConfigRequest {

    @Schema(description = "管理端聊天界面可选展示模型列表")
    @Valid
    private List<@NotNull(message = "聊天展示模型配置不能为空") @Valid AdminAssistantChatDisplayModelRequest>
            chatDisplayModels;
}
