package com.zhangyichuang.medicine.model.cache;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 管理端助手 Agent 配置。
 */
@Data
public class AdminAssistantAgentConfig implements Serializable {

    /**
     * 管理端聊天界面可选展示模型列表。
     */
    private List<AdminAssistantChatDisplayModelConfig> chatDisplayModels;
}
