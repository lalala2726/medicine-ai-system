package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端助手聊天展示模型请求对象。
 */
@Data
@Schema(description = "管理端助手聊天展示模型请求对象")
public class AdminAssistantChatDisplayModelRequest {

    @Schema(description = "前端聊天界面使用的自定义模型名称", example = "Qwen3-Max")
    @NotBlank(message = "自定义模型名称不能为空")
    private String customModelName;

    @Schema(description = "真实模型名称", example = "qwen-max")
    @NotBlank(message = "真实模型名称不能为空")
    private String actualModelName;

    @Schema(description = "前端聊天界面的模型说明文案", example = "适用于复杂推理与多步骤任务")
    private String description;
}
