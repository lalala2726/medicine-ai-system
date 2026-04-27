package com.zhangyichuang.medicine.model.cache;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用能力 Agent 配置。
 * <p>
 * 该配置组统一承载图片识别、聊天历史总结与聊天标题生成三个独立能力槽位。
 */
@Data
public class CommonCapabilityAgentConfig implements Serializable {

    /**
     * 图片识别模型配置。
     */
    private AgentModelSlotConfig imageRecognitionModel;

    /**
     * 聊天历史总结模型配置。
     */
    private AgentModelSlotConfig chatHistorySummaryModel;

    /**
     * 聊天标题生成模型配置。
     */
    private AgentModelSlotConfig chatTitleModel;
}
