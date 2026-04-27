import { requestClient } from '@/utils/request';
import type { Option, PageRequest, TableDataResult } from '@/types';

export namespace UserTypes {
  export interface UserListVo {
    /** 用户ID */
    id?: number;
    /** 用户名 */
    username?: string;
    /** 昵称 */
    nickname?: string;
    /** 头像URL */
    avatar?: string;
    /** 真实姓名 */
    realName?: string;
    /** 手机号 */
    phoneNumber?: string;
    /** 邮箱 */
    email?: string;
    /** 角色 */
    roles?: string;
    /** 状态 */
    status?: number;
    /** 创建时间 */
    createTime?: string;
  }

  export interface UserDetailVo {
    /** 头像 */
    avatar?: string;
    /** 昵称 */
    nickName?: string;
    /** 钱包余额 */
    walletBalance?: string;
    /** 总订单数 */
    totalOrders?: number;
    /** 总消费金额 */
    totalConsume?: string;
    /** 角色ID集合 */
    roles?: number[];
    /** 基础信息 */
    basicInfo?: UserDetailVo.BasicInfo;
    /** 安全信息 */
    securityInfo?: UserDetailVo.SecurityInfo;
  }

  export namespace UserDetailVo {
    export interface BasicInfo {
      /** 用户ID */
      userId?: string;
      /** 用户名 */
      realName?: string;
      /** 手机号 */
      phoneNumber?: string;
      /** 邮箱 */
      email?: string;
      /** 性别 */
      gender?: number;
      /** 身份证号 */
      idCard?: string;
    }

    export interface SecurityInfo {
      /** 注册时间 */
      registerTime?: string;
      /** 最后登录时间 */
      lastLoginTime?: string;
      /** 最后登录IP */
      lastLoginIp?: string;
      /** 用户状态 */
      status?: number;
    }
  }
  export interface UserWalletFlowInfoVo {
    /** 流水索引 */
    index?: string;
    /** 变动类型 */
    changeType?: string;
    /** 金额变动方向：1收入(正)、2支出(负)、3冻结、4解冻  */
    amountDirection: number;
    /** 是否收入 */
    isIncome: boolean;
    /** 变动金额 */
    amount?: string;
    /** 变动前余额 */
    beforeBalance?: string;
    /** 变动后余额 */
    afterBalance?: string;
    /** 变动时间 */
    changeTime?: string;
  }

  export interface UserConsumeInfo {
    /** 索引 */
    index?: string;
    /** 用户ID */
    userId?: string;
    /** 订单编号 */
    orderNo?: string;
    /** 商品总价 */
    totalPrice?: string;
    /** 实付金额 */
    payPrice?: string;
    /** 完成时间 */
    finishTime?: string;
  }

  export interface UserCouponListRequest extends PageRequest {
    /** 优惠券状态 */
    couponStatus?: string;
    /** 优惠券名称 */
    couponName?: string;
  }

  export interface UserCouponVo {
    /** 用户优惠券ID */
    couponId?: number;
    /** 优惠券模板ID */
    templateId?: number;
    /** 用户ID */
    userId?: number;
    /** 用户昵称 */
    userNickname?: string;
    /** 用户手机号 */
    userPhoneNumber?: string;
    /** 优惠券名称 */
    couponName?: string;
    /** 使用门槛金额 */
    thresholdAmount?: string;
    /** 优惠券总金额 */
    totalAmount?: string;
    /** 当前可用金额 */
    availableAmount?: string;
    /** 当前锁定消耗金额 */
    lockedConsumeAmount?: string;
    /** 优惠券状态 */
    couponStatus?: string;
    /** 是否允许继续使用 */
    continueUseEnabled?: number;
    /** 生效时间 */
    effectiveTime?: string;
    /** 失效时间 */
    expireTime?: string;
    /** 锁定订单号 */
    lockOrderNo?: string;
    /** 锁定时间 */
    lockTime?: string;
    /** 来源类型 */
    sourceType?: string;
    /** 创建时间 */
    createTime?: string;
  }

  export interface UserCouponLogListRequest extends PageRequest {
    /** 用户优惠券ID */
    couponId?: number;
    /** 订单号 */
    orderNo?: string;
    /** 变更类型 */
    changeType?: string;
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
    /** 创建时间 */
    createTime?: string;
  }

