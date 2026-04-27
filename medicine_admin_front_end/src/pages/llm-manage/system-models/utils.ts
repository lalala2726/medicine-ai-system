import type { SystemModelTypes } from '@/api/llm-manage/systemModels';

export function isKnowledgeBaseEnabled(
  config?:
    | SystemModelTypes.KnowledgeBaseSystemModelConfig
    | SystemModelTypes.ClientKnowledgeBaseSystemModelConfig,
) {
  if (typeof config?.enabled === 'boolean') {
    return config.enabled;
  }

  return Boolean(config?.knowledgeNames?.length || config?.embeddingModel?.modelName);
}
