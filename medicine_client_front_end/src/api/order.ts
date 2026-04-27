import { DeliveryType } from '@/types/orderStatus.ts'
import requestClient from '@/request/requestClient.ts'
import type { TableDataResult } from '@/types/api'

/**
 * 支付方式枚举
 */
export type PayTypeEnum =
  | 'WALLET' // 钱包
  | 'WAIT_PAY' // 待支付
  | 'COUPON' // 优惠券零元支付

export namespace newOrderTypes {
  export interface OrderCouponOptionVo {
    /** 用户优惠券ID */
    couponId?: string
    /** 优惠券名称 */
    couponName?: string
    /** 使用门槛金额 */
    thresholdAmount?: string
    /** 当前可用金额 */
    availableAmount?: string
    /** 是否允许继续使用（1-允许，0-不允许） */
    continueUseEnabled?: number
    /** 优惠券状态 */
    couponStatus?: string
    /** 生效时间 */
    effectiveTime?: string
    /** 失效时间 */
    expireTime?: string
    /** 当前订单是否命中 */
    matched?: boolean
    /** 当前订单不可用原因 */
    unusableReason?: string
    /** 订单抵扣金额 */
    couponDeductAmount?: string
    /** 优惠券消耗金额 */
    couponConsumeAmount?: string
    /** 优惠券浪费金额 */
    couponWasteAmount?: string
  }

  export interface OrderPreviewRequest {
    /** 预览类型: PRODUCT-单个商品, CART-购物车 */
    type: PreviewType
    /** 商品ID(单个商品购买时必填) */
    productId?: string
    /** 购买数量(单个商品购买时必填) */
    quantity?: number
    /** 购物车商品ID列表(购物车结算时必填) */
    cartIds?: string[]
    /** 收货地址ID */
    addressId: string
    /** 用户优惠券ID */
    couponId?: string
    /** 是否明确禁用优惠券 */
    disableCoupon?: boolean
  }

  export enum PreviewType {
    /** 单个商品购买 */
    PRODUCT = 'PRODUCT',
    /**  购物车结算 */
    CART = 'CART'
  }

  export interface OrderPreviewVo {
    /** 商品列表 */
    items?: OrderItemPreview[]
    /** 商品总金额 */
    itemsAmount?: string
    /** 优惠金额 */
    discountAmount?: string
    /** 使用的优惠券ID */
    couponId?: string
    /** 订单抵扣金额 */
    couponDeductAmount?: string
    /** 优惠券消耗金额 */
    couponConsumeAmount?: string
    /** 优惠券浪费金额 */
    couponWasteAmount?: string
    /** 当前选中的优惠券 */
    selectedCoupon?: OrderCouponOptionVo
    /** 当前可选优惠券列表 */
    couponCandidates?: OrderCouponOptionVo[]
    /** 订单总金额 */
    totalAmount?: string
    /** 收货地址 */
    address?: string
    /** 配送方式 */
    deliveryType?: string
    /** 配送方式名称 */
    deliveryTypeName?: string
    /** 预计送达时间 */
    estimatedDeliveryTime?: string
  }

  export interface OrderItemPreview {
    /** 商品ID */
    productId?: string
    /** 商品名称 */
    productName?: string
    /** 商品图片 */
    imageUrl?: string
    /** 商品价格 */
    price?: string
    /** 购买数量 */
    quantity?: number
    /** 小计金额 */
    subtotal?: string
    /** 商品库存 */
    stock?: number
    /** 商品状态 */
    status?: number
    /** 商品状态描述 */
    statusDesc?: string
  }

  export interface OrderCheckoutRequest {
    /** 商品ID */
    productId: string
    /** 购买数量 */
    quantity: number
    /** 收货地址ID */
    addressId: string
    /** 配送方式 */
    deliveryType: DeliveryType
    /** 订单备注 */
    remark?: string
    /** 用户优惠券ID */
    couponId?: string
    /** 是否明确禁用优惠券 */
    disableCoupon?: boolean
  }

