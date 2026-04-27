package com.zhangyichuang.medicine.admin.service;

import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.*;

import java.util.List;

/**
 * Agent 配置服务。
 */
public interface AgentConfigService {

    /**
     * 查询知识库 Agent 配置详情。
     *
     * @return 知识库 Agent 配置
     */
    KnowledgeBaseAgentConfigVo getKnowledgeBaseConfig();

    /**
     * 保存知识库 Agent 配置。
     *
     * @param request 知识库 Agent 配置请求
     * @return 是否保存成功
     */
    boolean saveKnowledgeBaseConfig(KnowledgeBaseAgentConfigRequest request);

    /**
     * 查询知识库下拉选项列表。
     *
     * @return 知识库下拉选项
     */
    List<KnowledgeBaseOptionVo> listKnowledgeBaseOptions();

    /**
     * 查询客户端知识库 Agent 配置详情。
     *
     * @return 客户端知识库 Agent 配置
     */
    KnowledgeBaseAgentConfigVo getClientKnowledgeBaseConfig();

    /**
     * 保存客户端知识库 Agent 配置。
     *
     * @param request 客户端知识库 Agent 配置请求
     * @return 是否保存成功
     */
    boolean saveClientKnowledgeBaseConfig(KnowledgeBaseAgentConfigRequest request);

    /**
     * 查询客户端知识库下拉选项列表。
     *
     * @return 客户端知识库下拉选项
     */
    List<KnowledgeBaseOptionVo> listClientKnowledgeBaseOptions();

    /**
     * 查询管理端助手 Agent 配置详情。
     *
     * @return 管理端助手 Agent 配置
     */
    AdminAssistantAgentConfigVo getAdminAssistantConfig();

    /**
     * 查询客户端助手 Agent 配置详情。
     *
     * @return 客户端助手 Agent 配置
     */
    ClientAssistantAgentConfigVo getClientAssistantConfig();

    /**
     * 保存管理端助手 Agent 配置。
     *
     * @param request 管理端助手 Agent 配置请求
     * @return 是否保存成功
     */
    boolean saveAdminAssistantConfig(AdminAssistantAgentConfigRequest request);

    /**
     * 保存客户端助手 Agent 配置。
     *
     * @param request 客户端助手 Agent 配置请求
     * @return 是否保存成功
     */
    boolean saveClientAssistantConfig(ClientAssistantAgentConfigRequest request);

    /**
     * 查询通用能力 Agent 配置详情。
     *
     * @return 通用能力 Agent 配置
     */
    CommonCapabilityAgentConfigVo getCommonCapabilityConfig();

    /**
     * 查询豆包语音 Agent 配置详情。
     *
     * @return 豆包语音 Agent 配置
     */
    SpeechAgentConfigVo getSpeechConfig();

    /**
     * 保存通用能力 Agent 配置。
     *
     * @param request 通用能力 Agent 配置请求
     * @return 是否保存成功
     */
    boolean saveCommonCapabilityConfig(CommonCapabilityAgentConfigRequest request);

    /**
     * 保存豆包语音 Agent 配置。
     *
     * @param request 豆包语音 Agent 配置请求
     * @return 是否保存成功
     */
    boolean saveSpeechConfig(SpeechAgentConfigRequest request);

    /**
     * 查询向量模型选项列表。
     *
     * @return 向量模型选项
     */
    List<AgentModelOptionVo> listEmbeddingModelOptions();

    /**
     * 查询聊天模型选项列表。
     *
     * @return 聊天模型选项
     */
    List<AgentModelOptionVo> listChatModelOptions();

    /**
     * 查询重排模型选项列表。
     *
     * @return 重排模型选项
     */
    List<AgentModelOptionVo> listRerankModelOptions();

    /**
     * 查询图片理解模型选项列表。
     *
     * @return 图片理解模型选项
     */
    List<AgentModelOptionVo> listVisionModelOptions();
}
