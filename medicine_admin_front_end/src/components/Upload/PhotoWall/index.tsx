import { PlusOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import { Image, Upload } from 'antd';
import React, { useCallback, useMemo, useState } from 'react';
import type { CustomUploadRequestOption, RcFile } from '@/components/Upload/utils';

import {
  buildAcceptFromTypes,
  buildBeforeUpload,
  createServiceUploader,
  mapResponseToFileList,
  type UploadChangeParam,
  type UploadValidationOptions,
} from '@/components/Upload/utils';

export interface PhotoWallUploadProps extends UploadValidationOptions {
  value?: UploadFile[];
  onChange?: (fileList: UploadFile[]) => void;
  maxCount?: number;
  disabled?: boolean;
  uploadText?: string;
}

const PhotoWallUpload: React.FC<PhotoWallUploadProps> = ({
  value,
  onChange,
  maxCount = 8,
  disabled = false,
  uploadText = '上传',
  allowedTypes = ['image/*'],
  maxSizeMB,
  customValidator,
}) => {
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewImage, setPreviewImage] = useState('');
  const [fileList, setFileList] = useState<UploadFile[]>(value ?? []);

  const beforeUpload = useMemo(
    () =>
      buildBeforeUpload({
        allowedTypes,
        maxSizeMB,
        customValidator,
      }),
    [allowedTypes, customValidator, maxSizeMB],
  );

  const accept = useMemo(() => buildAcceptFromTypes(allowedTypes), [allowedTypes]);

  const handlePreview = useCallback(async (file: UploadFile) => {
    if (!file.url && !file.preview && file.originFileObj) {
      file.preview = await new Promise<string>((resolve) => {
        const reader = new FileReader();
        reader.readAsDataURL(file.originFileObj as RcFile);
        reader.onload = () => resolve(reader.result as string);
      });
    }

    if (!file.url && !file.preview) {
      return;
    }

    setPreviewImage((file.url as string) || (file.preview as string));
    setPreviewOpen(true);
  }, []);

  const triggerChange = useCallback(
    (next: UploadFile[]) => {
      setFileList(next);
      onChange?.(next);
    },
    [onChange],
  );

  const handleChange: UploadProps['onChange'] = useCallback(
    (info: UploadChangeParam) => {
      const limited = info.fileList.slice(-maxCount);
      triggerChange(mapResponseToFileList(limited));
    },
    [maxCount, triggerChange],
  );

  const customRequest = useMemo(() => {
    const uploader = createServiceUploader();
    return (options: CustomUploadRequestOption) =>
      uploader({ ...options, file: options.file as RcFile });
  }, []);

  const uploadButton = (
    <button style={{ border: 0, background: 'none' }} type="button">
      <PlusOutlined />
      <div style={{ marginTop: 8 }}>{uploadText}</div>
    </button>
  );

  return (
    <>
      <Upload
        accept={accept}
        listType="picture-card"
        fileList={fileList}
        maxCount={maxCount}
        disabled={disabled}
        beforeUpload={beforeUpload}
        customRequest={customRequest}
        onChange={handleChange}
        onPreview={handlePreview}
      >
        {disabled || fileList.length >= maxCount ? null : uploadButton}
      </Upload>
      {previewImage ? (
        <Image
          wrapperStyle={{ display: 'none' }}
          preview={{
            visible: previewOpen,
            onVisibleChange: (visible) => setPreviewOpen(visible),
            afterOpenChange: (visible) => !visible && setPreviewImage(''),
          }}
          src={previewImage}
        />
      ) : null}
    </>
  );
};

export default PhotoWallUpload;
