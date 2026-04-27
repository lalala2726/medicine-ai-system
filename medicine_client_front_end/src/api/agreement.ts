import requestClient from '@/request/requestClient'

/**
 * 软件协议配置详情。
 */
export interface AgreementConfigVo {
  /** 软件协议 Markdown 内容。 */
  softwareAgreementMarkdown: string
  /** 隐私协议 Markdown 内容。 */
  privacyAgreementMarkdown: string
  /** 最后更新时间。 */
  updatedTime?: string
}

/**
 * 获取软件协议配置。
 *
 * @returns 软件协议配置详情
 */
export const getAgreementConfig = async (): Promise<AgreementConfigVo> => {
  return requestClient.get<AgreementConfigVo>('/auth/agreement')
}
