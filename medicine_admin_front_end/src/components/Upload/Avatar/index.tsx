import { LoadingOutlined, PlusOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import { Flex, Upload } from 'antd';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import type { CustomUploadRequestOption, RcFile } from '@/components/Upload/utils';

import {
  buildAcceptFromTypes,
  buildBeforeUpload,
  createServiceUploader,
  type UploadChangeParam,
  type UploadValidationOptions,
} from '@/components/Upload/utils';

export type AvatarShape = 'square' | 'circle';

export interface AvatarUploadProps extends UploadValidationOptions {
  value?: string;
  onChange?: (url: string, file?: UploadFile) => void;
  /** 默认渲染的样式，支持多种形态 */
  shapes?: AvatarShape[];
  disabled?: boolean;
  uploadText?: string;
}

const defaultShapes: AvatarShape[] = ['square'];

const AvatarUpload: React.FC<AvatarUploadProps> = ({
  value,
  onChange,
  shapes = defaultShapes,
  allowedTypes = ['image/*'],
  maxSizeMB = 2,
  disabled = false,
  uploadText = '上传头像',
  customValidator,
}) => {
  const [loading, setLoading] = useState(false);
  const [imageUrl, setImageUrl] = useState<string | undefined>(value);

  // 当value变化时更新imageUrl
  useEffect(() => {
    setImageUrl(value);
  }, [value]);

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

  const handleSuccess = useCallback(
    (file: UploadFile, response: any) => {
      const url = response?.fileUrl || file.url;
      setImageUrl(url);
      onChange?.(url as string, file);
    },
    [onChange],
  );

  const handleChange: UploadProps['onChange'] = useCallback(
    (info: UploadChangeParam) => {
      const { file } = info;

      if (file.status === 'uploading') {
        setLoading(true);
        return;
      }

      if (file.status === 'done') {
        setLoading(false);
        handleSuccess(file, file.response);
      }

      if (file.status === 'error') {
        setLoading(false);
      }
    },
    [handleSuccess],
  );

  const customRequest = useMemo(() => {
    const uploader = createServiceUploader();

    return async (options: CustomUploadRequestOption) => {
      setLoading(true);
      await uploader({ ...options, file: options.file as RcFile });
    };
  }, []);

  const uploadButton = (
    <button style={{ border: 0, background: 'none' }} type="button">
      {loading ? <LoadingOutlined /> : <PlusOutlined />}
      <div style={{ marginTop: 8 }}>{uploadText}</div>
    </button>
  );

  const renderUpload = (shape: AvatarShape) => {
    const isCircle = shape === 'circle';

    return (
      <Upload
        key={shape}
        accept={accept}
        listType={isCircle ? 'picture-circle' : 'picture-card'}
        showUploadList={false}
        beforeUpload={beforeUpload}
        customRequest={customRequest}
        onChange={handleChange}
        disabled={disabled}
      >
        {imageUrl ? (
          <img
            draggable={false}
            src={imageUrl}
            alt="avatar"
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              borderRadius: isCircle ? '50%' : '8px',
            }}
          />
        ) : (
          uploadButton
        )}
      </Upload>
    );
  };

  return (
    <Flex gap="middle" wrap>
      {shapes.map(renderUpload)}
    </Flex>
  );
};

export default AvatarUpload;
