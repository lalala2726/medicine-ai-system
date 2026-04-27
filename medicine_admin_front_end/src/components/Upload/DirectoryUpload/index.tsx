import { FolderAddOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import { Button, Upload } from 'antd';
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

export interface DirectoryUploadProps extends UploadValidationOptions {
  onChange?: (fileList: UploadFile[]) => void;
  disabled?: boolean;
  buttonText?: string;
}

const DirectoryUpload: React.FC<DirectoryUploadProps> = ({
  onChange,
  disabled = false,
  buttonText = '选择文件夹',
  allowedTypes,
  maxSizeMB,
  customValidator,
}) => {
  const [fileList, setFileList] = useState<UploadFile[]>([]);

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
      triggerChange(mapResponseToFileList(info.fileList));
    },
    [triggerChange],
  );

  const customRequest = useMemo(() => {
    const uploader = createServiceUploader();
    return (options: CustomUploadRequestOption) =>
      uploader({ ...options, file: options.file as RcFile });
  }, []);

  return (
    <Upload
      directory
      multiple
      accept={accept}
      beforeUpload={beforeUpload}
      customRequest={customRequest}
      fileList={fileList}
      onChange={handleChange}
      disabled={disabled}
    >
      <Button icon={<FolderAddOutlined />} disabled={disabled}>
        {buttonText}
      </Button>
    </Upload>
  );
};

export default DirectoryUpload;
