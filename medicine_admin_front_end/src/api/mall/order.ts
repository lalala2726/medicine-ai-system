import { requestClient } from '@/utils/request';
import type { TableDataResult } from '@/types';

import OrderPriceVo = MallOrderTypes.OrderPriceVo;
import OrderRemarkVo = MallOrderTypes.OrderRemarkVo;
import OrderAddressVo = MallOrderTypes.OrderAddressVo;

export namespace MallOrderTypes {
  export interface MallOrderListRequest {
    /** 订单编号 */
    orderNo?: string;
    /** 支付方式编码 */
    payType?: string;
    /** 订单状态编码 */
    orderStatus?: string;
    /** 配送方式编码 */
    deliveryType?: string;
    /** 收货人姓名 */
    receiverName?: string;
    /** 收货人电话 */
    receiverPhone?: string;
  }

  export interface MallOrderListVo {
    /** 订单ID */
    id?: string;
    /** 订单编号（业务唯一标识） */
    orderNo?: string;
    /** 订单总金额 */
    totalAmount?: string;
    /** 使用的优惠券ID */
    couponId?: string;
    /** 优惠券名称 */
    couponName?: string;
    /** 订单抵扣金额 */
    couponDeductAmount?: string;
    /** 实际支付金额 */
    payAmount?: string;
    /** 支付方式编码（大驼峰） */
    payType?: string;
    /** 订单状态编码（大驼峰） */
    orderStatus?: string;
    /** 配送方式编码（大驼峰） */
    deliveryType?: string;
    /** 支付时间 */
    payTime?: string;
    /** 创建时间 */
    createTime?: string;
    /** 商品信息 */
    productInfo: ProductInfo;
  }

  export interface OrderDetailVo {
    /** 用户信息 */
    userInfo?: OrderDetailVo.UserInfo;
    /** 配送信息 */
    deliveryInfo?: OrderDetailVo.DeliveryInfo;
    /** 订单信息 */
    orderInfo?: OrderDetailVo.OrderInfo;
    /** 商品信息 */
    productInfo?: OrderDetailVo.ProductInfo[];
  }

  export interface ProductInfo {
    /** 商品名称 */
    productName?: string;
    /** 商品图片 */
    productImage?: string;
    /** 商品数量 */
    quantity?: number;
  }

  export namespace OrderDetailVo {
    export interface UserInfo {
      /** 用户ID */
      userId?: string;
      /** 用户昵称 */
      nickname?: string;
      /** 用户手机号 */
      phoneNumber?: string;
    }

    export interface DeliveryInfo {
      /** 收货人 */
      receiverName?: string;
      /** 收货地址 */
      receiverAddress?: string;
      /** 收货人电话 */
      receiverPhone?: string;
      /** 配送方式 */
      deliveryMethod?: string;
    }

    export interface OrderInfo {
      /** 订单编号 */
      orderNo?: string;
      /** 订单状态 */
      orderStatus?: string;
      /** 支付方式 */
      payType?: string;
      /** 商品原始总金额 */
      itemsAmount?: string;
      /** 订单总金额 */
      totalAmount?: string;
      /** 实际支付金额 */
      payAmount?: string;
      /** 使用的优惠券ID */
      couponId?: string;
      /** 优惠券名称 */
      couponName?: string;
      /** 订单抵扣金额 */
      couponDeductAmount?: string;
      /** 优惠券消耗金额 */
      couponConsumeAmount?: string;
    }

    export interface ProductInfo {
      /** 商品ID */
      productId?: string;
      /** 商品名称 */
      productName?: string;
      /** 商品图片 */
      productImage?: string;
      /** 商品价格 */
      productPrice?: string;
      /** 商品数量 */
      productQuantity?: number;
      /** 商品总价 */
      productTotalAmount?: string;
      /** 分摊优惠金额 */
      couponDeductAmount?: string;
      /** 应付金额 */
      payableAmount?: string;
    }
  }

