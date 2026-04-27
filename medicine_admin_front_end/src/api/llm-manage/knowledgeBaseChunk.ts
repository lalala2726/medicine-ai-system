import { requestClient } from '@/utils/request';
import type { TableDataResult } from '@/types';

// ==================== 文档切片管理 ====================

export namespace KbChunkTypes {
  /**
   * 文档切片视图对象
   */
  export interface ChunkVo {
    /** 切片主键 ID */
    id: number;
    /** 归属文档 ID */
    documentId: number;
    /** 切片在文档中的序号 */
    chunkIndex: number;
    /** 切片的文本内容 */
    content: string;
    /** 向量化后的向量库记录 ID，为空表示未向量化 */
    vectorId: string | null;
    /** 字符数长度 */
    charCount: number;
    /** 状态 0-启用 1-禁用 */
    status: number;
    /** 处理阶段：PENDING-待处理 STARTED-处理中 COMPLETED-已完成 FAILED-失败 */
    stage: 'PENDING' | 'STARTED' | 'COMPLETED' | 'FAILED';
    /** 创建时间 */
    createdAt: string;
    /** 更新时间 */
    updatedAt: string;
  }

  /**
   * 文档切片列表查询参数
   */
  export interface ChunkListRequest {
    /** 当前页码 */
    pageNum?: number;
    /** 每页条数 */
    pageSize?: number;
  }

  /**
   * 手工新增文档切片请求参数
   */
  export interface ChunkAddRequest {
    /** 归属文档 ID */
    documentId: number;
    /** 切片的文本内容 */
    content: string;
  }

  /**
   * 修改切片内容请求参数
   */
  export interface ChunkUpdateRequest {
    /** 切片主键 ID */
    id: number;
    /** 更新后的文本内容 */
    content: string;
  }

  /**
   * 修改切片状态请求参数
   */
  export interface ChunkUpdateStatusRequest {
    /** 切片主键 ID */
    id: number;
    /** 0=启用，1=禁用 */
    status: 0 | 1;
  }
}

/**
 * 查询指定文档下的切片列表
 * @param documentId 文档主键 ID
 * @param params 分页参数
 * @returns 切片分页数据
 */
export async function getChunkList(documentId: number, params?: KbChunkTypes.ChunkListRequest) {
  return requestClient.get<TableDataResult<KbChunkTypes.ChunkVo>>(
    `/knowledge_base/document/chunk/${documentId}/list`,
    { params },
  );
}

/**
 * 手工新增单个切片
 * @param data 新增切片参数
 * @returns 成功响应
 */
export async function addChunk(data: KbChunkTypes.ChunkAddRequest) {
  return requestClient.post('/knowledge_base/document/chunk', data);
}

/**
 * 修改单个切片内容
 * @param data 更新切片内容参数
 * @returns 成功响应
 */
export async function updateChunkContent(data: KbChunkTypes.ChunkUpdateRequest) {
  return requestClient.put('/knowledge_base/document/chunk/content', data);
}

/**
 * 修改单个切片状态（0=启用，1=禁用）
 * @param data 更新切片状态参数
 * @returns 成功响应
 */
export async function updateChunkStatus(data: KbChunkTypes.ChunkUpdateStatusRequest) {
  return requestClient.put('/knowledge_base/document/chunk/status', data);
}
