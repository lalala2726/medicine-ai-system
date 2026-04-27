import { requestClient } from '@/utils/request';

export namespace AgreementConfigTypes {
  /** 软件协议配置详情。 */
  export interface AgreementConfigVo {
    /** 软件协议 Markdown 内容。 */
    softwareAgreementMarkdown: string;
    /** 隐私协议 Markdown 内容。 */
    privacyAgreementMarkdown: string;
    /** 最后更新时间。 */
    updatedTime?: string;
  }

  /** 软件协议配置更新请求。 */
  export interface AgreementConfigUpdateRequest {
    /** 软件协议 Markdown 内容。 */
    softwareAgreementMarkdown: string;
    /** 隐私协议 Markdown 内容。 */
    privacyAgreementMarkdown: string;
  }
}

/**
 * 查询软件协议配置。
 * @returns 软件协议配置详情。
 */
export async function getAgreementConfig() {
  return requestClient.get<AgreementConfigTypes.AgreementConfigVo>('/system/config/agreement');
}

/**
 * 更新软件协议配置。
 * @param data 软件协议配置更新请求。
 * @returns 更新结果。
 */
export async function updateAgreementConfig(
  data: AgreementConfigTypes.AgreementConfigUpdateRequest,
) {
  return requestClient.put<void, AgreementConfigTypes.AgreementConfigUpdateRequest>(
    '/system/config/agreement',
    data,
  );
}
