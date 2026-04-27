import { nanoid } from 'nanoid'

/** 客户端本地唯一标识默认长度。 */
const CLIENT_ID_DEFAULT_SIZE = 21

/**
 * 生成客户端侧使用的本地唯一标识。
 * 该标识只用于前端临时消息、占位数据等本地场景，不承担后端主键语义。
 *
 * @returns 基于 `nanoid` 生成的本地唯一标识
 */
export const createClientId = (): string => {
  return nanoid(CLIENT_ID_DEFAULT_SIZE)
}
