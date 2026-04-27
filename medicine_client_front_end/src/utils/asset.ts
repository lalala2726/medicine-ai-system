import { getBaseURL } from '@/utils/env'

/**
 * 绝对地址匹配规则。
 */
const ABSOLUTE_ASSET_URL_PATTERN = /^(https?:)?\/\//i

/**
 * 特殊协议地址前缀列表。
 */
const SPECIAL_ASSET_PROTOCOL_PREFIXES = ['data:', 'blob:']

/**
 * 将资源地址补全为当前前端可访问的地址。
 *
 * @param assetUrl 原始资源地址
 * @returns 补全后的资源地址
 */
export const resolveAssetUrl = (assetUrl?: string): string | undefined => {
  if (!assetUrl) {
    return assetUrl
  }

  if (
    ABSOLUTE_ASSET_URL_PATTERN.test(assetUrl) ||
    SPECIAL_ASSET_PROTOCOL_PREFIXES.some(prefix => assetUrl.startsWith(prefix))
  ) {
    return assetUrl
  }

  const baseUrl = getBaseURL()

  if (assetUrl.startsWith(baseUrl)) {
    return assetUrl
  }

  return assetUrl.startsWith('/') ? `${baseUrl}${assetUrl}` : `${baseUrl}/${assetUrl}`
}
