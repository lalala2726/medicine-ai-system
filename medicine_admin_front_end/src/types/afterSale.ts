/**
 * 售后类型、状态、原因枚举和配置
 */

/**
 * 售后类型枚举
 */
export enum AfterSaleType {
  /** 仅退款 */
  REFUND_ONLY = 'REFUND_ONLY',
  /** 退货退款 */
  RETURN_REFUND = 'RETURN_REFUND',
  /** 换货 */
  EXCHANGE = 'EXCHANGE',
}

/**
 * 售后类型映射配置
 */
export const AFTER_SALE_TYPE_MAP = {
  [AfterSaleType.REFUND_ONLY]: {
    text: '仅退款',
    color: 'orange',
    description: '不需要退货，直接退款',
  },
  [AfterSaleType.RETURN_REFUND]: {
    text: '退货退款',
    color: 'red',
    description: '需要退货后才能退款',
  },
  [AfterSaleType.EXCHANGE]: { text: '换货', color: 'blue', description: '更换商品' },
} as const;

/**
 * 售后状态枚举
 */
export enum AfterSaleStatus {
  /** 待审核 */
  PENDING = 'PENDING',
  /** 已通过 */
  APPROVED = 'APPROVED',
  /** 已拒绝 */
  REJECTED = 'REJECTED',
  /** 处理中 */
  PROCESSING = 'PROCESSING',
  /** 已完成 */
  COMPLETED = 'COMPLETED',
  /** 已取消 */
  CANCELLED = 'CANCELLED',
}

/**
 * 售后状态映射配置
 */
export const AFTER_SALE_STATUS_MAP = {
  [AfterSaleStatus.PENDING]: {
    text: '待审核',
    color: 'gold',
    description: '用户已提交售后申请，等待管理员审核',
  },
  [AfterSaleStatus.APPROVED]: {
    text: '已通过',
    color: 'green',
    description: '管理员审核通过，等待处理退款',
  },
  [AfterSaleStatus.REJECTED]: { text: '已拒绝', color: 'red', description: '管理员审核拒绝' },
  [AfterSaleStatus.PROCESSING]: {
    text: '处理中',
    color: 'blue',
    description: '正在处理退款或换货',
  },
  [AfterSaleStatus.COMPLETED]: { text: '已完成', color: 'success', description: '售后处理完成' },
  [AfterSaleStatus.CANCELLED]: {
    text: '已取消',
    color: 'default',
    description: '用户取消售后申请',
  },
} as const;

/**
 * 售后申请原因枚举
 */
export enum AfterSaleReason {
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
  OTHER = 'OTHER',
}

/**
 * 售后原因映射配置
 */
export const AFTER_SALE_REASON_MAP = {
  [AfterSaleReason.ADDRESS_ERROR]: '收货地址填错了',
  [AfterSaleReason.NOT_AS_DESCRIBED]: '与描述不符',
  [AfterSaleReason.INFO_ERROR]: '信息填错了，重新拍',
  [AfterSaleReason.DAMAGED]: '收到商品损坏了',
  [AfterSaleReason.DELAYED]: '未按预定时间发货',
  [AfterSaleReason.OTHER]: '其它原因',
} as const;

/**
 * 获取售后类型文本
 */
export const getAfterSaleTypeText = (type?: string): string => {
  if (!type) return '-';
  return AFTER_SALE_TYPE_MAP[type as AfterSaleType]?.text || type;
};

/**
 * 获取售后状态文本
 */
export const getAfterSaleStatusText = (status?: string): string => {
  if (!status) return '-';
  return AFTER_SALE_STATUS_MAP[status as AfterSaleStatus]?.text || status;
};

/**
 * 获取售后原因文本
 */
export const getAfterSaleReasonText = (reason?: string): string => {
  if (!reason) return '-';
  return AFTER_SALE_REASON_MAP[reason as AfterSaleReason] || reason;
};
