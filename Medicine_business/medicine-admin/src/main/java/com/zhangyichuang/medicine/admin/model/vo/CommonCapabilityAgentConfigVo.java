package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通用能力 Agent 配置视图对象。
 */
@Data
@Schema(description = "通用能力Agent配置视图对象")
public class CommonCapabilityAgentConfigVo {

    @Schema(description = "图片识别模型配置")
    private AgentModelSelectionVo imageRecognitionModel;

    @Schema(description = "聊天历史总结模型配置")
    private AgentModelSelectionVo chatHistorySummaryModel;

    @Schema(description = "聊天标题生成模型配置")
    private AgentModelSelectionVo chatTitleModel;
}