  export interface UserActivationLogListRequest extends PageRequest {
    /** 批次ID */
    batchId?: number;
    /** 激活码ID */
    activationCodeId?: number;
    /** 批次号 */
    batchNo?: string;
    /** 激活码关键字 */
    plainCode?: string;
    /** 结果状态 */
    resultStatus?: string;
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
    redeemRuleType?: string;
    /** 激活码列表展示值 */
    plainCodeSnapshot?: string;
    /** 结果状态 */
    resultStatus?: string;
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

  export interface UserAddRequest {
    /** 用户名 */
    username?: string;
    /** 昵称 */
    nickname?: string;
    /** 头像URL */
    avatar?: string;
    /** 密码 */
    password?: string;
    /** 角色ID集合 */
    roles?: number[];
    /** 状态 */
    status?: number;
    /** 手机号 */
    phoneNumber?: string;
    /** 邮箱 */
    email?: string;
    /** 身份证号 */
    idCard?: string;
    /** 真实姓名 */
    realName?: string;
  }

  export interface UserListQueryRequest extends PageRequest {
    /** 用户ID */
    id?: number;
    /** 用户名 */
    username?: string;
    /** 昵称 */
    nickname?: string;
    /** 头像URL */
    avatar?: string;
    /** 角色ID */
    roleId?: number;
    /** 身份证号 */
    idCard?: string;
    /** 手机号 */
    phoneNumber?: string;
    /** 真实姓名 */
    realName?: string;
    /** 邮箱 */
    email?: string;
    /** 状态 */
    status?: number;
    /** 创建人 */
    createBy?: string;
  }

  export interface UserUpdateRequest {
    /** 用户ID */
    id?: string | number;
    /** 用户名 */
    username?: string;
    /** 昵称 */
    nickname?: string;
    /** 头像URL */
    avatar?: string;
    /** 密码 */
    password?: string;
    /** 角色ID集合 */
    roles?: number[];
    /** 状态 */
    status?: number;
    /** 真实姓名 */
    realName?: string;
    /** 手机号 */
    phoneNumber?: string;
    /** 邮箱 */
    email?: string;
    /** 性别 */
    gender?: number;
    /** 身份证号 */
    idCard?: string;
  }

  export interface FreezeOrUnUserWalletRequest {
    /** 用户ID */
    userId: string;
    /** 冻结或解冻原因 */
    reason: string;
    /** 滑动验证码校验凭证 */
    captchaVerificationId: string;
  }

  export interface OpenUserWalletRequest {
    /** 用户ID */
    userId: string;
    /** 滑动验证码校验凭证 */
    captchaVerificationId: string;
  }

  export interface WalletChangeRequest {
    /** 用户ID */
    userId: string;
    /** 金额 */
    amount: string;
    /** 操作类型 (1-充值, 2-扣款) */
    operationType: number;
    /** 修改原因 */
    reason: string;
    /** 滑动验证码校验凭证 */
    captchaVerificationId: string;
  }

