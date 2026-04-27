import requestClient from '@/request/requestClient'

export namespace OrderAfterSaleTypes {
  export enum AfterSaleReasonEnum {
    /** 收货地址填错了 */
    ADDRESS_ERROR = 'ADDRESS_ERROR',
    /** 与描述不符 */
    NOT_AS_DESCRIBED = 'NOT_AS_DESCRIBED',
    /** 信息填错了，重新拍 */
    INFO_ERROR = 'INFO_ERROR',
    /** 收到商品损坏了 */
    DAMAGED = 'DAMAGED',
    /** 未按预定时间发货 */
    DELAYED = 'DELAYED',
    /** 其它原因 */
    OTHER = 'OTHER'
  }

  export enum AfterSaleTypeEnum {
    /** 仅退款 */
    REFUND_ONLY = 'REFUND_ONLY',
    /** 退货退款 */
    RETURN_REFUND = 'RETURN_REFUND',
    /** 换货 */
    EXCHANGE = 'EXCHANGE'
  }

  export enum AfterSaleScopeEnum {
    /** 整单 */
    ORDER = 'ORDER',
    /** 单商品 */
    ITEM = 'ITEM'
  }

  /** 统一售后申请请求 */
  export interface AfterSaleApplyRequest {
    /** 订单编号 */
    orderNo: string
    /** 申请范围 ORDER-整单 / ITEM-单商品 */
    scope: AfterSaleScopeEnum
    /** 订单项ID，scope=ITEM 时必传 */
    orderItemId?: string
    /** 售后类型(REFUND_ONLY-仅退款, RETURN_REFUND-退货退款, EXCHANGE-换货) */
    afterSaleType: AfterSaleTypeEnum
    /** 退款金额，scope=ITEM 时必传 */
    refundAmount?: string
    /** 申请原因 */
    applyReason: AfterSaleReasonEnum
    /** 详细说明 */
    applyDescription?: string
    /** 凭证图片URL列表 */
    evidenceImages?: string[]
  }

  /** 统一售后申请响应 */
  export interface AfterSaleApplyResponse {
    /** 订单编号 */
    orderNo: string
    /** 前端传入范围 */
    requestedScope: string
    /** 后端最终生效范围 */
    resolvedScope: string
    /** 生成的售后单号列表 */
    afterSaleNos: string[]
    /** 本次实际创建售后的订单项 ID 列表 */
    orderItemIds: number[]
  }

  /** 资格校验请求 */
  export interface EligibilityRequest {
    /** 订单编号 */
    orderNo: string
    /** 申请范围 ORDER-整单 / ITEM-单商品 */
    scope?: AfterSaleScopeEnum
    /** 订单项ID，scope=ITEM 时建议传 */
    orderItemId?: number
  }

  /** 资格校验商品维度结果 */
  export interface EligibilityItemVo {
    orderItemId: number
    productId: number
    productName: string
    imageUrl: string
    quantity: number
    price: number
    totalPrice: number
    refundedAmount: number
    refundableAmount: number
    afterSaleStatus: string
    eligible: boolean
    reasonCode: string
    reasonMessage: string
  }

  /** 资格校验响应 */
  export interface EligibilityResponse {
    orderNo: string
    requestedScope: string
    resolvedScope: string
    eligible: boolean
    reasonCode: string
    reasonMessage: string
    orderStatus: string
    orderStatusName: string
    selectedOrderItemId: number
    selectedRefundableAmount: number
    totalRefundableAmount: number
    afterSaleDeadlineTime?: string
    items: EligibilityItemVo[]
  }

  export interface AfterSaleCancelRequest {
    /** 售后申请ID */
    afterSaleId: string
    /** 取消原因 */
    cancelReason?: string
  }

  export interface AfterSaleListRequest {
    /** 售后类型(REFUND_ONLY/RETURN_REFUND/EXCHANGE) */
    afterSaleType?: string
    /** 售后状态(PENDING/APPROVED/REJECTED/PROCESSING/COMPLETED/CANCELLED) */
    afterSaleStatus?: string
    /** 订单编号 */
    orderNo?: string
    /** 当前页码 */
    pageNum?: number
    /** 每页数量 */
    pageSize?: number
  }

  export interface AfterSaleListVo {
    /** 售后申请ID */
    id?: string
    /** 售后单号 */
    afterSaleNo?: string
    /** 订单编号 */
    orderNo?: string
    /** 用户ID */
    userId?: string
    /** 用户昵称 */
    userNickname?: string
    /** 商品名称 */
    productName?: string
    /** 商品图片 */
    productImage?: string
    /** 售后类型 */
    afterSaleType?: string
    /** 售后类型名称 */
    afterSaleTypeName?: string
    /** 售后状态 */
    afterSaleStatus?: string
    /** 售后状态名称 */
    afterSaleStatusName?: string
    /** 退款金额 */
    refundAmount?: string
    /** 申请原因名称 */
    applyReasonName?: string
    /** 申请时间 */
    applyTime?: string
    /** 审核时间 */
    auditTime?: string
  }

