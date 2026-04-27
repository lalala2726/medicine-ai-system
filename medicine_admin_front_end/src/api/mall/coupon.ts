import { requestClient } from '@/utils/request';
import type { TableDataResult } from '@/types';

/**
 * 激活码导出接口在业务异常场景下返回的 Blob 错误标识。
 */
const ACTIVATION_CODE_EXPORT_BLOB_ERROR = 'ACTIVATION_CODE_EXPORT_BLOB_ERROR';

export namespace MallCouponTypes {
  export type CouponIssueTargetType = 'ALL' | 'SPECIFIED';
  export type CouponTemplateDeleteMode = 'HIDE_ONLY' | 'HIDE_AND_EXPIRE_ISSUED';
  export type ActivationRedeemRuleType = 'SHARED_PER_USER_ONCE' | 'UNIQUE_SINGLE_USE';
  export type ActivationCodeValidityType = 'ONCE' | 'AFTER_ACTIVATION';
  export type ActivationCodeStatus = 'ACTIVE' | 'DISABLED';
  export type ActivationCodeItemStatus = 'ACTIVE' | 'DISABLED' | 'USED';
  export type ActivationLogActionType = 'CREATE' | 'REDEEM';
  export type ActivationLogResultStatus = 'SUCCESS' | 'FAIL';

  export interface CouponTemplateListRequest {
    /** 优惠券名称 */
    name?: string;
    /** 模板状态 */
    status?: string;
    /** 当前页码 */
    pageNum?: number;
    /** 每页大小 */
    pageSize?: number;
  }

  export interface CouponTemplateSaveRequest {
    /** 优惠券名称 */
    name: string;
    /** 使用门槛金额 */
    thresholdAmount: string;
    /** 优惠券面额 */
    faceAmount: string;
    /** 是否允许继续使用（1-允许，0-不允许） */
    continueUseEnabled: number;
    /** 是否允许叠加（1-允许，0-不允许） */
    stackableEnabled: number;
    /** 模板状态 */
    status: string;
    /** 模板备注 */
    remark?: string;
  }

  export interface CouponTemplateUpdateRequest extends CouponTemplateSaveRequest {
    /** 模板ID */
    id: number;
  }

  export interface CouponTemplateVo {
    /** 模板ID */
    id?: number;
    /** 优惠券类型 */
    couponType?: string;
    /** 优惠券名称 */
    name?: string;
    /** 使用门槛金额 */
    thresholdAmount?: string;
    /** 优惠券面额 */
    faceAmount?: string;
    /** 是否允许继续使用（1-允许，0-不允许） */
    continueUseEnabled?: number;
    /** 是否允许叠加（1-允许，0-不允许） */
    stackableEnabled?: number;
    /** 模板状态 */
    status?: string;
    /** 模板备注 */
    remark?: string;
    /** 创建时间 */
    createTime?: string;
  }

  export interface CouponIssueRequest {
    /** 模板ID */
    templateId: number;
    /** 发券目标类型 */
    issueTargetType: CouponIssueTargetType;
    /** 指定用户ID列表 */
    userIds?: number[];
    /** 生效时间 */
    effectiveTime: string;
    /** 失效时间 */
    expireTime: string;
    /** 发券备注 */
    remark?: string;
    /** 验证码校验凭证 */
    captchaVerificationId: string;
  }

  export interface CouponLogListRequest {
    /** 用户优惠券ID */
    couponId?: number;
    /** 用户ID */
    userId?: number;
    /** 订单号 */
    orderNo?: string;
    /** 变更类型 */
    changeType?: string;
    /** 当前页码 */
    pageNum?: number;
    /** 每页大小 */
    pageSize?: number;
  }

  export interface CouponLogVo {
    /** 日志ID */
    id?: number;
    /** 用户优惠券ID */
    couponId?: number;
    /** 优惠券名称 */
    couponName?: string;
    /** 用户ID */
    userId?: number;
    /** 用户名 */
    userName?: string;
    /** 订单号 */
    orderNo?: string;
    /** 变更类型 */
    changeType?: string;
    /** 变更金额 */
    changeAmount?: string;
    /** 抵扣金额 */
    deductAmount?: string;
    /** 浪费金额 */
    wasteAmount?: string;
    /** 变更前可用金额 */
    beforeAvailableAmount?: string;
    /** 变更后可用金额 */
    afterAvailableAmount?: string;
    /** 来源类型 */
    sourceType?: string;
    /** 来源业务号 */
    sourceBizNo?: string;
    /** 备注 */
    remark?: string;
    /** 操作人标识 */
    operatorId?: string;
    /** 操作人用户名 */
    operatorName?: string;
    /** 创建时间 */
    createTime?: string;
  }

