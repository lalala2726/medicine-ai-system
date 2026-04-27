package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 通用能力 Agent 配置请求对象。
 */
@Data
@Schema(description = "通用能力Agent配置请求对象")
public class CommonCapabilityAgentConfigRequest {

    @Schema(description = "图片识别模型配置")
    @Valid
    @NotNull(message = "图片识别模型配置不能为空")
    private AgentModelSelectionRequest imageRecognitionModel;

    @Schema(description = "聊天历史总结模型配置")
    @Valid
    @NotNull(message = "聊天历史总结模型配置不能为空")
    private AgentModelSelectionRequest chatHistorySummaryModel;

    @Schema(description = "聊天标题生成模型配置")
    @Valid
    @NotNull(message = "聊天标题生成模型配置不能为空")
    private AgentModelSelectionRequest chatTitleModel;
}