  export interface AfterSaleDetailVo {
    /** 售后申请ID */
    id?: string
    /** 售后单号 */
    afterSaleNo?: string
    /** 订单ID */
    orderId?: string
    /** 订单编号 */
    orderNo?: string
    /** 订单项ID */
    orderItemId?: string
    /** 用户ID */
    userId?: string
    /** 用户昵称 */
    userNickname?: string
    /** 售后类型 */
    afterSaleType?: string
    /** 售后类型名称 */
    afterSaleTypeName?: string
    /** 售后状态 */
    afterSaleStatus?: string
    /** 售后状态名称 */
    afterSaleStatusName?: string
    /** 退款金额 */
    refundAmount?: string
    /** 申请原因 */
    applyReason?: string
    /** 申请原因名称 */
    applyReasonName?: string
    /** 详细说明 */
    applyDescription?: string
    /** 凭证图片列表 */
    evidenceImages?: string[]
    /** 收货状态 */
    receiveStatus?: string
    /** 收货状态名称 */
    receiveStatusName?: string
    /** 拒绝原因 */
    rejectReason?: string
    /** 管理员备注 */
    adminRemark?: string
    /** 申请时间 */
    applyTime?: string
    /** 审核时间 */
    auditTime?: string
    /** 完成时间 */
    completeTime?: string
    /** 商品信息 */
    productInfo?: ProductInfo
    /** 时间线列表 */
    timeline?: AfterSaleTimelineVo[] // Assuming AfterSaleTimelineVo is defined elsewhere
  }

  export interface ProductInfo {
    /** 商品ID */
    productId?: string
    /** 商品名称 */
    productName?: string
    /** 商品图片 */
    productImage?: string
    /** 商品单价 */
    productPrice?: string
    /** 购买数量 */
    quantity?: number
    /** 小计金额 */
    totalPrice?: string
  }

  export interface AfterSaleReapplyRequest {
    /** 售后单号 */
    afterSaleNo: string
    /** 申请原因 */
    applyReason: AfterSaleReasonEnum
    /** 详细说明 */
    applyDescription?: string
    /** 凭证图片URL列表 */
    evidenceImages?: string[]
  }

  export interface AfterSaleTimelineVo {
    /** 时间线ID */
    id?: string
    /** 事件类型 */
    eventType?: string
    /** 事件类型名称 */
    eventTypeName?: string
    /** 事件状态 */
    eventStatus?: string
    /** 操作人类型 */
    operatorType?: string
    /** 操作人类型名称 */
    operatorTypeName?: string
    /** 事件描述 */
    description?: string
    /** 创建时间 */
    createTime?: string
  }

  export interface ApiResponse<T = any> {
    code: number
    msg: string
    data: T
  }

  export interface TableDataResponse {
    code: number
    msg: string
    rows: AfterSaleListVo[]
    total: number
  }
}

/**
 * 申请售后
 * @param data 申请售后参数
 */
export const applyAfterSale = (data: OrderAfterSaleTypes.AfterSaleApplyRequest) => {
  return requestClient.post<OrderAfterSaleTypes.AfterSaleApplyResponse>('/mall/order/after_sale/apply', data)
}

/**
 * 售后资格校验
 */
export const checkAfterSaleEligibility = (params: OrderAfterSaleTypes.EligibilityRequest) => {
  return requestClient.get<OrderAfterSaleTypes.EligibilityResponse>('/mall/order/after_sale/eligibility', { params })
}

/**
 * 取消售后
 * @param data 取消售后参数
 */
export const cancelAfterSale = (data: OrderAfterSaleTypes.AfterSaleCancelRequest) => {
  return requestClient.post<void>('/mall/order/after_sale/cancel', data)
}

/**
 * 重新申请售后
 * @param data 重新申请售后参数
 */
export const reapplyAfterSale = (data: OrderAfterSaleTypes.AfterSaleReapplyRequest) => {
  return requestClient.post<void>('/mall/order/after_sale/reapply', data)
}

/**
 * 查询售后列表
 * @param params 查询参数
 */
export const getAfterSaleList = (params?: OrderAfterSaleTypes.AfterSaleListRequest) => {
  return requestClient.get<OrderAfterSaleTypes.TableDataResponse>('/mall/order/after_sale/list', { params })
}

/**
 * 售后详情
 * @param afterSaleNo 售后单号
 */
export const getAfterSaleDetail = (afterSaleNo: string) => {
  return requestClient.get<OrderAfterSaleTypes.AfterSaleDetailVo>(`/mall/order/after_sale/detail/${afterSaleNo}`)
}
