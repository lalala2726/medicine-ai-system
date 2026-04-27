import { requestClient } from '@/utils/request';
import type { PageRequest, TableDataResult } from '@/types';

export namespace MessageTypes {
  /**
   * 管理端通知消息列表视图对象
   */
  export interface NotifyMessageListVo {
    /** 通知ID */
    id?: string;
    /** 通知标题 */
    title?: string;
    /** 通知类型 */
    type?: string;
    /** 发送者类型 */
    senderType?: string;
    /** 发送者名称 */
    senderName?: string;
    /** 接收者类型 */
    receiverType?: string;
    /** 发布时间 */
    publishTime?: string;
    /** 创建时间 */
    createTime?: string;
  }

  /**
   * 管理端通知消息详情视图对象
   */
  export interface NotifyMessageDetailVo {
    /** 通知ID */
    id?: string;
    /** 通知标题 */
    title?: string;
    /** 通知内容 */
    content?: string;
    /** 发送者类型 */
    senderType?: string;
    /** 发送者ID */
    senderId?: string;
    /** 发送者名称 */
    senderName?: string;
    /** 接收者类型 */
    receiverType?: string;
    /** 通知类型 */
    type?: string;
    /** 发布时间 */
    publishTime?: string;
    /** 创建时间 */
    createTime?: string;
    /** 指定接收用户ID */
    receiverId?: number;
    /** 指定接收用户ID列表 */
    receiverIds?: number[];
    /** 接收用户ID(兼容字段) */
    userId?: number;
    /** 接收用户ID(兼容字段) */
    receiverUserId?: number;
  }

  /**
   * 管理端发送通知消息请求
   */
  export interface NotifyMessageSendRequest {
    /** 通知标题 */
    title: string;
    /** 通知内容 */
    content: string;
    /** 通知类型(ORDER/DRUG/SYSTEM) */
    type: string;
    /** 接收者类型(ALL_USER/DESIGNATED_USER) */
    receiverType: string;
    /** 发送者名称(不填写默认系统通知) */
    senderName?: string;
    /** 指定用户ID列表，仅 DESIGNATED_USER 需要 */
    receiverIds?: number[];
  }

  /**
   * 管理端编辑通知消息请求
   */
  export interface NotifyMessageUpdateRequest {
    /** 通知ID */
    id: string;
    /** 通知标题 */
    title: string;
    /** 通知内容 */
    content: string;
    /** 通知类型(ORDER/DRUG/SYSTEM) */
    type: string;
  }

  /**
   * 管理端通知消息分页查询请求
   */
  export interface NotifyMessageListRequest extends PageRequest {
    /** 通知标题(模糊查询) */
    title?: string;
    /** 通知类型(ORDER/DRUG/SYSTEM) */
    type?: string;
    /** 接收者类型(ALL_USER/DESIGNATED_USER) */
    receiverType?: string;
    /** 发送者类型(SYSTEM/ADMIN) */
    senderType?: string;
  }
}

/**
 * 分页查询通知消息列表
 * @param params 查询条件
 */
export async function notifyMessageList(params?: MessageTypes.NotifyMessageListRequest) {
  return requestClient.get<TableDataResult<MessageTypes.NotifyMessageListVo>>('/notify/message/list', {
    params,
  });
}

/**
 * 获取通知消息详情
 * @param id 通知ID
 */
export async function getNotifyMessageDetail(id: string | number) {
  return requestClient.get<MessageTypes.NotifyMessageDetailVo>(`/notify/message/${id}`);
}

/**
 * 发送通知消息
 * @param data 发送参数
 */
export async function sendNotifyMessage(data: MessageTypes.NotifyMessageSendRequest) {
  return requestClient.post('/notify/message', data);
}

/**
 * 编辑通知消息
 * @param data 编辑参数
 */
export async function updateNotifyMessage(data: MessageTypes.NotifyMessageUpdateRequest) {
  return requestClient.put('/notify/message', data);
}

/**
 * 删除通知消息
 * @param ids 通知ID列表
 */
export async function deleteNotifyMessages(ids: (string | number)[]) {
  return requestClient.delete(`/notify/message/${ids.join(',')}`);
}
