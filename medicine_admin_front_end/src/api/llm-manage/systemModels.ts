import { requestClient } from '@/utils/request';

export namespace SystemModelTypes {
  /**
   * 模型下拉选项
   */
  export interface ModelOption {
    /** 选项标签 */
    label: string;
    /** 选项值 */
    value: string;
    /** 是否支持深度思考 */
    supportReasoning?: boolean;
    /** 是否支持图片理解 */
    supportVision?: boolean;
    /** 模型描述文案 */
    description?: string;
  }

  /**
   * 模型选择配置
   */
  export interface ModelSelection {
    /** 模型名称 */
    modelName?: string;
    /** 当前业务槽位是否开启深度思考 */
    reasoningEnabled?: boolean;
    /** 当前模型自身是否支持深度思考 */
    supportReasoning?: boolean;
    /** 当前模型是否支持图片理解 */
    supportVision?: boolean;
  }

  /**
   * 管理端助手聊天展示模型配置
   */
  export interface AdminAssistantChatDisplayModel {
    /** 前端聊天界面展示和提交使用的自定义模型名称 */
    customModelName: string;
    /** 实际调用时使用的真实模型名称 */
    actualModelName: string;
    /** 前端聊天界面展示文案 */
    description?: string;
    /** 是否支持深度思考 */
    supportReasoning?: boolean;
    /** 是否支持图片理解 */
    supportVision?: boolean;
  }

  /**
   * 知识库下拉选项
   */
  export interface KnowledgeBaseOption {
    /** 知识库业务名称 */
    knowledgeName: string;
    /** 知识库展示名称 */
    displayName?: string;
    /** 知识库向量模型 */
    embeddingModel?: string;
    /** 知识库向量维度 */
    embeddingDim?: number;
  }

  /**
   * 知识库系统模型配置
   */
  export interface KnowledgeBaseSystemModelConfig {
    /** 是否启用知识库 */
    enabled?: boolean;
    /** 可访问知识库名称列表 */
    knowledgeNames?: string[];
    /** 向量维度 */
    embeddingDim?: number;
    /** 检索默认返回条数 */
    topK?: number | null;
    /** 向量模型配置 */
    embeddingModel?: ModelSelection | null;
    /** 是否启用排序 */
    rankingEnabled?: boolean;
    /** 排序模型配置 */
    rankingModel?: ModelSelection | null;
  }

  /**
   * 客户端知识库系统模型配置
   */
  export type ClientKnowledgeBaseSystemModelConfig = KnowledgeBaseSystemModelConfig;

  /**
   * 管理端助手系统模型配置
   */
  export interface AdminAssistantSystemModelConfig {
    /** 管理端聊天界面可选展示模型列表 */
    chatDisplayModels?: AdminAssistantChatDisplayModel[];
  }

  /**
   * 客户端助手系统模型配置
   */
  export interface ClientAssistantSystemModelConfig {
    /** 路由模型 */
    routeModel?: ModelSelection;
    /** 服务节点模型 */
    serviceNodeModel?: ModelSelection;
    /** 诊断节点模型 */
    diagnosisNodeModel?: ModelSelection;
    /** 是否允许客户端聊天开启统一深度思考 */
    reasoningEnabled?: boolean;
  }

  /**
   * 通用能力系统模型配置
   */
  export interface CommonCapabilitySystemModelConfig {
    /** 图片识别模型 */
    imageRecognitionModel?: ModelSelection;
    /** 聊天历史总结模型 */
    chatHistorySummaryModel?: ModelSelection;
    /** 聊天标题生成模型 */
    chatTitleModel?: ModelSelection;
  }

  /**
   * 豆包语音合成配置
   */
  export interface TextToSpeechConfig {
    /** 语音合成音色 */
    voiceType?: string;
    /** 最大文本长度 */
    maxTextChars?: number | null;
  }

