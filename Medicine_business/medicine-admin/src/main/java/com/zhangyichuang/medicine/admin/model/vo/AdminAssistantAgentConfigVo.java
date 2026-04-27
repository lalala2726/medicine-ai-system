package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 管理端助手 Agent 配置视图对象。
 */
@Data
@Schema(description = "管理端助手Agent配置视图对象")
public class AdminAssistantAgentConfigVo {

    @Schema(description = "管理端聊天界面可选展示模型列表")
    private List<AdminAssistantChatDisplayModelVo> chatDisplayModels;
}
