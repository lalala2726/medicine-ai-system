package com.zhangyichuang.medicine.model.cache;

import java.io.Serializable;

/**
 * Redis 中各业务 Agent 配置分组。
 */
public class AgentConfigsCache implements Serializable {

    /**
     * 知识库相关 Agent 配置。
     */
    private KnowledgeBaseAgentConfig knowledgeBase;

    /**
     * 客户端聊天知识库相关 Agent 配置。
     */
    private KnowledgeBaseAgentConfig clientKnowledgeBase;

    /**
     * 管理端助手相关 Agent 配置。
     */
    private AdminAssistantAgentConfig adminAssistant;

    /**
     * 客户端助手相关 Agent 配置。
     */
    private ClientAssistantAgentConfig clientAssistant;

    /**
     * 通用能力相关 Agent 配置。
     */
    private CommonCapabilityAgentConfig commonCapability;

    public KnowledgeBaseAgentConfig getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(KnowledgeBaseAgentConfig knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * 读取客户端聊天知识库相关 Agent 配置。
     *
     * @return 客户端聊天知识库相关 Agent 配置
     */
    public KnowledgeBaseAgentConfig getClientKnowledgeBase() {
        return clientKnowledgeBase;
    }

    /**
     * 写入客户端聊天知识库相关 Agent 配置。
     *
     * @param clientKnowledgeBase 客户端聊天知识库相关 Agent 配置
     */
    public void setClientKnowledgeBase(KnowledgeBaseAgentConfig clientKnowledgeBase) {
        this.clientKnowledgeBase = clientKnowledgeBase;
    }

    public AdminAssistantAgentConfig getAdminAssistant() {
        return adminAssistant;
    }

    public void setAdminAssistant(AdminAssistantAgentConfig adminAssistant) {
        this.adminAssistant = adminAssistant;
    }

    public ClientAssistantAgentConfig getClientAssistant() {
        return clientAssistant;
    }

    public void setClientAssistant(ClientAssistantAgentConfig clientAssistant) {
        this.clientAssistant = clientAssistant;
    }

    /**
     * 读取通用能力相关 Agent 配置。
     *
     * @return 通用能力相关 Agent 配置
     */
    public CommonCapabilityAgentConfig getCommonCapability() {
        return commonCapability;
    }

    /**
     * 写入通用能力相关 Agent 配置。
     *
     * @param commonCapability 通用能力相关 Agent 配置
     */
    public void setCommonCapability(CommonCapabilityAgentConfig commonCapability) {
        this.commonCapability = commonCapability;
    }
}
