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

export interface ControlledUploadProps extends UploadValidationOptions {
  value?: UploadFile[];
  onChange?: (fileList: UploadFile[]) => void;
  maxCount?: number;
  multiple?: boolean;
  listType?: UploadProps['listType'];
  disabled?: boolean;
  buttonText?: string;
}

const ControlledUpload: React.FC<ControlledUploadProps> = ({
  value,
  onChange,
  maxCount = 2,
  multiple = true,
  listType = 'text',
  disabled = false,
  buttonText = '上传文件',
  allowedTypes,
  maxSizeMB,
  customValidator,
}) => {
  const [fileList, setFileList] = useState<UploadFile[]>(value ?? []);

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
      const limited = info.fileList.slice(-maxCount);
      const normalized = mapResponseToFileList(limited);
      triggerChange(normalized);
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
      multiple={multiple}
      listType={listType}
      fileList={fileList}
      disabled={disabled}
      beforeUpload={beforeUpload}
      customRequest={customRequest}
      onChange={handleChange}
      maxCount={maxCount}
    >
      <Button icon={<UploadOutlined />} disabled={disabled}>
        {buttonText}
      </Button>
    </Upload>
  );
};

export default ControlledUpload;
