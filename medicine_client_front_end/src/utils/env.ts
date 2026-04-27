/**
 * 环境配置工具
 */

/** 业务接口同源反向代理前缀。 */
const BASE_URL = '/api'

/**
 * 获取业务接口基础前缀。
 *
 * @returns 业务接口基础前缀
 */
export const getBaseURL = (): string => {
  return BASE_URL
}

/**
 * 获取当前环境。
 *
 * @returns 当前 Vite 运行模式
 */
export const getEnv = (): string => {
  return import.meta.env.MODE
}

/**
 * 判断当前是否为开发环境。
 *
 * @returns 当前是否处于开发模式
 */
export const isDev = (): boolean => {
  return import.meta.env.DEV
}

/**
 * 判断当前是否为生产环境。
 *
 * @returns 当前是否处于生产模式
 */
export const isProd = (): boolean => {
  return import.meta.env.PROD
}
