import { requestClient } from '@/utils/request';

export namespace ModelProviderTypes {
  /**
   * 提供商类型
   */
  export type ProviderType = 'aliyun';

  /**
   * 模型类型
   * CHAT - 对话模型
   * EMBEDDING - 向量模型
   * RERANK - 重排模型
   */
  export type ModelType = 'CHAT' | 'EMBEDDING' | 'RERANK';

  /**
   * 预设提供商简要信息
   */
  export interface ProviderPresetItem {
    /** 预设提供商唯一标识键 */
    providerKey: string;
    /** 提供商名称 */
    providerName: string;
    /** 提供商类型 */
    providerType: ProviderType;
    /** 默认的基础请求地址 */
    baseUrl: string;
    /** 提供商描述说明 */
    description?: string;
  }

  /**
   * 模型实体视图对象
   */
  export interface ProviderModelVo {
    /** 模型主键 ID */
    id?: string | null;
    /** 所属提供商 ID */
    providerId?: string | null;
    /** 模型实际名称（调用接口传参值） */
    modelName: string;
    /** 模型类型 */
    modelType: ModelType;
    /** 是否支持深度思考 0-否 1-是 */
    supportReasoning?: number;
    /** 是否支持图片识别 0-否 1-是 */
    supportVision?: number;
    /** 模型描述 */
    description?: string;
    /** 是否启用 0-启用 1-停用 */
    enabled?: number;
    /** 排序值 */
    sort?: number;
    /** 创建时间 */
    createdAt?: string | null;
    /** 更新时间 */
    updatedAt?: string | null;
  }

  /**
   * 预设提供商详情（包含默认模型列表）
   */
  export interface ProviderPresetDetail extends ProviderPresetItem {
    /** 预设的模型列表 */
    models: ProviderModelVo[];
  }

  /**
   * 提供商列表分页查询参数
   */
  export interface ProviderListRequest {
    /** 当前页码 */
    pageNum?: number;
    /** 每页条数 */
    pageSize?: number;
    /** 提供商名称模糊搜索 */
    providerName?: string;
  }

  /**
   * 提供商列表项视图对象
   */
  export interface ProviderListVo {
    /** 提供商主键 ID */
    id: string;
    /** 提供商编码 */
    providerCode?: string;
    /** 提供商名称 */
    providerName: string;
    /** 提供商类型 */
    providerType: ProviderType;
    /** 基础请求地址 */
    baseUrl: string;
    /** 描述说明 */
    description?: string;
    /** 排序值 */
    sort?: number;
    /** 启用状态 1-启用 0-停用 */
    status?: number;
    /** 总模型数 */
    modelCount?: number;
    /** 对话模型数 */
    chatModelCount?: number;
    /** 向量模型数 */
    embeddingModelCount?: number;
    /** 重排模型数 */
    rerankModelCount?: number;
    /** 创建时间 */
    createdAt?: string;
    /** 更新时间 */
    updatedAt?: string;
  }

  /**
   * 提供商详情视图对象（包含模型列表）
   */
  export interface ProviderDetailVo extends ProviderListVo {
    /** 绑定的 API 密钥 */
    apiKey?: string;
    /** 创建人 */
    createBy?: string;
    /** 更新人 */
    updateBy?: string;
    /** 绑定的模型列表 */
    models: ProviderModelVo[];
  }

  /**
   * 提供商分页结果
   */
  export interface ProviderPageResult {
    /** 总条数 */
    total?: string;
    /** 当前页码 */
    pageNum?: string;
    /** 每页条数 */
    pageSize?: string;
    /** 数据行 */
    rows?: ProviderListVo[];
  }

  /**
   * 新增/编辑提供商时的单个模型请求结构
   */
  export interface ProviderModelItemRequest {
    /** 模型实际名称 */
    modelName: string;
    /** 模型类型 */
    modelType: ModelType;
    /** 是否支持深度思考 0-否 1-是 */
    supportReasoning?: number;
    /** 是否支持图片识别 0-否 1-是 */
    supportVision?: number;
    /** 模型描述 */
    description?: string;
    /** 是否启用 0-启用 1-停用 */
    enabled?: number;
    /** 排序值 */
    sort?: number;
  }

  /**
   * 新增提供商请求体
   */
  export interface ProviderCreateRequest {
    /** 如果是基于预设创建，需要传入对应的 key */
    providerKey?: string;
    /** 提供商名称 */
    providerName: string;
    /** 提供商类型 */
    providerType: ProviderType;
    /** 基础请求地址 */
    baseUrl: string;
    /** API 密钥 */
    apiKey: string;
    /** 描述说明 */
    description?: string;
    /** 排序值 */
    sort?: number;
    /** 绑定的模型列表 */
    models: ProviderModelItemRequest[];
  }

