/**
 * 支付方式编码类型。
 */
export type PayTypeCode = 'WALLET' | 'WAIT_PAY' | 'COUPON'

/**
 * 支付方式中文文案映射。
 */
export const PAY_TYPE_TEXT_MAP: Record<PayTypeCode, string> = {
  WALLET: '钱包支付',
  WAIT_PAY: '待支付',
  COUPON: '优惠券抵扣(0元支付)'
}

/**
 * 获取支付方式展示文案。
 * @param payType 支付方式编码。
 * @returns string 支付方式中文文案。
 */
export const getPayTypeText = (payType?: string): string => {
  if (!payType) {
    return '-'
  }
  return PAY_TYPE_TEXT_MAP[payType as PayTypeCode] || payType
}
