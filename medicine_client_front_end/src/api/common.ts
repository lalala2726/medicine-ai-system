import requestClient from '@/request/requestClient'

export namespace commonTypes {
  export interface FileUploadVo {
    /** 文件名 */
    fileName?: string
    /** 文件大小 */
    fileSize?: string
    /** 文件类型 */
    fileType?: string
    /** 文件访问地址 */
    fileUrl?: string
  }
}

/**
 * 文件上传
 * @param file 要上传的文件
 * @returns 文件上传结果
 */
export const uploadFile = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)

  return requestClient.post<commonTypes.FileUploadVo>('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