  // 请求参数类型
  export interface AddressUpdateRequest {
    /** 订单ID */
    orderId: string;
    /** 收货人 */
    receiverName: string;
    /** 收货地址 */
    receiverAddress: string;
    /** 收货人电话 */
    receiverPhone: string;
    /** 配送方式 */
    deliveryType?: string;
  }

  export interface RemarkUpdateRequest {
    /** 订单ID */
    orderId: string;
    /** 订单备注 */
    remark: string;
  }

  export interface OrderUpdatePriceRequest {
    /** 订单ID */
    orderId: string;
    /** 订单价格 */
    price: string;
  }

  export interface OrderRefundRequest {
    /** 订单ID */
    orderNo: string;
    /** 退款金额 */
    refundAmount?: string;
    /** 退款原因 */
    refundReason?: string;
  }

  export interface OrderCancelRequest {
    /** 订单ID */
    orderId: string;
    /** 取消原因 */
    cancelReason?: string;
  }

  export interface MallOrderTimelineVo {
    /** 主键 */
    id?: string;
    /** 订单ID */
    orderId?: string;
    /** 事件类型 */
    eventType?: string;
    /** 事件状态 */
    eventStatus?: string;
    /** 操作方(USER/ADMIN/SYSTEM) */
    operatorType?: string;
    /** 事件描述 */
    description?: string;
    /** 事件时间 */
    createdTime?: string;
  }

  export interface OrderAddressVo {
    /** 订单ID */
    orderId?: string;
    /** 订单号 */
    orderNo?: string;
    /** 订单状态 */
    orderStatus?: string;
    /** 收货人姓名 */
    receiverName?: string;
    /** 收货人电话 */
    receiverPhone?: string;
    /** 收货详细地址 */
    receiverDetail?: string;
    /** 配送方式 */
    deliveryType?: string;
  }

  export interface OrderPriceVo {
    /** 订单ID */
    orderId?: string;
    /** 订单号 */
    orderNo?: string;
    /** 商品原始总金额 */
    itemsAmount?: string;
    /** 订单总金额 */
    totalAmount?: string;
    /** 使用的优惠券ID */
    couponId?: string;
    /** 优惠券名称 */
    couponName?: string;
    /** 订单抵扣金额 */
    couponDeductAmount?: string;
  }

  export interface OrderRemarkVo {
    /** 订单ID */
    orderId?: string;
    /** 订单号 */
    orderNo?: string;
    /** 订单备注 */
    remark?: string;
    /** 用户留言 */
    note?: string;
  }

  export interface OrderShipRequest {
    /** 订单ID */
    orderId: string;
    /** 物流公司 */
    logisticsCompany: string;
    /** 物流单号 */
    trackingNumber: string;
    /** 发货备注 */
    shipmentNote?: string;
  }

  export interface OrderShippingVo {
    /** 订单ID */
    orderId?: string;
    /** 订单号 */
    orderNo?: string;
    /** 订单状态 */
    orderStatus?: string;
    /** 订单状态名称 */
    orderStatusName?: string;
    /** 物流公司 */
    logisticsCompany?: string;
    /** 物流单号 */
    trackingNumber?: string;
    /** 发货备注 */
    shipmentNote?: string;
    /** 发货时间 */
    deliverTime?: string;
    /** 签收时间 */
    receiveTime?: string;
    /** 物流状态 */
    status?: string;
    /** 物流状态名称 */
    statusName?: string;
    /** 收货人信息 */
    receiverInfo?: ReceiverInfo;
  }

  export interface ReceiverInfo {
    /** 收货人姓名 */
    receiverName?: string;
    /** 收货人电话 */
    receiverPhone?: string;
    /** 收货详细地址 */
    receiverDetail?: string;
    /** 配送方式 */
    deliveryType?: string;
    /** 配送方式名称 */
    deliveryTypeName?: string;
  }

