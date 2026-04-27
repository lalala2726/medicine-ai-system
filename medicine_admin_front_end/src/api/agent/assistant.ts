/**
 * 智能助手会话管理 API
 *
 * 所有请求使用 /ai_api 前缀，通过代理转发到 AI 服务器。
 */
import { requestClient } from '@/utils/request';

// ======================== 类型定义 ========================

/** 会话列表项 */
export interface ConversationItem {
  /** 会话 UUID */
  conversation_uuid: string;
  /** 会话标题 */
  title: string;
}

/** 分页响应结构 */
export interface PageResponse<T> {
  rows: T[];
  total: number;
  page_num: number;
  page_size: number;
}

// ======================== API 方法 ========================

/**
 * 获取会话列表（分页）
 *
 * @param pageNum  页码，从 1 开始
 * @param pageSize 每页数量，默认 20
 */
export async function getConversationList(
  pageNum = 1,
  pageSize = 20,
): Promise<PageResponse<ConversationItem>> {
  return requestClient.get('/ai_api/admin/assistant/conversation/list', {
    params: { page_num: pageNum, page_size: pageSize },
  });
}

/**
 * 删除会话
 *
 * @param conversationUuid 会话 UUID
 */
export async function deleteConversation(conversationUuid: string): Promise<void> {
  return requestClient.delete(`/ai_api/admin/assistant/conversation/${conversationUuid}`);
}

/**
 * 修改会话标题
 *
 * @param conversationUuid 会话 UUID
 * @param title            新标题
 */
export async function updateConversationTitle(
  conversationUuid: string,
  title: string,
): Promise<void> {
  return requestClient.put(`/ai_api/admin/assistant/conversation/${conversationUuid}`, { title });
}

/**
 * 获取会话历史消息（分页，按最新排序）
 *
 * @param conversationUuid 会话 UUID
 * @param pageNum          页码，从 1 开始
 * @param pageSize         每页数量，默认 50
 * @returns Message[] 数组（已按时间升序排列）
 */
export async function getConversation(
  conversationUuid: string,
  pageNum = 1,
  pageSize = 50,
): Promise<any[]> {
  return requestClient.get(`/ai_api/admin/assistant/history/${conversationUuid}`, {
    params: { page_num: pageNum, page_size: pageSize },
  });
}
