package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端助手聊天展示模型视图对象。
 */
@Data
@Schema(description = "管理端助手聊天展示模型视图对象")
public class AdminAssistantChatDisplayModelVo {

    @Schema(description = "前端聊天界面使用的自定义模型名称", example = "Qwen3-Max")
    private String customModelName;

    @Schema(description = "真实模型名称", example = "qwen-max")
    private String actualModelName;

    @Schema(description = "前端聊天界面的模型说明文案", example = "适用于复杂推理与多步骤任务")
    private String description;

    @Schema(description = "当前真实模型是否支持深度思考", example = "true")
    private Boolean supportReasoning;

    @Schema(description = "当前真实模型是否支持图片理解", example = "false")
    private Boolean supportVision;
}
