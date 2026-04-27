import { UploadOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';
import { Button, message, Upload } from 'antd';
import React, { useMemo } from 'react';
import type { CustomUploadRequestOption, RcFile } from '@/components/Upload/utils';

import {
  buildAcceptFromTypes,
  buildBeforeUpload,
  createServiceUploader,
  type UploadChangeParam,
  type UploadValidationOptions,
} from '@/components/Upload/utils';

export interface RestrictedUploadProps extends UploadValidationOptions {
  buttonText?: string;
  disabled?: boolean;
}

const RestrictedUpload: React.FC<RestrictedUploadProps> = ({
  allowedTypes = ['image/png'],
  maxSizeMB,
  customValidator,
  buttonText = '仅上传 PNG',
  disabled = false,
}) => {
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

  const props: UploadProps = {
    accept,
    beforeUpload,
    customRequest,
    disabled,
    onChange(info: UploadChangeParam) {
      if (info.file.status === 'done') {
        message.success(`${info.file.name} 上传成功`);
      } else if (info.file.status === 'error') {
        message.error(`${info.file.name} 上传失败`);
      }
    },
  };

  return (
    <Upload {...props}>
      <Button icon={<UploadOutlined />} disabled={disabled}>
        {buttonText}
      </Button>
    </Upload>
  );
};

export default RestrictedUpload;
