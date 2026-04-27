import { requestClient } from '@/utils/request';
import type { TableDataResult } from '@/types';

export namespace MallAfterSaleTypes {
  export interface AfterSaleListVo {
    /** 售后申请ID */
    id?: string;
    /** 售后单号 */
    afterSaleNo?: string;
    /** 订单编号 */
    orderNo?: string;
    /** 用户ID */
    userId?: string;
    /** 用户昵称 */
    userNickname?: string;
    /** 商品名称 */
    productName?: string;
    /** 商品图片 */
    productImage?: string;
    /** 售后类型 */
    afterSaleType?: string;
    /** 售后类型名称 */
    afterSaleTypeName?: string;
    /** 售后状态 */
    afterSaleStatus?: string;
    /** 售后状态名称 */
    afterSaleStatusName?: string;
    /** 退款金额 */
    refundAmount?: string;
    /** 申请原因 */
    applyReason?: string;
    /** 申请原因名称 */
    applyReasonName?: string;
    /** 申请时间 */
    applyTime?: string;
    /** 审核时间 */
    auditTime?: string;
  }

  export interface AfterSaleListRequest {
    /** 售后类型(REFUND_ONLY/RETURN_REFUND/EXCHANGE) */
    afterSaleType?: string;
    /** 售后状态(PENDING/APPROVED/REJECTED/PROCESSING/COMPLETED/CANCELLED) */
    afterSaleStatus?: string;
    /** 订单编号 */
    orderNo?: string;
    /** 用户ID */
    userId?: string;
    /** 申请原因 */
    applyReason?: string;
  }

  export interface AfterSaleDetailVo {
    /** 售后申请ID */
    id?: string;
    /** 售后单号 */
    afterSaleNo?: string;
    /** 订单ID */
    orderId?: string;
    /** 订单编号 */
    orderNo?: string;
    /** 订单项ID */
    orderItemId?: string;
    /** 用户ID */
    userId?: string;
    /** 用户昵称 */
    userNickname?: string;
    /** 售后类型 */
    afterSaleType?: string;
    /** 售后类型名称 */
    afterSaleTypeName?: string;
    /** 售后状态 */
    afterSaleStatus?: string;
    /** 售后状态名称 */
    afterSaleStatusName?: string;
    /** 退款金额 */
    refundAmount?: string;
    /** 申请原因 */
    applyReason?: string;
    /** 申请原因名称 */
    applyReasonName?: string;
    /** 详细说明 */
    applyDescription?: string;
    /** 凭证图片列表 */
    evidenceImages?: string[];
    /** 收货状态 */
    receiveStatus?: string;
    /** 收货状态名称 */
    receiveStatusName?: string;
    /** 拒绝原因 */
    rejectReason?: string;
    /** 管理员备注 */
    adminRemark?: string;
    /** 申请时间 */
    applyTime?: string;
    /** 审核时间 */
    auditTime?: string;
    /** 完成时间 */
    completeTime?: string;
    /** 商品信息 */
    productInfo?: AfterSaleDetailVo.ProductInfo;
    /** 时间线列表 */
    timeline?: AfterSaleTimelineVo[];
  }

  export namespace AfterSaleDetailVo {
    export interface ProductInfo {
      /** 商品ID */
      productId?: string;
      /** 商品名称 */
      productName?: string;
      /** 商品图片 */
      productImage?: string;
      /** 商品单价 */
      productPrice?: string;
      /** 购买数量 */
      quantity?: number;
      /** 小计金额 */
      totalPrice?: string;
    }
  }

  export interface AfterSaleTimelineVo {
    /** 时间线ID */
    id?: string;
    /** 事件类型 */
    eventType?: string;
    /** 事件类型名称 */
    eventTypeName?: string;
    /** 事件状态 */
    eventStatus?: string;
    /** 操作人类型 */
    operatorType?: string;
    /** 操作人类型名称 */
    operatorTypeName?: string;
    /** 事件描述 */
    description?: string;
    /** 创建时间 */
    createTime?: string;
  }

  export interface AfterSaleAuditRequest {
    /** 售后申请ID */
    afterSaleId: string;
    /** 审核结果(true-通过, false-拒绝) */
    approved: boolean;
    /** 拒绝原因(审核拒绝时必填) */
    rejectReason?: string;
    /** 管理员备注 */
    adminRemark?: string;
  }

  export interface AfterSaleProcessRequest {
    /** 售后申请ID */
    afterSaleId: string;
    /** 处理备注 */
    processRemark?: string;
    /** 换货物流公司(换货时填写) */
    logisticsCompany?: string;
    /** 换货物流单号(换货时填写) */
    trackingNumber?: string;
  }
}

/**
 * 查询售后列表
 * @param params 查询参数
 */
export async function getAfterSaleList(params: MallAfterSaleTypes.AfterSaleListRequest) {
  return requestClient.get<TableDataResult<MallAfterSaleTypes.AfterSaleListVo>>(
    '/mall/after-sale/list',
    {
      params: params,
    },
  );
}

/**
 * 获取售后详情
 * @param afterSaleId 售后申请ID
 */
export async function getAfterSaleDetail(afterSaleId: string) {
  return requestClient.get<MallAfterSaleTypes.AfterSaleDetailVo>(
    `/mall/after-sale/detail/${afterSaleId}`,
  );
}

/**
 * 审核售后申请
 * @param data 审核参数
 */
export async function auditAfterSale(data: MallAfterSaleTypes.AfterSaleAuditRequest) {
  return requestClient.post<void>('/mall/after-sale/audit', data);
}

/**
 * 处理退款
 * @param data 处理参数
 */
export async function processRefund(data: MallAfterSaleTypes.AfterSaleProcessRequest) {
  return requestClient.post<void>('/mall/after-sale/process-refund', data);
}

/**
 * 处理换货
 * @param data 处理参数
 */
export async function processExchange(data: MallAfterSaleTypes.AfterSaleProcessRequest) {
  return requestClient.post<void>('/mall/after-sale/process-exchange', data);
}