  export interface UserWalletVo {
    /** 用户ID */
    userId?: string;
    /** 钱包编号，唯一标识一个用户的钱包 */
    walletNo?: string;
    /** 可用余额 */
    balance?: string;
    /** 累计入账金额（充值、退款等） */
    totalIncome?: string;
    /** 累计支出金额（消费、提现等） */
    totalExpend?: string;
    /** 币种，默认人民币 */
    currency?: string;
    /** 状态：0正常，1冻结 */
    status?: number;
    /** 冻结原因 */
    freezeReason?: string;
    /** 冻结时间 */
    freezeTime?: string;
    /** 更新时间 */
    updatedAt?: string;
  }
}

export async function userList(params?: UserTypes.UserListQueryRequest) {
  return requestClient.get<TableDataResult<UserTypes.UserListVo>>('/system/user/list', {
    params,
  });
}

/**
 * 获取用户钱包流水
 * @param id 用户ID
 * @param params 查询参数
 */
export async function getUserWalletFlow(id: number | string, params: PageRequest) {
  return requestClient.get<TableDataResult<UserTypes.UserWalletFlowInfoVo>>(
    `/system/user/${id}/wallet-flow`,
    {
      params,
    },
  );
}

/**
 * 获取用户消费信息
 * @param id 用户ID
 * @param params 查询参数
 */
export async function getConsumeInfo(id: number | string, params: PageRequest) {
  return requestClient.get<TableDataResult<UserTypes.UserConsumeInfo>>(
    `/system/user/${id}/consume-info`,
    {
      params,
    },
  );
}

/**
 * 获取指定用户优惠券列表
 * @param id 用户ID
 * @param params 查询参数
 */
export async function getUserCoupons(id: number | string, params: UserTypes.UserCouponListRequest) {
  return requestClient.get<TableDataResult<UserTypes.UserCouponVo>>(`/system/user/${id}/coupons`, {
    params,
  });
}

/**
 * 获取指定用户优惠券日志列表
 * @param id 用户ID
 * @param params 查询参数
 */
export async function getUserCouponLogs(
  id: number | string,
  params: UserTypes.UserCouponLogListRequest,
) {
  return requestClient.get<TableDataResult<UserTypes.CouponLogVo>>(
    `/system/user/${id}/coupon-logs`,
    {
      params,
    },
  );
}

/**
 * 获取指定用户激活码日志列表
 * @param id 用户ID
 * @param params 查询参数
 */
export async function getUserActivationLogs(
  id: number | string,
  params: UserTypes.UserActivationLogListRequest,
) {
  return requestClient.get<TableDataResult<UserTypes.ActivationLogVo>>(
    `/system/user/${id}/activation-logs`,
    {
      params,
    },
  );
}

/**
 * 删除指定用户优惠券
 * @param userId 用户ID
 * @param couponId 用户优惠券ID
 */
export async function deleteUserCoupon(userId: number | string, couponId: number | string) {
  return requestClient.delete<void>(`/system/user/${userId}/coupons/${couponId}`);
}

/**
 * 获取用户钱包的余额
 * @param id 用户ID
 */
export async function getUserWallet(id: number | string) {
  return requestClient.get<UserTypes.UserWalletVo>(`/system/user/${id}/wallet`);
}

/**
 * 开通用户钱包
 * @param data 请求参数
 */
export async function openUserWallet(data: UserTypes.OpenUserWalletRequest) {
  return requestClient.post('/system/user/wallet/open', data);
}

/**
 * 冻结用户钱包
 * @param data 请求参数
 */
export async function freezeUserWallet(data: UserTypes.FreezeOrUnUserWalletRequest) {
  return requestClient.post('/system/user/wallet/freeze', data);
}

/**
 * 解冻用户钱包
 * @param data 请求参数
 */
export async function unfreezeUserWallet(data: UserTypes.FreezeOrUnUserWalletRequest) {
  return requestClient.post('/system/user/wallet/unfreeze', data);
}

/**
 * 修改用户钱包
 * @param data 请求参数
 */
export async function changeUserWallet(data: UserTypes.WalletChangeRequest) {
  return requestClient.post('/system/user/wallet/change', data);
}

/**
 * 添加用户
 * @param data 添加用户参数
 */
export async function addUser(data: UserTypes.UserAddRequest) {
  return requestClient.post('/system/user', data);
}

/**
 * 获取用户详情
 * @param id 用户ID
 */
export async function getUserDetail(id: number | string) {
  return requestClient.get<UserTypes.UserDetailVo>(`/system/user/${id}/detail`);
}

/**
 * 修改用户
 * @param data 修改用户参数
 */
export async function updateUser(data: UserTypes.UserUpdateRequest) {
  return requestClient.put('/system/user', data);
}

/**
 * 删除用户
 * @param ids 用户ID
 * @param options 请求配置项
 */
export async function deleteUser(ids: number[], options?: { [key: string]: any }) {
  return requestClient.delete(`/system/user/${ids.join(',')}`, options);
}

/**
 * 批量获取用户ID与用户名映射
 * @param userIds 用户ID列表
 */
export async function listUserOptions(userIds: number[]) {
  return requestClient.post<Option<number>[]>('/system/user/options', userIds);
}
