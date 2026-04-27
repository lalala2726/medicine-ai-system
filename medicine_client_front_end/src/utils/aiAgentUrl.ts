/** AI Agent URL 查询参数允许的值类型。 */
type AiAgentUrlQueryValue = string | number | boolean | null | undefined

/** AI Agent URL 查询参数集合。 */
export type AiAgentUrlQuery = Record<string, AiAgentUrlQueryValue>

/** AI Agent 同源反向代理前缀。 */
const AI_AGENT_BASE_PATH = '/ai_agent'

/**
 * 拼接 AI Agent 基础地址与目标路径。
 *
 * @param path - 目标接口路径
 * @returns 拼接后的原始地址字符串
 */
const joinAiAgentPath = (path: string): string => {
  /** 规范化后的目标路径。 */
  const normalizedPath = path.startsWith('/') ? path : `/${path}`

  return `${AI_AGENT_BASE_PATH}${normalizedPath}`
}

/**
 * 创建 AI Agent URL 对象，并附带查询参数。
 * 浏览器端统一解析为当前站点下的 `/ai_agent/...` 地址。
 *
 * @param path - 目标接口路径
 * @param query - 需要附加的查询参数
 * @returns 标准化后的 URL 对象
 */
const createAiAgentUrl = (path: string, query: AiAgentUrlQuery = {}): URL => {
  /** 拼接后的原始地址字符串。 */
  const joinedUrl = joinAiAgentPath(path)
  /** URL 解析时使用的基准 origin。 */
  const baseOrigin = typeof window === 'undefined' ? 'http://localhost' : window.location.origin
  /** 标准化后的 URL 对象。 */
  const url = new URL(joinedUrl, baseOrigin)

  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null) {
      return
    }

    url.searchParams.set(key, String(value))
  })

  return url
}

/**
 * 解析 AI Agent HTTP 请求地址。
 *
 * @param path - 目标接口路径
 * @param query - 需要附加的查询参数
 * @returns 可直接用于 HTTP / SSE / TTS 请求的地址
 */
export const resolveAiAgentHttpUrl = (path: string, query: AiAgentUrlQuery = {}): string => {
  /** 拼接后的原始地址字符串。 */
  const joinedUrl = joinAiAgentPath(path)
  /** 标准化后的 URL 对象。 */
  const url = createAiAgentUrl(path, query)

  if (typeof window === 'undefined') {
    return `${joinedUrl}${url.search}`
  }

  return url.toString()
}

/**
 * 解析 AI Agent WebSocket 地址。
 * 同源 AI Agent HTTP 地址会自动切换为 `ws` / `wss` 协议。
 *
 * @param path - 目标 WebSocket 路径
 * @param query - 需要附加的查询参数
 * @returns 可直接建立 WebSocket 连接的地址
 */
export const resolveAiAgentWebSocketUrl = (path: string, query: AiAgentUrlQuery = {}): string => {
  /** 拼接后的原始地址字符串。 */
  const joinedUrl = joinAiAgentPath(path)
  /** 标准化后的 URL 对象。 */
  const url = createAiAgentUrl(path, query)

  if (url.protocol === 'https:') {
    url.protocol = 'wss:'
  } else if (url.protocol === 'http:') {
    url.protocol = 'ws:'
  }

  if (typeof window === 'undefined') {
    return `${joinedUrl}${url.search}`
  }

  return url.toString()
}