  export interface OrderReceiveRequest {
    /** 订单ID */
    orderId: string;
    /** 确认备注（可选） */
    remark?: string;
  }

  export interface OrderDeleteRequest {
    /** 订单ID列表 */
    ids: string[];
  }
}

/**
 * 获取订单列表
 * @param params 请求参数
 * @param options
 */
export async function getOrderList(
  params: MallOrderTypes.MallOrderListRequest,
  options?: { [key: string]: any },
) {
  return requestClient.get<TableDataResult<MallOrderTypes.MallOrderListVo>>('/mall/order/list', {
    params,
    ...options,
  });
}

/** 获取订单详情 GET /mall/order/detail/{orderId} */
export async function getOrderDetail(orderId: string, options?: { [key: string]: any }) {
  return requestClient.get(`/mall/order/detail/${orderId}`, options);
}

/** 修改订单地址 PUT /mall/order/address */
export async function updateOrderAddress(
  data: MallOrderTypes.AddressUpdateRequest,
  options?: { [key: string]: any },
) {
  return requestClient.put('/mall/order/address', data, options);
}

/** 修改订单备注 PUT /mall/order/remark */
export async function updateOrderRemark(
  data: MallOrderTypes.RemarkUpdateRequest,
  options?: { [key: string]: any },
) {
  return requestClient.put('/mall/order/remark', data, options);
}

/**
 * 获取订单价格信息
 * @param orderId 订单ID
 */
export async function getOrderPrice(orderId: string | number) {
  return requestClient.get<OrderPriceVo>(`/mall/order/price/${orderId}`);
}

/**
 * 获取订单备注信息
 * @param orderId 订单ID
 */
export async function getOrderRemark(orderId: string | number) {
  return requestClient.get<OrderRemarkVo>(`/mall/order/remark/${orderId}`);
}

/**
 * 获取订单地址信息
 * @param orderId 订单ID
 */
export async function getOrderAddress(orderId: string | number) {
  return requestClient.get<OrderAddressVo>(`/mall/order/address/${orderId}`);
}

/** 订单改价 PUT /mall/order/price */
export async function updateOrderPrice(
  data: MallOrderTypes.OrderUpdatePriceRequest,
  options?: { [key: string]: any },
) {
  return requestClient.put('/mall/order/price', data, options);
}

/** 订单退款 POST /mall/order/refund */
export async function orderRefund(
  data: MallOrderTypes.OrderRefundRequest,
  options?: { [key: string]: any },
) {
  return requestClient.post('/mall/order/refund', data, options);
}

/** 取消订单 POST /mall/order/cancel */
export async function cancelOrder(
  data: MallOrderTypes.OrderCancelRequest,
  options?: { [key: string]: any },
) {
  return requestClient.post('/mall/order/cancel', data, options);
}

export async function getOrderTimeline(orderId: string | number) {
  return requestClient.get(`/mall/order/timeline/${orderId}`);
}

/**
 * 订单发货
 * @param data 订单发货
 */
export async function shipOrder(data: MallOrderTypes.OrderShipRequest) {
  return requestClient.post('/mall/order/ship', data);
}

/**
 * 获取订单物流信息
 * @param orderId 订单ID
 */
export async function getOrderShipping(orderId: string | number) {
  return requestClient.get<MallOrderTypes.OrderShippingVo>(`/mall/order/shipping/${orderId}`);
}

/**
 * 订单确认收货
 * @param data 订单确认收货
 */
export async function manualConfirmReceipt(data: MallOrderTypes.OrderReceiveRequest) {
  return requestClient.post(`/mall/order/confirm-receipt`, data);
}

/**
 * 删除订单 ,只有当订单为完成、订单过期、订单取消状态时才可删除
 * @param data 删除请求参数
 */
export async function deleteOrders(data: MallOrderTypes.OrderDeleteRequest) {
  return requestClient.delete(`/mall/order/${data.ids.join(',')}`);
}
