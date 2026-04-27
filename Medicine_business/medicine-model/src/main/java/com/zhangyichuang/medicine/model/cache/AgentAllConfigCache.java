package com.zhangyichuang.medicine.model.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

/**
 * Agent 全量配置缓存对象。
 * <p>
 * 该对象作为 Redis 中 agent:config:all 的最终保存结构，供管理端写入、Agent 端读取。
 */
@Data
public class AgentAllConfigCache implements Serializable {

    /**
     * 配置更新时间
     */
    private String updatedAt;

    /**
     * 配置更新人
     */
    private String updatedBy;

    /**
     * 当前启用 LLM 提供商运行时配置。
     */
    private AgentLlmConfig llm;

    /**
     * 业务 Agent 配置集合。
     */
    private AgentConfigsCache agentConfigs = new AgentConfigsCache();

    /**
     * 豆包语音相关 Agent 配置
     */
    private SpeechAgentConfig speech;

    /**
     * 兼容旧调用点的只读访问器，不参与 Redis 序列化。
     */
    @JsonIgnore
    public KnowledgeBaseAgentConfig getKnowledgeBase() {
        return agentConfigs == null ? null : agentConfigs.getKnowledgeBase();
    }

    /**
     * 兼容旧调用点的写访问器，不参与 Redis 序列化。
     */
    @JsonIgnore
    public void setKnowledgeBase(KnowledgeBaseAgentConfig knowledgeBase) {
        ensureAgentConfigs().setKnowledgeBase(knowledgeBase);
    }

    /**
     * 读取客户端知识库配置，不参与 Redis 序列化。
     */
    @JsonIgnore
    public KnowledgeBaseAgentConfig getClientKnowledgeBase() {
        return agentConfigs == null ? null : agentConfigs.getClientKnowledgeBase();
    }

    /**
     * 写入客户端知识库配置，不参与 Redis 序列化。
     */
    @JsonIgnore
    public void setClientKnowledgeBase(KnowledgeBaseAgentConfig clientKnowledgeBase) {
        ensureAgentConfigs().setClientKnowledgeBase(clientKnowledgeBase);
    }

    /**
     * 兼容旧调用点的只读访问器，不参与 Redis 序列化。
     */
    @JsonIgnore
    public AdminAssistantAgentConfig getAdminAssistant() {
        return agentConfigs == null ? null : agentConfigs.getAdminAssistant();
    }

    /**
     * 兼容旧调用点的写访问器，不参与 Redis 序列化。
     */
    @JsonIgnore
    public void setAdminAssistant(AdminAssistantAgentConfig adminAssistant) {
        ensureAgentConfigs().setAdminAssistant(adminAssistant);
    }

    /**
     * 兼容旧调用点的只读访问器，不参与 Redis 序列化。
     */
    @JsonIgnore
    public ClientAssistantAgentConfig getClientAssistant() {
        return agentConfigs == null ? null : agentConfigs.getClientAssistant();
    }

    /**
     * 兼容旧调用点的写访问器，不参与 Redis 序列化。
     */
    @JsonIgnore
    public void setClientAssistant(ClientAssistantAgentConfig clientAssistant) {
        ensureAgentConfigs().setClientAssistant(clientAssistant);
    }

    /**
     * 兼容旧调用点的只读访问器，不参与 Redis 序列化。
     */
    @JsonIgnore
    public CommonCapabilityAgentConfig getCommonCapability() {
        return agentConfigs == null ? null : agentConfigs.getCommonCapability();
    }

    /**
     * 写入通用能力配置，不参与 Redis 序列化。
     */
    @JsonIgnore
    public void setCommonCapability(CommonCapabilityAgentConfig commonCapability) {
        ensureAgentConfigs().setCommonCapability(commonCapability);
    }

    private AgentConfigsCache ensureAgentConfigs() {
        if (agentConfigs == null) {
            agentConfigs = new AgentConfigsCache();
        }
        return agentConfigs;
    }
}
