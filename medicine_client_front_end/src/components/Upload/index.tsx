import React, { useState, useEffect, useRef } from 'react'
import { Uploader } from '@nutui/nutui-react'
import type { UploaderFileItem } from '@nutui/nutui-react'
import { Photograph } from '@nutui/icons-react'
import { uploadFile } from '@/api/common'
import styles from './index.module.less'

interface UploadProps {
  value?: string[]
  onChange?: (value: string[]) => void
  maxCount?: number
  uploadLabel?: string
}

const Upload: React.FC<UploadProps> = ({ value = [], onChange, maxCount = 5, uploadLabel = '上传图片' }) => {
  const [fileList, setFileList] = useState<UploaderFileItem[]>([])
  const lastEmittedValue = useRef<string[]>([])

  // Sync value prop to fileList when it changes externally
  useEffect(() => {
    if (JSON.stringify(value) !== JSON.stringify(lastEmittedValue.current)) {
      const newFileList = value.map(url => ({
        uid: url,
        name: 'image',
        url: url,
        status: 'success',
        type: 'image'
      }))
      setFileList(newFileList as UploaderFileItem[])
      lastEmittedValue.current = value
    }
  }, [value])

  const onUpload = async (file: File) => {
    try {
      const res = await uploadFile(file)
      return {
        url: res.fileUrl,
        name: res.fileName,
        type: 'image' as const
      }
    } catch (error) {
      console.error('Upload failed', error)
      throw new Error('Upload failed')
    }
  }

  const handleChange = (files: UploaderFileItem[]) => {
    setFileList(files)

    const successUrls = files.filter(f => f.status === 'success' && f.url).map(f => f.url!)

    if (JSON.stringify(successUrls) !== JSON.stringify(lastEmittedValue.current)) {
      lastEmittedValue.current = successUrls
      onChange?.(successUrls)
    }
  }

  return (
    <div className={styles.uploadWrapper}>
      <Uploader
        value={fileList}
        onChange={handleChange}
        upload={onUpload}
        maxCount={maxCount}
        uploadIcon={<Photograph />}
        uploadLabel={uploadLabel}
        multiple={false}
        className={styles.uploader}
      />
    </div>
  )
}

export default Upload
