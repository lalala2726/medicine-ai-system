package com.zhangyichuang.medicine.model.cache;

import lombok.Data;

import java.io.Serializable;

/**
 * 客户端助手 Agent 配置。
 */
@Data
public class ClientAssistantAgentConfig implements Serializable {

    /**
     * 路由节点模型槽位配置。
     */
    private AgentModelSlotConfig routeModel;

    /**
     * 服务节点模型槽位配置。
     */
    private AgentModelSlotConfig serviceNodeModel;

    /**
     * 诊断节点模型槽位配置。
     */
    private AgentModelSlotConfig diagnosisNodeModel;

    /**
     * 是否允许客户端聊天开启统一深度思考。
     */
    private Boolean reasoningEnabled;
}
