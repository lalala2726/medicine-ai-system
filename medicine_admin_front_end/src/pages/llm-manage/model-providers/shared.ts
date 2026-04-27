import type { ModelProviderTypes } from '@/api/llm-manage/modelProviders';

export interface OptionLike<T = string> {
  label: string;
  value: T;
}

export interface ProviderModelFormValue {
  modelName: string;
  modelType?: ModelProviderTypes.ModelType;
  supportReasoning?: number;
  supportVision?: number;
  description?: string;
  enabled?: boolean;
}

export interface ProviderFormValues {
  providerKey?: string;
  providerName: string;
  providerType?: ModelProviderTypes.ProviderType;
  baseUrl: string;
  apiKey?: string;
  description?: string;
  sort: number;
  models: ProviderModelFormValue[];
}

/**
 * 模型提供商页面的预设来源类型，仅使用预设创建。
 */
export const PRESET_SOURCE = 'preset';

/**
 * 阿里云百联预设提供商键。
 */
export const ALIYUN_BAILIAN_PROVIDER_KEY = 'aliyun-bailian';

/**
 * 阿里云百联默认 Base URL。
 */
export const ALIYUN_BAILIAN_BASE_URL = 'https://dashscope.aliyuncs.com/compatible-mode/v1';

export const MODEL_TYPE_LABELS: Record<ModelProviderTypes.ModelType, string> = {
  CHAT: '对话模型',
  EMBEDDING: '向量模型',
  RERANK: '重排模型',
};

export const MODEL_TYPE_OPTIONS: Array<OptionLike<ModelProviderTypes.ModelType>> = [
  { label: MODEL_TYPE_LABELS.CHAT, value: 'CHAT' },
  { label: MODEL_TYPE_LABELS.EMBEDDING, value: 'EMBEDDING' },
  { label: MODEL_TYPE_LABELS.RERANK, value: 'RERANK' },
];

export const PROVIDER_TYPE_LABELS: Record<ModelProviderTypes.ProviderType, string> = {
  aliyun: '阿里云百联',
};

export const PROVIDER_TYPE_OPTIONS: Array<OptionLike<ModelProviderTypes.ProviderType>> = [
  { label: PROVIDER_TYPE_LABELS.aliyun, value: 'aliyun' },
];

export function createEmptyModel(): ProviderModelFormValue {
  return {
    modelName: '',
    modelType: 'CHAT',
    supportReasoning: 0,
    supportVision: 0,
    description: '',
    enabled: true,
  };
}

/**
 * 创建阿里云百联新增页面默认表单值。
 * @returns 新增模型提供商页面默认值
 */
export function createAliyunInitialValues(): ProviderFormValues {
  return {
    providerKey: ALIYUN_BAILIAN_PROVIDER_KEY,
    providerName: '阿里云百联',
    providerType: 'aliyun',
    baseUrl: ALIYUN_BAILIAN_BASE_URL,
    apiKey: '',
    description: '阿里云百联 OpenAI 兼容接口预设模板',
    sort: 0,
    models: [],
  };
}

function toNumberOrDefault(value: string | number | undefined | null, fallback = 0): number {
  if (typeof value === 'number' && !Number.isNaN(value)) {
    return value;
  }
  if (typeof value === 'string') {
    const parsed = Number(value);
    return Number.isNaN(parsed) ? fallback : parsed;
  }
  return fallback;
}

export function mapModelToFormValue(
  model: ModelProviderTypes.ProviderModelVo,
): ProviderModelFormValue {
  return {
    modelName: model.modelName || '',
    modelType: model.modelType,
    supportReasoning: model.supportReasoning ?? 0,
    supportVision: model.supportVision ?? 0,
    description: model.description || '',
    enabled: model.enabled !== 1,
  };
}

export function mapPresetDetailToFormValues(
  detail: ModelProviderTypes.ProviderPresetDetail,
): ProviderFormValues {
  return {
    providerKey: detail.providerKey,
    providerName: detail.providerName || '',
    providerType: detail.providerType,
    baseUrl: detail.baseUrl || '',
    apiKey: '',
    description: detail.description || '',
    sort: 0,
    models: (detail.models || []).map(mapModelToFormValue),
  };
}

export function mapProviderDetailToFormValues(
  detail: ModelProviderTypes.ProviderDetailVo,
): ProviderFormValues {
  return {
    providerKey: undefined,
    providerName: detail.providerName || '',
    providerType: detail.providerType,
    baseUrl: detail.baseUrl || '',
    apiKey: detail.apiKey || '',
    description: detail.description || '',
    sort: toNumberOrDefault(detail.sort, 0),
    models: (detail.models || []).map(mapModelToFormValue),
  };
}

export function buildProviderListRequest(params: {
  current?: number;
  pageSize?: number;
  providerName?: string;
}): ModelProviderTypes.ProviderListRequest {
  return {
    pageNum: Number(params.current ?? 1),
    pageSize: Number(params.pageSize ?? 10),
    providerName: params.providerName || undefined,
  };
}

function trimToUndefined(value?: string): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

export function buildProviderPayload(
  values: ProviderFormValues,
  mode: 'create' | 'edit',
  id?: string,
): ModelProviderTypes.ProviderCreateRequest | ModelProviderTypes.ProviderUpdateRequest {
  const models = values.models.map((model, index) => ({
    modelName: model.modelName?.trim() || '',
    modelType: model.modelType || 'CHAT',
    supportReasoning: model.supportReasoning || 0,
    supportVision: model.supportVision || 0,
    description: trimToUndefined(model.description),
    enabled: model.enabled === false ? 1 : 0,
    sort: index,
  }));

  const payloadBase = {
    providerKey: trimToUndefined(values.providerKey),
    providerName: values.providerName?.trim() || '',
    providerType: values.providerType!,
    baseUrl: values.baseUrl?.trim() || '',
    description: trimToUndefined(values.description),
    sort: typeof values.sort === 'number' ? values.sort : 0,
    models,
  };

  if (mode === 'edit') {
    const payload: ModelProviderTypes.ProviderUpdateRequest = {
      id: id || '',
      ...payloadBase,
    };
    return payload;
  }

  return {
    ...payloadBase,
    apiKey: values.apiKey?.trim() || '',
  };
}