  export interface ActivationCodeGenerateRequest {
    /** 优惠券模板ID */
    templateId: number;
    /** 兑换规则类型 */
    redeemRuleType: ActivationRedeemRuleType;
    /** 生成数量 */
    generateCount: number;
    /** 有效期类型 */
    validityType: ActivationCodeValidityType;
    /** 固定生效时间 */
    fixedEffectiveTime?: string;
    /** 固定失效时间 */
    fixedExpireTime?: string;
    /** 激活后有效天数 */
    relativeValidDays?: number;
    /** 备注 */
    remark?: string;
    /** 验证码校验凭证 */
    captchaVerificationId: string;
  }

  export interface ActivationCodeGeneratedItemVo {
    /** 激活码ID */
    id?: number;
    /** 激活码明文 */
    plainCode?: string;
    /** 激活码状态 */
    status?: ActivationCodeItemStatus;
    /** 成功使用次数 */
    successUseCount?: number;
    /** 创建时间 */
    createTime?: string;
    /** 最近一次成功激活时间 */
    lastSuccessTime?: string;
    /** 最近一次成功激活客户端IP */
    lastSuccessClientIp?: string;
    /** 最近一次成功激活用户ID */
    lastSuccessUserId?: number;
    /** 最近一次成功激活用户名 */
    lastSuccessUserName?: string;
  }

  export interface ActivationCodeItemListRequest {
    /** 当前页码 */
    pageNum?: number;
    /** 每页大小 */
    pageSize?: number;
  }

  export interface ActivationCodeGenerateResultVo {
    /** 批次号 */
    batchNo?: string;
    /** 优惠券模板ID */
    templateId?: number;
    /** 优惠券模板名称 */
    templateName?: string;
    /** 兑换规则类型 */
    redeemRuleType?: ActivationRedeemRuleType;
    /** 有效期类型 */
    validityType?: ActivationCodeValidityType;
    /** 本次生成数量 */
    generatedCount?: number;
    /** 激活码列表 */
    codes?: ActivationCodeGeneratedItemVo[];
  }

  export interface ActivationCodeListRequest {
    /** 批次号 */
    batchNo?: string;
    /** 优惠券模板ID */
    templateId?: number;
    /** 优惠券模板名称 */
    templateName?: string;
    /** 兑换规则类型 */
    redeemRuleType?: ActivationRedeemRuleType;
    /** 有效期类型 */
    validityType?: ActivationCodeValidityType;
    /** 激活码状态 */
    status?: ActivationCodeStatus;
    /** 创建开始时间 */
    startTime?: string;
    /** 创建结束时间 */
    endTime?: string;
    /** 当前页码 */
    pageNum?: number;
    /** 每页大小 */
    pageSize?: number;
  }

  export interface ActivationCodeVo {
    /** 激活码批次ID */
    id?: number;
    /** 批次号 */
    batchNo?: string;
    /** 优惠券模板ID */
    templateId?: number;
    /** 优惠券模板名称 */
    templateName?: string;
    /** 兑换规则类型 */
    redeemRuleType?: ActivationRedeemRuleType;
    /** 有效期类型 */
    validityType?: ActivationCodeValidityType;
    /** 固定生效时间 */
    fixedEffectiveTime?: string;
    /** 固定失效时间 */
    fixedExpireTime?: string;
    /** 激活后有效天数 */
    relativeValidDays?: number;
    /** 激活码状态 */
    status?: ActivationCodeStatus;
    /** 生成数量 */
    generateCount?: number;
    /** 成功使用次数 */
    successUseCount?: number;
    /** 备注 */
    remark?: string;
    /** 创建时间 */
    createTime?: string;
    /** 创建人 */
    createBy?: string;
  }

