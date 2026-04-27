import type { Dayjs } from 'dayjs';
import type { MallCouponTypes } from '@/api/mall/coupon';

/**
 * 激活码生成表单值。
 */
export interface ActivationCodeGenerateFormValues {
  /** 优惠券模板ID */
  templateId?: number;
  /** 兑换规则 */
  redeemRuleType: MallCouponTypes.ActivationRedeemRuleType;
  /** 生成数量 */
  generateCount: number;
  /** 有效期类型 */
  validityType: MallCouponTypes.ActivationCodeValidityType;
  /** 固定时间范围 */
  fixedRange?: [Dayjs, Dayjs];
  /** 激活后有效天数 */
  relativeValidDays?: number;
  /** 备注 */
  remark?: string;
}

/**
 * 将 Dayjs 转换为后端接收的日期时间字符串。
 * @param value 日期对象。
 * @returns 标准日期时间字符串。
 */
function toDateTimeString(value: Dayjs): string {
  return value.format('YYYY-MM-DDTHH:mm:ss.SSSZ');
}

/**
 * 构建激活码生成请求参数。
 * @param values 表单值。
 * @param captchaVerificationId 验证码校验凭证。
 * @returns 激活码生成请求参数。
 */
export function buildActivationCodeGeneratePayload(
  values: ActivationCodeGenerateFormValues,
  captchaVerificationId = '',
): MallCouponTypes.ActivationCodeGenerateRequest {
  const fixedRange = values.fixedRange;
  return {
    templateId: Number(values.templateId),
    redeemRuleType: values.redeemRuleType,
    generateCount:
      values.redeemRuleType === 'SHARED_PER_USER_ONCE' ? 1 : Number(values.generateCount || 1),
    validityType: values.validityType,
    fixedEffectiveTime:
      values.validityType === 'ONCE' && fixedRange?.[0]
        ? toDateTimeString(fixedRange[0])
        : undefined,
    fixedExpireTime:
      values.validityType === 'ONCE' && fixedRange?.[1]
        ? toDateTimeString(fixedRange[1])
        : undefined,
    relativeValidDays:
      values.validityType === 'AFTER_ACTIVATION'
        ? Number(values.relativeValidDays || 0)
        : undefined,
    remark: values.remark?.trim() || undefined,
    captchaVerificationId,
  };
}
