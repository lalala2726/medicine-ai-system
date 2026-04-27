import { baseRequestClient, requestClient } from '@/utils/request';

/** 登录接口 POST /api/login */
export async function login(body: API.LoginParams, options?: { [key: string]: any }) {
  return baseRequestClient.post<API.LoginResult>('/auth/login', body, options);
}

/** 获取当前用户信息 GET /api/currentUser */
export async function currentUser(options?: { [key: string]: any }) {
  return requestClient.get<API.CurrentUser>('/auth/currentUser', options);
}

export async function outLogin(options?: { [key: string]: any }) {
  return requestClient.post('/auth/logout', undefined, options);
}

/**
 * 管理端个人资料更新参数。
 */
export interface UpdateAdminProfileParams {
  /** 头像地址。 */
  avatar?: string;
  /** 昵称。 */
  nickname: string;
  /** 真实姓名。 */
  realName?: string;
  /** 邮箱。 */
  email?: string;
}

/**
 * 管理端发送手机号验证码参数。
 */
export interface SendAdminPhoneCodeParams {
  /** 新手机号。 */
  phoneNumber: string;
  /** 滑动验证码校验凭证。 */
  captchaVerificationId: string;
}

/**
 * 管理端修改手机号参数。
 */
export interface ChangeAdminPhoneParams {
  /** 新手机号。 */
  phoneNumber: string;
  /** 手机验证码。 */
  verificationCode: string;
}

export interface RefreshTokenResult {
  accessToken: string;
  refreshToken?: string | null;
}

export async function refreshToken(refreshToken: string, options?: { [key: string]: any }) {
  return baseRequestClient.post<RefreshTokenResult>('/auth/refresh', { refreshToken }, options);
}

/**
 * 更新当前登录管理员资料。
 *
 * @param body 资料更新参数。
 * @param options 请求配置。
 * @returns 更新结果。
 */
export async function updateAdminProfile(
  body: UpdateAdminProfileParams,
  options?: { [key: string]: any },
) {
  return requestClient.put<void, UpdateAdminProfileParams>('/auth/profile', body, options);
}

/**
 * 发送管理端手机号修改验证码。
 *
 * @param body 发送验证码参数。
 * @param options 请求配置。
 * @returns 发送结果。
 */
export async function sendAdminPhoneCode(
  body: SendAdminPhoneCodeParams,
  options?: { [key: string]: any },
) {
  return requestClient.post<void, SendAdminPhoneCodeParams>(
    '/auth/phone/verification-code',
    body,
    options,
  );
}

/**
 * 修改当前登录管理员手机号。
 *
 * @param body 修改手机号参数。
 * @param options 请求配置。
 * @returns 修改结果。
 */
export async function changeAdminPhone(
  body: ChangeAdminPhoneParams,
  options?: { [key: string]: any },
) {
  return requestClient.put<void, ChangeAdminPhoneParams>('/auth/phone', body, options);
}

/** 发送验证码 POST /api/login/captcha */
export async function getFakeCaptcha(
  params: {
    // query
    /** 手机号 */
    phone?: string;
  },
  options?: { [key: string]: any },
) {
  return requestClient.get('/login/captcha', {
    params,
    ...options,
  });
}