  /**
   * 豆包语音系统配置
   */
  export interface SpeechSystemModelConfig {
    /** 火山引擎 App ID */
    appId?: string;
    /** 火山引擎 Access Token，不回显时返回 null */
    accessToken?: string | null;
    /** 语音合成配置 */
    textToSpeech?: TextToSpeechConfig;
  }

  /**
   * 提示词键选项
   */
  export interface PromptKeyOption {
    /** 提示词业务键 */
    promptKey: string;
    /** 提示词用途说明 */
    description?: string;
    /** 是否已完成数据库配置且 Redis 运行时 key 已存在 */
    configured?: boolean;
    /** 当前生效版本号 */
    promptVersion?: number | null;
  }

  /**
   * 提示词业务键新增/更新请求
   */
  export interface PromptKeyUpsertRequest {
    /** 提示词业务键 */
    promptKey: string;
    /** 提示词用途说明 */
    description?: string;
  }

  /**
   * 提示词配置详情
   */
  export interface PromptConfig {
    /** 提示词业务键 */
    promptKey?: string;
    /** 提示词正文 */
    promptContent?: string;
    /** 当前生效版本号 */
    promptVersion?: number | null;
    /** 更新时间 */
    updatedAt?: string;
    /** 更新人 */
    updatedBy?: string;
  }

  /**
   * 提示词历史版本
   */
  export interface PromptHistoryItem {
    /** 提示词业务键 */
    promptKey?: string;
    /** 历史版本号 */
    promptVersion?: number | null;
    /** 历史提示词正文 */
    promptContent?: string;
    /** 历史创建时间 */
    createdAt?: string;
    /** 历史创建人 */
    createdBy?: string;
  }

  /**
   * 提示词单条同步请求。
   */
  export interface PromptSyncRequest {
    /** 需要同步的提示词业务键 */
    promptKey: string;
  }
}

/**
 * 获取知识库系统模型配置
 */
export async function getKnowledgeBaseConfig() {
  return requestClient.get<SystemModelTypes.KnowledgeBaseSystemModelConfig>(
    '/agent/config/knowledge-base',
  );
}

/**
 * 保存知识库系统模型配置
 */
export async function saveKnowledgeBaseConfig(
  data: SystemModelTypes.KnowledgeBaseSystemModelConfig,
) {
  return requestClient.put('/agent/config/knowledge-base', data);
}

/**
 * 获取知识库选项
 */
export async function getKnowledgeBaseOptions() {
  return requestClient.get<SystemModelTypes.KnowledgeBaseOption[]>(
    '/agent/config/knowledge-base/option',
  );
}

/**
 * 获取客户端知识库系统模型配置
 */
export async function getClientKnowledgeBaseConfig() {
  return requestClient.get<SystemModelTypes.ClientKnowledgeBaseSystemModelConfig>(
    '/agent/config/client-knowledge-base',
  );
}

/**
 * 保存客户端知识库系统模型配置
 */
export async function saveClientKnowledgeBaseConfig(
  data: SystemModelTypes.ClientKnowledgeBaseSystemModelConfig,
) {
  return requestClient.put('/agent/config/client-knowledge-base', data);
}

/**
 * 获取客户端知识库选项
 */
export async function getClientKnowledgeBaseOptions() {
  return requestClient.get<SystemModelTypes.KnowledgeBaseOption[]>(
    '/agent/config/client-knowledge-base/option',
  );
}

/**
 * 获取管理端助手系统模型配置
 */
export async function getAdminAssistantConfig() {
  return requestClient.get<SystemModelTypes.AdminAssistantSystemModelConfig>(
    '/agent/config/admin-assistant',
  );
}

/**
 * 保存管理端助手系统模型配置
 */
export async function saveAdminAssistantConfig(
  data: SystemModelTypes.AdminAssistantSystemModelConfig,
) {
  return requestClient.put('/agent/config/admin-assistant', data);
}

/**
 * 获取客户端助手系统模型配置
 */
export async function getClientAssistantConfig() {
  return requestClient.get<SystemModelTypes.ClientAssistantSystemModelConfig>(
    '/agent/config/client-assistant',
  );
}

