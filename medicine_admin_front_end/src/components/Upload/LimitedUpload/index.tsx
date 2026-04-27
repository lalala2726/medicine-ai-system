import { UploadOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import { Button, Space, Upload } from 'antd';
import React, { useMemo, useState } from 'react';
import type { CustomUploadRequestOption, RcFile } from '@/components/Upload/utils';

import {
  buildAcceptFromTypes,
  buildBeforeUpload,
  createServiceUploader,
  mapResponseToFileList,
  type UploadChangeParam,
  type UploadValidationOptions,
} from '@/components/Upload/utils';

export interface LimitedUploadProps extends UploadValidationOptions {
  maxCount?: number;
  multiple?: boolean;
  buttonText?: string;
  listType?: UploadProps['listType'];
  disabled?: boolean;
}

const LimitedUpload: React.FC<LimitedUploadProps> = ({
  maxCount = 3,
  multiple = true,
  buttonText = `最多上传${multiple ? '多个' : '一个'}文件`,
  listType = 'picture',
  disabled = false,
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

  const customRequest = useMemo(() => {
    const uploader = createServiceUploader();
    return (options: CustomUploadRequestOption) =>
      uploader({ ...options, file: options.file as RcFile });
  }, []);

  const handleChange: UploadProps['onChange'] = (info: UploadChangeParam) => {
    const limited = info.fileList.slice(-maxCount);
    setFileList(mapResponseToFileList(limited));
  };

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Upload
        accept={accept}
        listType={listType}
        multiple={multiple}
        maxCount={maxCount}
        beforeUpload={beforeUpload}
        customRequest={customRequest}
        onChange={handleChange}
        fileList={fileList}
        disabled={disabled}
      >
        <Button icon={<UploadOutlined />} disabled={disabled}>
          {buttonText}
        </Button>
      </Upload>
    </Space>
  );
};

export default LimitedUpload;