  export interface OrderCheckoutVo {
    /** 订单号 */
    orderNo?: string
    /** 订单金额 */
    totalAmount?: string
    /** 商品原始总金额 */
    itemsAmount?: string
    /** 使用的优惠券ID */
    couponId?: string
    /** 优惠券名称 */
    couponName?: string
    /** 订单抵扣金额 */
    couponDeductAmount?: string
    /** 优惠券消耗金额 */
    couponConsumeAmount?: string
    /** 订单状态 */
    orderStatus?: string
    /** 创建时间 */
    createTime?: string
    /** 过期时间 */
    expireTime?: string
    /** 支付过期时间 */
    payExpireTime?: string
    /** 商品摘要 */
    productSummary?: string
    /** 商品种类数量 */
    itemCount?: number
  }

  export interface OrderPayRequest {
    /** 订单编号 */
    orderNo: string
    /** 支付方式 */
    payMethod: PayTypeEnum
  }

  export interface OrderPayVo {
    /** 订单号 */
    orderNo?: string
    /** 支付金额 */
    payAmount?: string
    /** 订单状态 */
    orderStatus?: string
    /** 支付方式 */
    paymentMethod?: string
    /** 支付状态: SUCCESS-支付成功, PENDING-待支付 */
    paymentStatus?: string
    /** 支付数据: 钱包支付固定为null */
    paymentData?: string
  }

  export interface CartSettleRequest {
    /** 购物车商品ID列表 */
    cartIds: string[]
    /** 收货地址ID */
    addressId: string
    /** 配送方式 */
    deliveryType: DeliveryType
    /** 订单备注 */
    remark?: string
    /** 用户优惠券ID */
    couponId?: string
    /** 是否明确禁用优惠券 */
    disableCoupon?: boolean
  }

  export interface OrderReceiveRequest {
    /** 订单编号 */
    orderNo: string
  }

  export interface OrderShippingVo {
    /** 订单ID */
    orderId?: string
    /** 订单号 */
    orderNo?: string
    /** 订单状态 */
    orderStatus?: string
    /** 订单状态名称 */
    orderStatusName?: string
    /** 物流公司 */
    logisticsCompany?: string
    /** 物流单号 */
    trackingNumber?: string
    /** 发货备注 */
    shipmentNote?: string
    /** 发货时间 */
    deliverTime?: string
    /** 签收时间 */
    receiveTime?: string
    /** 物流状态 */
    status?: string
    /** 物流状态名称 */
    statusName?: string
    /** 收货人信息 */
    receiverInfo?: ReceiverInfo
  }

  export interface ReceiverInfo {
    /** 收货人姓名 */
    receiverName?: string
    /** 收货人电话 */
    receiverPhone?: string
    /** 收货详细地址 */
    receiverDetail?: string
    /** 配送方式 */
    deliveryType?: string
    /** 配送方式名称 */
    deliveryTypeName?: string
  }

  export interface OrderListRequest {
    /** 订单状态(PENDING_PAYMENT-待支付, PENDING_SHIPMENT-待发货, PENDING_RECEIPT-待收货, COMPLETED-已完成, CANCELLED-已取消等) */
    orderStatus?: string
    /** 订单编号 */
    orderNo?: string
    /** 商品名称(模糊搜索) */
    productName?: string
    /** 页码 */
    pageNum?: number
    /** 每页大小 */
    pageSize?: number
  }

  export interface OrderListVo {
    /** 订单ID */
    id?: string
    /** 订单编号 */
    orderNo?: string
    /** 订单状态 */
    orderStatus?: string
    /** 订单状态名称 */
    orderStatusName?: string
    /** 订单总金额 */
    totalAmount?: string
    /** 实际支付金额 */
    payAmount?: string
    /** 支付到期时间 */
    payExpireTime?: string
    /** 是否已支付(0-否, 1-是) */
    paid?: number
    /** 是否存在售后 */
    afterSaleFlag?: OrderItemAfterSaleStatusEnum
    /** 创建时间 */
    createTime?: string
    /** 支付时间 */
    payTime?: string
    /** 订单商品列表(只包含必要信息) */
    items?: OrderItemSimpleVo[]
    /** 收货人信息 */
    receiverInfo?: ReceiverInfoSimple
  }