  /**
   * 编辑提供商请求体
   */
  export interface ProviderUpdateRequest {
    /** 提供商主键 ID */
    id: string;
    /** 提供商键值（预设才有） */
    providerKey?: string;
    /** 提供商名称 */
    providerName?: string;
    /** 提供商类型 */
    providerType?: ProviderType;
    /** 基础请求地址 */
    baseUrl?: string;
    /** 描述说明 */
    description?: string;
    /** 排序值 */
    sort?: number;
    /** 绑定的全量模型列表（覆盖更新） */
    models: ProviderModelItemRequest[];
  }

  /**
   * 单独修改 API Key 的请求参数
   */
  export interface ProviderUpdateApiKeyRequest {
    /** 提供商主键 ID */
    id: number;
    /** 新的 API Key */
    apiKey: string;
  }

  /**
   * 连通性测试请求参数
   */
  export interface ProviderConnectivityTestRequest {
    /** 基础请求地址 */
    baseUrl: string;
    /** API 密钥 */
    apiKey: string;
  }

  /**
   * 连通性测试响应结果
   */
  export interface ProviderConnectivityTestResponse {
    /** 是否连通成功 */
    success: boolean;
    /** HTTP 状态码；网络异常时为空 */
    httpStatus: number | null;
    /** 后端实际请求的地址 */
    endpoint: string;
    /** 请求耗时，单位毫秒 */
    latencyMs: number;
    /** 结果说明文案 */
    message: string;
  }

  /**
   * 单独修改提供商状态的请求参数
   */
  export interface ProviderUpdateStatusRequest {
    /** 提供商主键 ID */
    id: string;
    /** 状态值，1=启用，0=停用 */
    status: number;
  }
}

/**
 * 获取预设的提供商列表
 * @returns 预设提供商列表
 */
export async function getProviderPresetList() {
  return requestClient.get<ModelProviderTypes.ProviderPresetItem[]>('/llm/provider/preset/list');
}

/**
 * 获取预设提供商的详情（包含默认模型）
 * @param providerKey 预设提供商标识
 * @returns 预设详情
 */
export async function getProviderPresetDetail(providerKey: string) {
  return requestClient.get<ModelProviderTypes.ProviderPresetDetail>(
    `/llm/provider/preset/${providerKey}`,
  );
}

/**
 * 分页查询提供商列表
 * @param params 查询参数
 * @returns 提供商分页数据
 */
export async function getProviderList(params: ModelProviderTypes.ProviderListRequest) {
  return requestClient.get<ModelProviderTypes.ProviderPageResult>('/llm/provider/list', { params });
}

/**
 * 根据 ID 获取提供商详情（包含模型列表）
 * @param id 提供商 ID
 * @returns 提供商详情
 */
export async function getProviderById(id: string) {
  return requestClient.get<ModelProviderTypes.ProviderDetailVo>(`/llm/provider/${id}`);
}

/**
 * 新增提供商及模型配置
 * @param data 新增请求数据
 * @returns 成功响应
 */
export async function addProvider(data: ModelProviderTypes.ProviderCreateRequest) {
  return requestClient.post('/llm/provider', data);
}

/**
 * 更新提供商及模型配置（模型为全量覆盖）
 * @param data 更新请求数据
 * @returns 成功响应
 */
export async function updateProvider(data: ModelProviderTypes.ProviderUpdateRequest) {
  return requestClient.put('/llm/provider', data);
}

/**
 * 单独更新提供商的 API Key
 * @param data 包含 ID 和 API Key 的数据
 * @returns 成功响应
 */
export async function updateProviderApiKey(data: ModelProviderTypes.ProviderUpdateApiKeyRequest) {
  return requestClient.put('/llm/provider/api-key', data);
}

/**
 * 测试提供商连通性（仅测试不保存）
 * @param data 基础 URL 和 API 密钥
 * @returns 连通性测试结果
 */
export async function testProviderConnectivity(
  data: ModelProviderTypes.ProviderConnectivityTestRequest,
) {
  return requestClient.post<ModelProviderTypes.ProviderConnectivityTestResponse>(
    '/llm/provider/connectivity-test',
    data,
  );
}

/**
 * 删除提供商（级联删除下属模型）
 * @param id 提供商 ID
 * @returns 成功响应
 */
export async function deleteProvider(id: string) {
  return requestClient.delete(`/llm/provider/${id}`);
}

/**
 * 修改大模型提供商状态
 * @param data 包含 ID 和状态的数据
 * @returns 成功响应
 */
export async function updateProviderStatus(data: ModelProviderTypes.ProviderUpdateStatusRequest) {
  return requestClient.put('/llm/provider/status', data);
}
