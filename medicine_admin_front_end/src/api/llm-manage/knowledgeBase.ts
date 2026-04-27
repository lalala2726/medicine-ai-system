import { requestClient } from '@/utils/request';
import type { TableDataResult } from '@/types';

export namespace KnowledgeBaseTypes {
  /**
   * 知识库列表展示对象
   */
  export interface KnowledgeBaseDetail {
    updateTime?: string;
    chunkCount?: string;
    fileCount?: string;
  }

  export interface KnowledgeBaseListVo {
    /** 主键ID */
    id?: number;
    /** 知识库唯一名称（业务键） */
    knowledgeName?: string;
    /** 知识库展示名称 */
    displayName?: string;
    /** 知识库封面地址 */
    cover?: string;
    /** 知识库描述 */
    description?: string;
    /** 状态，0 启用，1 停用 */
    status?: number;
    /** 知识库详情 */
    detail?: KnowledgeBaseDetail;
  }

  export interface KnowledgeBaseListRequest {
    pageNum?: number;
    pageSize?: number;
    /** 知识库展示名称，模糊匹配 */
    displayName?: string;
  }

  export interface KnowledgeBaseVo {
    /** 主键ID */
    id?: number;
    /** 知识库唯一名称（业务键） */
    knowledgeName?: string;
    /** 知识库展示名称 */
    displayName?: string;
    /** 知识库封面地址 */
    cover?: string;
    /** 知识库描述 */
    description?: string;
    /** 向量模型标识 */
    embeddingModel?: string;
    /** 向量维度 */
    embeddingDim?: number;
    /** 状态，0 启用，1 停用 */
    status?: number;
    /** 创建时间 */
    createdAt?: string;
    /** 更新时间 */
    updatedAt?: string;
    /** 切片数量 */
    chunkCount?: number;
  }

  export interface KnowledgeBaseAddRequest {
    /** 知识库业务名称 */
    knowledgeName: string;
    /** 知识库展示名称 */
    displayName: string;
    /** 知识库封面地址 */
    cover: string;
    /** 知识库描述 */
    description?: string;
    /** 向量模型标识 */
    embeddingModel: string;
    /** 向量维度，范围 128 ~ 8192 */
    embeddingDim: number;
    /** 状态，0 启用，1 停用 */
    status?: number;
  }

  export interface KnowledgeBaseUpdateRequest {
    /** 主键ID */
    id: number;
    /** 知识库展示名称 */
    displayName?: string;
    /** 知识库封面地址 */
    cover: string;
    /** 知识库描述 */
    description?: string;
    /** 状态，0 启用，1 停用 */
    status?: number;
  }

  /**
   * 知识库结构化检索请求对象。
   */
  export interface KnowledgeBaseSearchRequest {
    /** 检索问题 */
    question: string;
    /** 参与检索的知识库名称列表 */
    knowledgeNames: string[];
    /** 本次检索使用的重排模型名称，不传则表示不启用重排 */
    rankingModel?: string;
  }

  /**
   * 知识库结构化检索命中结果。
   */
  export interface KnowledgeBaseSearchHit {
    /** 命中结果所在的知识库名称 */
    knowledgeName: string;
    /** 命中结果所在的知识库展示名称 */
    knowledgeDisplayName?: string;
    /** 命中的相似度分数 */
    score: number;
    /** 命中的业务文档 ID */
    documentId?: number | null;
    /** 命中的切片序号 */
    chunkIndex?: number | null;
    /** 命中的切片字符数 */
    charCount?: number | null;
    /** 命中的知识文本内容 */
    content: string;
  }

  /**
   * 知识库结构化检索响应对象。
   */
  export interface KnowledgeBaseSearchResult {
    /** 命中结果列表 */
    hits: KnowledgeBaseSearchHit[];
  }
}

/**
 * 获取知识库列表
 * @param params 查询参数
 */
export async function knowledgeBaseList(params: KnowledgeBaseTypes.KnowledgeBaseListRequest) {
  return requestClient.get<TableDataResult<KnowledgeBaseTypes.KnowledgeBaseListVo>>(
    '/knowledge_base/list',
    {
      params,
    },
  );
}

/**
 * 获取知识库详情
 * @param id 知识库ID
 */
export async function getKnowledgeBaseById(id: number) {
  return requestClient.get<KnowledgeBaseTypes.KnowledgeBaseVo>(`/knowledge_base/${id}`);
}

/**
 * 添加知识库
 * @param data 知识库数据
 */