  export interface ReceiverInfoSimple {
    /** 收货人姓名 */
    name?: string
    /** 收货人电话 */
    phone?: string
    /** 收货详细地址 */
    address?: string
  }
  export interface OrderItemSimpleVo {
    /** 订单项ID */
    id?: string
    /** 商品ID */
    productId?: string
    /** 商品名称 */
    productName?: string
    /** 商品图片 */
    imageUrl?: string
    /** 购买数量 */
    quantity?: number
    /** 单价 */
    price?: string
    /** 小计金额 */
    totalPrice?: string
    /** 售后状态 */
    afterSaleStatus?: string
    /** 售后状态名称 */
    afterSaleStatusName?: string
  }

  export enum OrderItemAfterSaleStatusEnum {
    /** 无售后 */
    NONE = 'NONE',
    /** 售后中 */
    IN_PROGRESS = 'IN_PROGRESS',
    /** 售后完成 */
    COMPLETED = 'COMPLETED'
  }

  export interface OrderDetailVo {
    /** 订单ID */
    id?: string
    /** 订单编号 */
    orderNo?: string
    /** 订单状态 */
    orderStatus?: string
    /** 订单状态名称 */
    orderStatusName?: string
    /** 订单总金额 */
    totalAmount?: string
    /** 实际支付金额 */
    payAmount?: string
    /** 商品原始总金额 */
    itemsAmount?: string
    /** 运费金额 */
    freightAmount?: string
    /** 使用的优惠券ID */
    couponId?: string
    /** 优惠券名称 */
    couponName?: string
    /** 订单抵扣金额 */
    couponDeductAmount?: string
    /** 优惠券消耗金额 */
    couponConsumeAmount?: string
    /** 支付方式 */
    payType?: string
    /** 支付方式名称 */
    payTypeName?: string
    /** 配送方式 */
    deliveryType?: string
    /** 配送方式名称 */
    deliveryTypeName?: string
    /** 是否已支付(0-否, 1-是) */
    paid?: number
    /** 支付时间 */
    payTime?: string
    /** 支付到期时间 */
    payExpireTime?: string
    /** 发货时间 */
    deliverTime?: string
    /** 确认收货时间 */
    receiveTime?: string
    /** 完成时间 */
    finishTime?: string
    /** 创建时间 */
    createTime?: string
    /** 用户留言 */
    note?: string
    /** 是否存在售后 */
    afterSaleFlag?: OrderItemAfterSaleStatusEnum
    /** 退款状态 */
    refundStatus?: string
    /** 退款金额 */
    refundPrice?: string
    /** 退款时间 */
    refundTime?: string
    /** 收货人姓名 */
    receiverName?: string
    /** 收货人电话 */
    receiverPhone?: string
    /** 收货详细地址 */
    receiverDetail?: string
    /** 收货人信息 */
    receiverInfo?: OrderDetailVo.ReceiverInfo
    /** 订单商品列表 */
    items?: OrderDetailVo.OrderItemDetailVo[]
    /** 物流信息 */
    shippingInfo?: OrderDetailVo.ShippingInfo
  }
  export namespace OrderDetailVo {
    export interface ReceiverInfo {
      /** 收货人姓名 */
      receiverName?: string
      /** 收货人电话 */
      receiverPhone?: string
      /** 收货详细地址 */
      receiverDetail?: string
    }

    export interface OrderItemDetailVo {
      /** 订单项ID */
      id?: string
      /** 商品ID */
      productId?: string
      /** 商品名称 */
      productName?: string
      /** 商品图片 */
      imageUrl?: string
      /** 购买数量 */
      quantity?: number
      /** 单价 */
      price?: string
      /** 小计金额 */
      totalPrice?: string
      /** 分摊优惠金额 */
      couponDeductAmount?: string
      /** 应付金额 */
      payableAmount?: string
      /** 售后状态 */
      afterSaleStatus?: string
      /** 售后状态名称 */
      afterSaleStatusName?: string
      /** 已退款金额 */
      refundedAmount?: string
    }

    export interface ShippingInfo {
      /** 物流公司 */
      logisticsCompany?: string
      /** 物流单号 */
      trackingNumber?: string
      /** 物流状态 */
      shippingStatus?: string
      /** 物流状态名称 */
      shippingStatusName?: string
      /** 发货时间 */
      shipTime?: string
    }

