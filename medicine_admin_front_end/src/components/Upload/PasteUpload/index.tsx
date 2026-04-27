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

export interface PasteUploadProps extends UploadValidationOptions {
  buttonText?: string;
  disabled?: boolean;
}

const PasteUpload: React.FC<PasteUploadProps> = ({
  buttonText = '粘贴或点击上传',
  disabled = false,
  allowedTypes,
  maxSizeMB,
  customValidator,
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
    pastable: true,
    beforeUpload,
    customRequest,
    disabled,
    onChange(info: UploadChangeParam) {
      if (info.file.status === 'done') {
        message.success(`${info.file.name} 上传完成`);
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

export default PasteUpload;
