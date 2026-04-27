/**
 * 订单状态枚举和配置
 */

// 订单状态枚举
export enum OrderStatus {
  /** 待支付 */
  PENDING_PAYMENT = 'PENDING_PAYMENT',
  /** 待发货 */
  PENDING_SHIPMENT = 'PENDING_SHIPMENT',
  /** 待收货 */
  PENDING_RECEIPT = 'PENDING_RECEIPT',
  /** 已完成 */
  COMPLETED = 'COMPLETED',
  /** 已退款 */
  REFUNDED = 'REFUNDED',
  /** 售后中 */
  AFTER_SALE = 'AFTER_SALE',
  /** 已过期 */
  EXPIRED = 'EXPIRED',
  /** 已取消 */
  CANCELLED = 'CANCELLED',
}

// 订单状态映射配置
export const ORDER_STATUS_MAP = {
  [OrderStatus.PENDING_PAYMENT]: { text: '待支付', color: 'warning' },
  [OrderStatus.PENDING_SHIPMENT]: { text: '待发货', color: 'processing' },
  [OrderStatus.PENDING_RECEIPT]: { text: '待收货', color: 'processing' },
  [OrderStatus.COMPLETED]: { text: '已完成', color: 'success' },
  [OrderStatus.REFUNDED]: { text: '已退款', color: 'error' },
  [OrderStatus.AFTER_SALE]: { text: '售后中', color: 'warning' },
  [OrderStatus.EXPIRED]: { text: '已过期', color: 'error' },
  [OrderStatus.CANCELLED]: { text: '已取消', color: 'error' },
} as const;

// 配送方式枚举
export enum DeliveryType {
  /** 咨询商家 */
  CONSULT_SELLER = 'CONSULT_SELLER',
  /** 用户自提 */
  SELF_PICKUP = 'SELF_PICKUP',
  /** 快递配送 */
  EXPRESS = 'EXPRESS',
  /** 同城配送 */
  CITY_DELIVERY = 'CITY_DELIVERY',
  /** 药店自送 */
  DRUG_STORE_DELIVERY = 'DRUG_STORE_DELIVERY',
  /** 冷链配送 */
  COLD_CHAIN = 'COLD_CHAIN',
  /** 智能药柜取药 */
  PHARMACY_PICKUP_LOCKER = 'PHARMACY_PICKUP_LOCKER',
}

// 配送方式映射配置
export const DELIVERY_TYPE_MAP = {
  [DeliveryType.CONSULT_SELLER]: '咨询商家',
  [DeliveryType.SELF_PICKUP]: '自提',
  [DeliveryType.EXPRESS]: '快递配送',
  [DeliveryType.CITY_DELIVERY]: '同城配送',
  [DeliveryType.DRUG_STORE_DELIVERY]: '药店自送',
  [DeliveryType.COLD_CHAIN]: '冷链配送',
  [DeliveryType.PHARMACY_PICKUP_LOCKER]: '智能药柜取药',
} as const;

// 配送方式选项（用于下拉框）
export const DELIVERY_TYPE_OPTIONS = [
  { label: '咨询商家', value: DeliveryType.CONSULT_SELLER },
  { label: '自提', value: DeliveryType.SELF_PICKUP },
  { label: '快递配送', value: DeliveryType.EXPRESS },
  { label: '同城配送', value: DeliveryType.CITY_DELIVERY },
  { label: '药店自送', value: DeliveryType.DRUG_STORE_DELIVERY },
  { label: '冷链配送', value: DeliveryType.COLD_CHAIN },
  { label: '智能药柜取药', value: DeliveryType.PHARMACY_PICKUP_LOCKER },
];

/**
 * 工具函数：获取订单状态文本
 */
export const getOrderStatusText = (status?: string): string => {
  if (!status) return '-';
  return ORDER_STATUS_MAP[status as OrderStatus]?.text || status;
};

/**
 * 工具函数：获取订单状态颜色
 */
export const getOrderStatusColor = (status?: string): string => {
  if (!status) return 'default';
  return ORDER_STATUS_MAP[status as OrderStatus]?.color || 'default';
};

/**
 * 工具函数：获取配送方式文本
 */
export const getDeliveryTypeText = (type?: string): string => {
  if (!type) return '-';
  return DELIVERY_TYPE_MAP[type as DeliveryType] || type;
};

/**
 * 工具函数：判断订单是否已支付
 */
export const isOrderPaid = (status?: string): boolean => {
  if (!status) return false;
  return status !== OrderStatus.PENDING_PAYMENT && status !== OrderStatus.EXPIRED;
};

/**
 * 工具函数：判断是否可以改价
 */
export const canChangePrice = (status?: string): boolean => {
  return status === OrderStatus.PENDING_PAYMENT;
};

/**
 * 工具函数：判断是否可以退款
 */
export const canRefund = (status?: string): boolean => {
  if (!status) return false;
  return (
    status === OrderStatus.PENDING_SHIPMENT ||
    status === OrderStatus.PENDING_RECEIPT ||
    status === OrderStatus.COMPLETED ||
    status === OrderStatus.AFTER_SALE
  );
};

/**
 * 工具函数：判断是否可以发货
 */
export const canShipOrder = (status?: string): boolean => {
  return status === OrderStatus.PENDING_SHIPMENT;
};

/**
 * 工具函数：判断是否可以确认收货
 */
export const canConfirmReceipt = (status?: string): boolean => {
  return status === OrderStatus.PENDING_RECEIPT;
};

/**
 * 工具函数：判断是否可以取消订单
 */
export const canCancelOrder = (status?: string): boolean => {
  return status === OrderStatus.PENDING_PAYMENT || status === OrderStatus.PENDING_SHIPMENT;
};
