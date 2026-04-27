/** 后端错误来源类型。 */
export type ServerErrorSource = 'business' | 'ai'

/** AI Agent 同源代理路径片段。 */
const AI_AGENT_PATH_FRAGMENT = '/ai_agent'

/** 服务端错误来源对应的控制台标签。 */
const SERVER_ERROR_SOURCE_LABELS: Record<ServerErrorSource, string> = {
  business: '业务服务器错误',
  ai: 'AI端服务器错误'
}

/** 后端可能拼接在错误前面的来源前缀。 */
const SERVER_ERROR_PREFIX_PATTERN =
  /^(?:(?:业务|AI|ai|Ai)\s*(?:端)?\s*服务器|(?:AI|ai|Ai)\s*端)\s*(?:请求)?(?:失败|错误|异常)\s*[：:]\s*/

/**
 * 根据请求地址判断当前错误来源。
 *
 * @param url - 当前请求地址
 * @returns 当前请求对应的服务端来源
 */
export const resolveServerErrorSourceByUrl = (url?: string | null): ServerErrorSource => {
  if (typeof url === 'string' && url.includes(AI_AGENT_PATH_FRAGMENT)) {
    return 'ai'
  }

  return 'business'
}

/**
 * 去掉后端错误文案中的来源前缀，界面只展示真正错误原因。
 *
 * @param message - 后端或请求库返回的错误文案
 * @param fallbackMessage - message 为空时使用的默认文案
 * @returns 去掉来源前缀后的展示文案
 */
export const normalizeServerErrorMessage = (message?: string | null, fallbackMessage = '请求失败'): string => {
  /** 当前待处理的错误文案。 */
  let normalizedMessage = typeof message === 'string' ? message.trim() : ''

  while (SERVER_ERROR_PREFIX_PATTERN.test(normalizedMessage)) {
    normalizedMessage = normalizedMessage.replace(SERVER_ERROR_PREFIX_PATTERN, '').trim()
  }

  return normalizedMessage || fallbackMessage
}

/**
 * 在控制台输出带服务端来源的错误信息。
 *
 * @param source - 当前错误来源
 * @param context - 当前请求或业务上下文
 * @param error - 原始错误对象
 * @returns 无返回值
 */
export const logServerError = (source: ServerErrorSource, context: string, error: unknown): void => {
  console.error(`[${SERVER_ERROR_SOURCE_LABELS[source]}] ${context}`, error)
}