    export interface OrderCancelRequest {
      /** 订单编号 */
      orderNo: string
      /** 取消原因 */
      cancelReason: string
    }
    export interface OrderPayInfoRequest {
      /** 订单号 */
      orderNo: string
    }
    export interface OrderPayInfoVo {
      /** 订单号 */
      orderNo?: string
      /** 订单金额 */
      totalAmount?: string
      /** 商品原始总金额 */
      itemsAmount?: string
      /** 使用的优惠券ID */
      couponId?: string
      /** 优惠券名称 */
      couponName?: string
      /** 订单抵扣金额 */
      couponDeductAmount?: string
      /** 优惠券消耗金额 */
      couponConsumeAmount?: string
      /** 订单状态 */
      orderStatus?: string
      /** 订单状态名称 */
      orderStatusName?: string
      /** 是否已支付(0-否, 1-是) */
      paid?: number
      /** 支付方式编码 */
      payType?: string
      /** 支付方式名称 */
      payTypeName?: string
      /** 支付时间 */
      payTime?: string
      /** 创建时间 */
      createTime?: string
      /** 过期时间 */
      payExpireTime?: string
      /** 商品摘要 */
      productSummary?: string
      /** 商品种类数量 */
      itemCount?: number
    }
  }

  /**
   * 订单预览
   * @param params 订单预览请求参数
   * @returns 订单预览信息
   */
  export const previewOrder = (params: OrderPreviewRequest) => {
    return requestClient.post<OrderPreviewVo>('/order/preview', params)
  }

  /**
   * 提交订单（创建订单并锁定库存）
   * @param params 订单提交请求参数
   * @returns 订单提交结果
   */
  export const checkoutOrder = (params: OrderCheckoutRequest) => {
    return requestClient.post<OrderCheckoutVo>('/order/checkout', params)
  }

  /**
   * 订单支付
   * @param params 订单支付请求参数
   * @returns 订单支付结果
   */
  export const payOrder = (params: OrderPayRequest) => {
    return requestClient.post<OrderPayVo>('/order/pay', params)
  }

  /**
   * 从购物车提交订单（创建订单并锁定库存）
   * @param params 购物车提交订单请求参数
   * @returns 订单提交结果
   */
  export const createOrderFromCart = (params: CartSettleRequest) => {
    return requestClient.post<OrderCheckoutVo>('/order/create-from-cart', params)
  }

  /**
   * 确认收货
   * @param params 确认收货请求参数
   * @returns 确认结果
   */
  export const confirmReceipt = (params: OrderReceiveRequest) => {
    return requestClient.post<void>('/order/confirm-receipt', params)
  }

  /**
   * 查询订单物流信息
   * @param orderNo 订单编号
   * @returns 物流信息
   */
  export const getOrderShipping = (orderNo: string) => {
    return requestClient.get<OrderShippingVo>(`/order/shipping/${encodeURIComponent(orderNo)}`)
  }

  /**
   * 分页查询用户订单列表
   * @param params 查询条件
   * @returns 订单列表
   */
  export const getOrderList = (params: OrderListRequest) => {
    return requestClient.get<TableDataResult<OrderListVo[]>>('/order/list', { params })
  }

  /**
   * 查询订单详情
   * @param orderNo 订单编号
   * @returns 订单详情
   */
  export const getOrderDetail = (orderNo: string) => {
    return requestClient.get<OrderDetailVo>(`/order/detail/${encodeURIComponent(orderNo)}`)
  }

  /**
   * 取消订单
   * @param params 取消订单请求参数
   * @returns 取消结果
   */
  export const cancelOrder = (params: OrderDetailVo.OrderCancelRequest) => {
    return requestClient.post<void>('/order/cancel', params)
  }

  /**
   * 获取订单支付信息
   * @param orderNo 订单编号
   * @returns 订单支付信息
   */
  export const getOrderPayInfo = (orderNo: string) => {
    return requestClient.get<OrderDetailVo.OrderPayInfoVo>(`/order/pay_info/${encodeURIComponent(orderNo)}`)
  }
}
