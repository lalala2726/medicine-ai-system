package com.zhangyichuang.medicine.model.cache;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理端助手聊天展示模型配置。
 * <p>
 * 用于维护前端展示名称与真实模型名称的映射关系，并携带聊天页展示所需的文案与能力信息。
 */
@Data
public class AdminAssistantChatDisplayModelConfig implements Serializable {

    /**
     * 前端聊天界面展示和提交使用的自定义模型名称。
     */
    private String customModelName;

    /**
     * 实际调用时使用的真实模型名称。
     */
    private String actualModelName;

    /**
     * 前端聊天界面的模型说明文案。
     */
    private String description;

    /**
     * 当前真实模型是否支持深度思考能力。
     */
    private Boolean supportReasoning;

    /**
     * 当前真实模型是否支持图片理解能力。
     */
    private Boolean supportVision;
}