/**
 * 保存客户端助手系统模型配置
 */
export async function saveClientAssistantConfig(
  data: SystemModelTypes.ClientAssistantSystemModelConfig,
) {
  return requestClient.put('/agent/config/client-assistant', data);
}

/**
 * 获取通用能力系统模型配置
 */
export async function getCommonCapabilityConfig() {
  return requestClient.get<SystemModelTypes.CommonCapabilitySystemModelConfig>(
    '/agent/config/common-capability',
  );
}

/**
 * 保存通用能力系统模型配置
 */
export async function saveCommonCapabilityConfig(
  data: SystemModelTypes.CommonCapabilitySystemModelConfig,
) {
  return requestClient.put('/agent/config/common-capability', data);
}

/**
 * 获取豆包语音系统配置
 */
export async function getSpeechConfig() {
  return requestClient.get<SystemModelTypes.SpeechSystemModelConfig>('/agent/config/speech');
}

/**
 * 保存豆包语音系统配置
 */
export async function saveSpeechConfig(data: SystemModelTypes.SpeechSystemModelConfig) {
  return requestClient.put('/agent/config/speech', data);
}

/**
 * 获取向量模型选项
 */
export async function getEmbeddingModelOptions() {
  return requestClient.get<SystemModelTypes.ModelOption[]>('/agent/config/embedding-model/option');
}

/**
 * 获取聊天模型选项
 */
export async function getChatModelOptions() {
  return requestClient.get<SystemModelTypes.ModelOption[]>('/agent/config/chat-model/option');
}

/**
 * 获取重排模型选项
 */
export async function getRerankModelOptions() {
  return requestClient.get<SystemModelTypes.ModelOption[]>('/agent/config/rerank-model/option');
}

/**
 * 获取图片理解模型选项
 */
export async function getVisionModelOptions() {
  return requestClient.get<SystemModelTypes.ModelOption[]>('/agent/config/vision-model/option');
}

/**
 * 获取提示词键选项
 */
export async function getPromptKeyOptions() {
  return requestClient.get<SystemModelTypes.PromptKeyOption[]>('/agent/config/prompt/keys');
}

/**
 * 新增或更新提示词业务键。
 */
export async function savePromptKey(data: SystemModelTypes.PromptKeyUpsertRequest) {
  return requestClient.put('/agent/config/prompt/key', data);
}

/**
 * 获取提示词详情
 */
export async function getPromptConfig(promptKey: string) {
  return requestClient.get<SystemModelTypes.PromptConfig>('/agent/config/prompt', {
    params: { promptKey },
  });
}

/**
 * 保存提示词配置
 */
export async function savePromptConfig(data: { promptKey: string; promptContent: string }) {
  return requestClient.put('/agent/config/prompt', data);
}

/**
 * 提交全量提示词同步任务。
 */
export async function syncAllPromptConfigs() {
  return requestClient.post('/agent/config/prompt/sync/all');
}

/**
 * 提交单条提示词同步任务。
 */
export async function syncPromptConfig(data: SystemModelTypes.PromptSyncRequest) {
  return requestClient.post('/agent/config/prompt/sync', data);
}

/**
 * 获取提示词历史
 */
export async function getPromptHistory(promptKey: string, limit?: number) {
  const params: Record<string, string | number> = { promptKey };
  if (typeof limit === 'number') {
    params.limit = limit;
  }
  return requestClient.get<SystemModelTypes.PromptHistoryItem[]>('/agent/config/prompt/history', {
    params,
  });
}

/**
 * 回滚提示词版本
 */
export async function rollbackPromptConfig(data: { promptKey: string; targetVersion: number }) {
  return requestClient.post('/agent/config/prompt/rollback', data);
}

/**
 * 删除提示词当前配置与全部历史。
 */
export async function deletePromptConfig(data: {
  /** 提示词业务键 */
  promptKey: string;
  /** 验证码校验凭证 */
  captchaVerificationId: string;
}) {
  return requestClient.delete('/agent/config/prompt', {
    params: data,
  });
}