  export interface ActivationCodeDetailVo {
    /** 激活码批次ID */
    id?: number;
    /** 批次号 */
    batchNo?: string;
    /** 优惠券模板ID */
    templateId?: number;
    /** 优惠券模板名称 */
    templateName?: string;
    /** 兑换规则类型 */
    redeemRuleType?: ActivationRedeemRuleType;
    /** 有效期类型 */
    validityType?: ActivationCodeValidityType;
    /** 固定生效时间 */
    fixedEffectiveTime?: string;
    /** 固定失效时间 */
    fixedExpireTime?: string;
    /** 激活后有效天数 */
    relativeValidDays?: number;
    /** 激活码状态 */
    status?: ActivationCodeStatus;
    /** 生成数量 */
    generateCount?: number;
    /** 成功使用次数 */
    successUseCount?: number;
    /** 备注 */
    remark?: string;
    /** 创建时间 */
    createTime?: string;
    /** 创建人 */
    createBy?: string;
  }

  export interface ActivationCodeStatusUpdateRequest {
    /** 激活码批次ID */
    id: number;
    /** 激活码批次状态 */
    status: ActivationCodeStatus;
  }

  export interface ActivationCodeItemStatusUpdateRequest {
    /** 激活码ID */
    id: number;
    /** 激活码单码状态 */
    status: ActivationCodeItemStatus;
  }

  export interface ActivationLogListRequest {
    /** 批次ID */
    batchId?: number;
    /** 激活码ID */
    activationCodeId?: number;
    /** 批次号 */
    batchNo?: string;
    /** 激活码关键字 */
    plainCode?: string;
    /** 用户ID */
    userId?: number;
    /** 结果状态 */
    resultStatus?: ActivationLogResultStatus;
    /** 创建开始时间 */
    startTime?: string;
    /** 创建结束时间 */
    endTime?: string;
    /** 当前页码 */
    pageNum?: number;
    /** 每页大小 */
    pageSize?: number;
  }

  export interface ActivationLogVo {
    /** 日志ID */
    id?: number;
    /** 兑换请求ID */
    requestId?: string;
    /** 批次ID */
    batchId?: number;
    /** 激活码ID */
    activationCodeId?: number;
    /** 批次号 */
    batchNo?: string;
    /** 优惠券模板ID */
    templateId?: number;
    /** 优惠券模板名称 */
    templateName?: string;
    /** 兑换规则类型 */
    redeemRuleType?: ActivationRedeemRuleType;
    /** 激活码列表展示值 */
    plainCodeSnapshot?: string;
    /** 结果状态 */
    resultStatus?: ActivationLogResultStatus;
    /** 用户ID */
    userId?: number;
    /** 用户名 */
    userName?: string;
    /** 用户优惠券ID */
    couponId?: number;
    /** 失败编码 */
    failCode?: string;
    /** 失败信息 */
    failMessage?: string;
    /** 客户端IP */
    clientIp?: string;
    /** 发券方式 */
    grantMode?: string;
    /** 发券状态 */
    grantStatus?: string;
    /** 创建时间 */
    createTime?: string;
  }
}

/**
 * 查询优惠券模板列表。
 * @param params 查询参数。
 * @returns 模板分页数据。
 */
export async function listCouponTemplate(params: MallCouponTypes.CouponTemplateListRequest) {
  return requestClient.get<TableDataResult<MallCouponTypes.CouponTemplateVo>>(
    '/mall/coupon/template/list',
    {
      params,
    },
  );
}

/**
 * 查询优惠券模板详情。
 * @param id 模板ID。
 * @returns 模板详情。
 */
export async function getCouponTemplateById(id: number) {
  return requestClient.get<MallCouponTypes.CouponTemplateVo>(`/mall/coupon/template/${id}`);
}

/**
 * 新增优惠券模板。
 * @param data 模板新增参数。
 * @returns 新增结果。
 */
export async function addCouponTemplate(data: MallCouponTypes.CouponTemplateSaveRequest) {
  return requestClient.post<void>('/mall/coupon/template', data);
}

/**
 * 修改优惠券模板。
 * @param data 模板修改参数。
 * @returns 修改结果。
 */
export async function updateCouponTemplate(data: MallCouponTypes.CouponTemplateUpdateRequest) {
  return requestClient.put<void>('/mall/coupon/template', data);
}

/**
 * 删除优惠券模板。
 * @param id 模板ID。
 * @param deleteMode 删除模式。
 * @returns 删除结果。
 */
export async function deleteCouponTemplateById(
  id: number,
  deleteMode: MallCouponTypes.CouponTemplateDeleteMode,
) {
  return requestClient.delete<void>(`/mall/coupon/template/${id}`, {
    params: {
      deleteMode,
    },
  });
}

