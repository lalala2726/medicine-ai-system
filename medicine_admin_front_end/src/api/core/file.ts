import { requestClient } from '@/utils/request';

export namespace FileUploadTypes {
  export interface FileUploadVo {
    /** 文件名 */
    fileName?: string;
    /** 文件大小 */
    fileSize?: number;
    /** 文件类型 */
    fileType?: string;
    /** 文件访问地址 */
    fileUrl?: string;
  }
}

/**
 * 文件上传
 * @param file 待上传的文件对象
 * @param onProgress 上传进度回调
 */
export async function upload(file: File, onProgress?: (percent: number) => void) {
  const formData = new FormData();
  formData.append('file', file);

  return requestClient.post('/file/upload', formData, {
    requestType: 'form',
    onUploadProgress: (event: any) => {
      if (!onProgress || !event.total) {
        return;
      }
      onProgress(Number(((event.loaded / event.total) * 100).toFixed(2)));
    },
  });
}
