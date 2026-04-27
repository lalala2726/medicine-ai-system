import { requestClient } from '@/utils/request';

export namespace EsIndexTypes {
  /** 商品索引重建状态。 */
  export interface ProductIndexRebuildStatusVo {
    /** 当前是否正在重建。 */
    running?: boolean;
    /** 当前触发来源。 */
    triggerSource?: 'startup' | 'manual' | string;
    /** 当前已处理的商品数量。 */
    processedCount?: number;
    /** 本次预计处理的商品总数量。 */
    totalCount?: number;
    /** 当前已完成的批次数量。 */
    batchCount?: number;
    /** 本次重建开始时间。 */
    startedTime?: string;
    /** 最近一次完成时间。 */
    finishedTime?: string;
    /** 最近一次错误信息。 */
    lastError?: string;
  }

  /** Elasticsearch 与商品索引概览。 */
  export interface EsIndexConfigVo {
    /** Elasticsearch 是否可用。 */
    esAvailable?: boolean;
    /** 商品索引名称。 */
    indexName?: string;
    /** 商品索引是否存在。 */
    indexExists?: boolean;
    /** 当前商品索引文档数量。 */
    documentCount?: number;
    /** 是否启用启动自动重建。 */
    startupAutoRebuildEnabled?: boolean;
    /** 启动自动重建触发策略说明。 */
    startupTriggerPolicy?: string;
    /** 商品索引重建运行状态。 */
    rebuildStatus?: ProductIndexRebuildStatusVo;
  }
}

/**
 * 查询 Elasticsearch 与商品索引概览。
 * @returns Elasticsearch 与商品索引概览。
 */
export async function getEsIndexConfig() {
  return requestClient.get<EsIndexTypes.EsIndexConfigVo>('/system/config/es-index');
}

/**
 * 手动触发商品索引全量重建。
 * @returns true 表示已成功提交重建任务。
 */
export async function triggerEsIndexRebuild() {
  return requestClient.post<boolean>('/system/config/es-index/rebuild');
}
