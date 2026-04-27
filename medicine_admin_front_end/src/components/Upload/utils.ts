import type { GetProp, UploadProps } from 'antd';
import { message } from 'antd';
import type { UploadFile } from 'antd/es/upload/interface';

import { upload as uploadFile } from '@/api/core/file';

export type RcFile = Parameters<GetProp<UploadProps, 'beforeUpload'>>[0];
export type CustomUploadRequestOption = Parameters<NonNullable<UploadProps['customRequest']>>[0];
export type UploadChangeParam = Parameters<NonNullable<UploadProps['onChange']>>[0];

export interface UploadValidationOptions {
  /** 允许的文件类型，可以是 MIME（image/png）或扩展名（.png） */
  allowedTypes?: string[];
  /** 单个文件允许的最大体积，单位 MB */
  maxSizeMB?: number;
  /** 自定义校验函数，返回 false/Rejected Promise 时中断上传 */
  customValidator?: (file: RcFile) => boolean | Promise<boolean>;
}

const matchFileType = (file: RcFile, rule: string): boolean => {
  const normalizedRule = rule.trim().toLowerCase();
  const fileType = file.type.toLowerCase();
  const fileName = file.name.toLowerCase();

  if (!normalizedRule) {
    return true;
  }

  if (normalizedRule === '*') {
    return true;
  }

  if (normalizedRule.endsWith('/*')) {
    const prefix = normalizedRule.replace('/*', '');
    return fileType.startsWith(prefix);
  }

  if (normalizedRule.startsWith('.')) {
    return fileName.endsWith(normalizedRule);
  }

  return fileType === normalizedRule;
};

export const buildBeforeUpload = (options?: UploadValidationOptions) => async (file: RcFile) => {
  if (!options) {
    return true;
  }

  if (options.allowedTypes && options.allowedTypes.length > 0) {
    const matched = options.allowedTypes.some((rule) => matchFileType(file, rule));
    if (!matched) {
      message.error(`文件类型不支持，仅允许: ${options.allowedTypes.join(', ')}`);
      return false;
    }
  }

  if (options.maxSizeMB && file.size / 1024 / 1024 > options.maxSizeMB) {
    message.error(`文件体积需小于 ${options.maxSizeMB}MB`);
    return false;
  }

  if (options.customValidator) {
    const result = await options.customValidator(file);
    if (!result) {
      return false;
    }
  }

  return true;
};

export const createServiceUploader = () => {
  return async (options: CustomUploadRequestOption) => {
    const { file, onError, onProgress, onSuccess } = options;

    try {
      const response = await uploadFile(file as File, (percent) => {
        onProgress?.({ percent }, file as RcFile);
      });

      onSuccess?.(response, file as RcFile);
    } catch (error: any) {
      message.error(error?.message || '文件上传失败，请稍后再试');
      onError?.(error instanceof Error ? error : new Error('文件上传失败'));
    }
  };
};

export const mapResponseToFileList = (list: UploadFile[]): UploadFile[] =>
  list.map((item) => {
    const response: any = item.response;
    if (!item.url && response?.fileUrl) {
      return {
        ...item,
        url: response.fileUrl,
      };
    }
    return item;
  });

export const buildAcceptFromTypes = (types?: string[]): string | undefined => {
  if (!types || types.length === 0) {
    return undefined;
  }

  return types.join(',');
};