/**
 * 管理端发券。
 * @param data 发券参数。
 * @returns 发券结果。
 */
export async function issueCouponToUser(data: MallCouponTypes.CouponIssueRequest) {
  return requestClient.post<void>('/mall/coupon/issue', data);
}

/**
 * 查询优惠券日志列表。
 * @param params 查询参数。
 * @returns 优惠券日志分页数据。
 */
export async function listCouponLog(params: MallCouponTypes.CouponLogListRequest) {
  return requestClient.get<TableDataResult<MallCouponTypes.CouponLogVo>>('/mall/coupon/log/list', {
    params,
  });
}

/**
 * 生成激活码。
 * @param data 生成参数。
 * @returns 生成结果。
 */
export async function generateActivationCodes(data: MallCouponTypes.ActivationCodeGenerateRequest) {
  return requestClient.post<MallCouponTypes.ActivationCodeGenerateResultVo>(
    '/mall/coupon/activation-batch/generate',
    data,
  );
}

/**
 * 查询激活码列表。
 * @param params 查询参数。
 * @returns 激活码分页数据。
 */
export async function listActivationCodes(params: MallCouponTypes.ActivationCodeListRequest) {
  return requestClient.get<TableDataResult<MallCouponTypes.ActivationCodeVo>>(
    '/mall/coupon/activation-batch/list',
    {
      params,
    },
  );
}

/**
 * 查询激活码详情。
 * @param id 激活码ID。
 * @returns 激活码详情。
 */
export async function getActivationCodeById(id: number) {
  return requestClient.get<MallCouponTypes.ActivationCodeDetailVo>(
    `/mall/coupon/activation-batch/${id}`,
  );
}

/**
 * 查询批次下的全部激活码。
 * @param id 批次ID。
 * @param params 查询参数。
 * @returns 激活码明细列表。
 */
export async function listActivationBatchCodes(
  id: number,
  params: MallCouponTypes.ActivationCodeItemListRequest,
) {
  return requestClient.get<TableDataResult<MallCouponTypes.ActivationCodeGeneratedItemVo>>(
    `/mall/coupon/activation-batch/${id}/codes`,
    {
      params,
    },
  );
}

/**
 * 下载批次下的全部激活码 Excel。
 * @param id 批次ID。
 * @returns Excel 二进制数据。
 */
export async function downloadActivationBatchCodesExcel(id: number): Promise<Blob> {
  const blob = await requestClient.get<Blob>(`/mall/coupon/activation-batch/${id}/codes/export`, {
    responseType: 'blob',
  });
  if (blob.type.includes('application/json')) {
    throw new Error(ACTIVATION_CODE_EXPORT_BLOB_ERROR);
  }
  return blob;
}

/**
 * 更新激活码状态。
 * @param data 状态更新参数。
 * @returns 更新结果。
 */
export async function updateActivationCodeStatus(
  data: MallCouponTypes.ActivationCodeStatusUpdateRequest,
) {
  return requestClient.put<void>('/mall/coupon/activation-batch/status', data);
}

/**
 * 删除激活码批次。
 * @param id 激活码批次ID。
 * @returns 删除结果。
 */
export async function deleteActivationBatchById(id: number) {
  return requestClient.delete<void>(`/mall/coupon/activation-batch/${id}`);
}

/**
 * 更新激活码单码状态。
 * @param data 状态更新参数。
 * @returns 更新结果。
 */
export async function updateActivationCodeItemStatus(
  data: MallCouponTypes.ActivationCodeItemStatusUpdateRequest,
) {
  return requestClient.put<void>('/mall/coupon/activation-code/status', data);
}

/**
 * 删除激活码单码。
 * @param id 激活码ID。
 * @returns 删除结果。
 */
export async function deleteActivationCodeItemById(id: number) {
  return requestClient.delete<void>(`/mall/coupon/activation-code/${id}`);
}

/**
 * 查询激活码兑换日志列表。
 * @param params 查询参数。
 * @returns 激活码兑换日志分页数据。
 */
export async function listActivationLogs(params: MallCouponTypes.ActivationLogListRequest) {
  return requestClient.get<TableDataResult<MallCouponTypes.ActivationLogVo>>(
    '/mall/coupon/activation-log/redeem/list',
    {
      params,
    },
  );
}
