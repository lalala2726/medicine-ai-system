import { baseRequestClient } from '@/utils/request';
import type { RequestOptions } from '@/utils/request';

/**
 * 滑块验证码生成接口地址。
 */
export const CAPTCHA_GENERATE_URL = '/auth/captcha/gen';

/**
 * 滑块验证码校验接口地址。
 */
export const CAPTCHA_CHECK_URL = '/auth/captcha/check';

/**
 * 验证码接口支持的 HTTP 方法。
 */
export type CaptchaRequestMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

/**
 * tianai 验证码接口通用响应结构。
 */
export interface TianaiCaptchaResponse<TData = unknown> {
  /** 业务状态码 */
  code?: number;
  /** 业务提示消息 */
  msg?: string;
  /** 业务数据 */
  data?: TData;
}

/**
 * 验证码请求体结构。
 */
export interface CaptchaRequestPayload<TData = unknown> {
  /** 请求地址 */
  url: string;
  /** 请求方法 */
  method?: CaptchaRequestMethod;
  /** 请求头 */
  headers?: Record<string, string>;
  /** 请求体 */
  data?: TData;
}

/**
 * 登录阶段消费的验证码校验结果。
 */
export interface CaptchaVerificationResult {
  /** 登录阶段消费的验证码校验凭证 */
  id: string;
}

/**
 * 滑块验证码 challenge 数据。
 */
export interface SliderCaptchaChallenge {
  /** challenge ID */
  id: string;
  /** 验证码类型 */
  type: string;
  /** 背景图 base64 内容 */
  backgroundImage: string;
  /** 拼图块 base64 内容 */
  templateImage: string;
  /** 背景图宽度 */
  backgroundImageWidth?: number;
  /** 背景图高度 */
  backgroundImageHeight?: number;
  /** 拼图块宽度 */
  templateImageWidth?: number;
  /** 拼图块高度 */
  templateImageHeight?: number;
  /** challenge 透传扩展数据 */
  data?: unknown;
}

/**
 * 滑块轨迹点。
 */
export interface SliderCaptchaTrackPoint {
  /** 页面 X 坐标 */
  x: number;
  /** 页面 Y 坐标 */
  y: number;
  /** 相对开始拖动的毫秒偏移 */
  t: number;
  /** 轨迹点类型 */
  type: 'down' | 'move' | 'up';
}

/**
 * 滑块验证码轨迹数据。
 */
export interface SliderCaptchaTrackPayload {
  /** 当前渲染背景图宽度 */
  bgImageWidth: number;
  /** 当前渲染背景图高度 */
  bgImageHeight: number;
  /** 当前渲染拼图块宽度 */
  templateImageWidth: number;
  /** 当前渲染拼图块高度 */
  templateImageHeight: number;
  /** 拖动开始时间戳 */
  startTime: number;
  /** 拖动结束时间戳 */
  stopTime: number;
  /** 拼图块最终 X 偏移 */
  left: number;
  /** 拼图块最终 Y 偏移 */
  top: number;
  /** 前端采集的轨迹点列表 */
  trackList: SliderCaptchaTrackPoint[];
  /** challenge 透传扩展数据 */
  data?: unknown;
}

/**
 * 滑块验证码校验请求。
 */
export interface SliderCaptchaCheckPayload {
  /** challenge ID */
  id: string;
  /** 滑块轨迹数据 */
  data: SliderCaptchaTrackPayload;
}

/**
 * 统一发送验证码请求并转成 tianai SDK 兼容响应。
 *
 * @param payload 请求参数
 * @returns tianai SDK 兼容响应
 */
export async function requestCaptcha<TResponseData = unknown, TRequestData = unknown>(
  payload: CaptchaRequestPayload<TRequestData>,
): Promise<TianaiCaptchaResponse<TResponseData>> {
  const response = await baseRequestClient.request<any>(payload.url, {
    method: payload.method ?? 'GET',
    data: payload.data,
    headers: payload.headers,
  } as RequestOptions);

  return {
    code: response.code,
    msg: response.msg ?? response.message,
    data: response.data as TResponseData,
  };
}

/**
 * 获取滑块验证码 challenge。
 *
 * @returns challenge 响应数据
 */
export async function getSliderCaptcha(): Promise<TianaiCaptchaResponse<SliderCaptchaChallenge>> {
  return requestCaptcha<SliderCaptchaChallenge>({
    url: CAPTCHA_GENERATE_URL,
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
  });
}

/**
 * 校验滑块验证码轨迹。
 *
 * @param payload 校验请求体
 * @returns 登录阶段消费的验证码校验结果
 */
export async function checkSliderCaptcha(
  payload: SliderCaptchaCheckPayload,
): Promise<TianaiCaptchaResponse<CaptchaVerificationResult>> {
  return requestCaptcha<CaptchaVerificationResult, SliderCaptchaCheckPayload>({
    url: CAPTCHA_CHECK_URL,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data: payload,
  });
}