export async function addKnowledgeBase(data: KnowledgeBaseTypes.KnowledgeBaseAddRequest) {
  return requestClient.post('/knowledge_base', data);
}

/**
 * 更新知识库
 * @param data 知识库数据
 */
export async function updateKnowledgeBase(data: KnowledgeBaseTypes.KnowledgeBaseUpdateRequest) {
  return requestClient.put('/knowledge_base', data);
}

/**
 * 删除知识库
 * @param id 知识库ID
 */
export async function deleteKnowledgeBase(id: number) {
  return requestClient.delete(`/knowledge_base/${id}`);
}

/**
 * 执行知识库结构化检索。
 * @param data 检索请求数据
 */
export async function searchKnowledgeBase(data: KnowledgeBaseTypes.KnowledgeBaseSearchRequest) {
  return requestClient.post<KnowledgeBaseTypes.KnowledgeBaseSearchResult>(
    '/knowledge_base/search',
    data,
  );
}

// ==================== 文档管理 ====================

export namespace KbDocumentTypes {
  export interface DocumentVo {
    /** 文档 ID */
    id: number;
    /** 知识库 ID */
    knowledgeBaseId: number;
    /** 文件名 */
    fileName: string;
    /** 文件访问地址 */
    fileUrl: string;
    /** 文件类型，小写不带点，例如 pdf */
    fileType: string;
    /** 文件大小，单位 Byte，导入刚提交时可能为 null */
    fileSize: number | null;
    /** 切片模式 */
    chunkMode?: string;
    /** 切片长度 */
    chunkSize?: number;
    /** 切片重叠长度 */
    chunkOverlap?: number;
    /** 切片数量 */
    chunkCount?: number;
    /** 文档处理阶段 */
    stage: 'PENDING' | 'STARTED' | 'PROCESSING' | 'INSERTING' | 'COMPLETED' | 'FAILED';
    /** 最近一次处理失败错误信息 */
    lastError?: string;
    /** 创建人账号 */
    createBy?: string;
    /** 最后更新人账号 */
    updateBy?: string;
    /** 创建时间 */
    createdAt: string;
    /** 更新时间 */
    updatedAt: string;
  }

  export interface DocumentListRequest {
    pageNum?: number;
    pageSize?: number;
    /** 文件类型筛选 */
    fileType?: string;
    /** 文件名模糊搜索 */
    fileName?: string;
  }

  export interface DocumentRenameRequest {
    /** 文档 ID */
    id: number;
    /** 修改后的文件名 */
    fileName: string;
  }

  export interface DocumentDeleteRequest {
    /** 文档 ID 列表 */
    documentIds: number[];
  }

  export interface FileDetail {
    /** 文件名 */
    fileName: string;
    /** 文件访问地址 */
    fileUrl: string;
    /** 文件类型 */
    fileType?: string;
  }

  export interface CustomChunkMode {
    /** 切片大小，范围 100-6000 */
    chunkSize: number;
    /** 切片重叠大小，范围 0-1000 */
    chunkOverlap: number;
  }

  export interface DocumentImportRequest {
    /** 知识库 ID */
    knowledgeBaseId: number;
    /** 文件列表 */
    fileDetails: FileDetail[];
    /** 切片模式 */
    chunkMode: 'balancedMode' | 'precisionMode' | 'contextMode' | 'custom';
    /** 自定义切片配置，chunkMode=custom 时必填 */
    customChunkMode?: CustomChunkMode;
  }
}

/**
 * 获取知识库文档列表
 */
export async function getDocumentList(
  knowledgeBaseId: number,
  params: KbDocumentTypes.DocumentListRequest,
) {
  return requestClient.get<TableDataResult<KbDocumentTypes.DocumentVo>>(
    `/knowledge_base/document/${knowledgeBaseId}/list`,
    { params },
  );
}

/**
 * 修改文档文件名
 */
export async function renameDocument(data: KbDocumentTypes.DocumentRenameRequest) {
  return requestClient.put('/knowledge_base/document/file_name', data);
}

/**
 * 删除文档
 */
export async function deleteDocuments(data: KbDocumentTypes.DocumentDeleteRequest) {
  return requestClient.delete('/knowledge_base/document', { data });
}

/**
 * 获取文档详情
 * @param id 文档ID
 */
export async function getDocumentById(id: number) {
  return requestClient.get<KbDocumentTypes.DocumentVo>(`/knowledge_base/document/${id}`);
}

/**
 * 导入文档
 */
export async function importDocuments(data: KbDocumentTypes.DocumentImportRequest) {
  return requestClient.post('/knowledge_base/document/import', data);
}
