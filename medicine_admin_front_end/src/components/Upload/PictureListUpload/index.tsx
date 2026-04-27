import { UploadOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import { Button, Upload } from 'antd';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import type { CustomUploadRequestOption, RcFile } from '@/components/Upload/utils';

import {
  buildAcceptFromTypes,
  buildBeforeUpload,
  createServiceUploader,
  mapResponseToFileList,
  type UploadChangeParam,
  type UploadValidationOptions,
} from '@/components/Upload/utils';

export interface PictureListUploadProps extends UploadValidationOptions {
  value?: UploadFile[];
  defaultFileList?: UploadFile[];
  onChange?: (fileList: UploadFile[]) => void;
  maxCount?: number;
  disabled?: boolean;
  buttonText?: string;
}

const PictureListUpload: React.FC<PictureListUploadProps> = ({
  value,
  defaultFileList,
  onChange,
  maxCount,
  disabled = false,
  buttonText = '上传图片',
  allowedTypes = ['image/*'],
  maxSizeMB,
  customValidator,
}) => {
  const [fileList, setFileList] = useState<UploadFile[]>(value ?? defaultFileList ?? []);

  useEffect(() => {
    if (value) {
      setFileList(value);
    }
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

  const triggerChange = useCallback(
    (next: UploadFile[]) => {
      setFileList(next);
      onChange?.(next);
    },
    [onChange],
  );

  const handleChange: UploadProps['onChange'] = useCallback(
    (info: UploadChangeParam) => {
      const next = info.fileList;
      const limited = typeof maxCount === 'number' ? next.slice(-maxCount) : next;
      triggerChange(mapResponseToFileList(limited));
    },
    [maxCount, triggerChange],
  );

  const customRequest = useMemo(() => {
    const uploader = createServiceUploader();
    return (options: CustomUploadRequestOption) =>
      uploader({ ...options, file: options.file as RcFile });
  }, []);

  return (
    <Upload
      accept={accept}
      listType="picture"
      disabled={disabled}
      fileList={fileList}
      maxCount={maxCount}
      beforeUpload={beforeUpload}
      customRequest={customRequest}
      onChange={handleChange}
    >
      <Button type="primary" icon={<UploadOutlined />} disabled={disabled}>
        {buttonText}
      </Button>
    </Upload>
  );
};

export default PictureListUpload;
