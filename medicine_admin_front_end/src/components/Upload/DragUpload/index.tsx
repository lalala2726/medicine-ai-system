import { InboxOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import { Typography, Upload } from 'antd';
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

const { Paragraph } = Typography;

export interface DragUploadProps extends UploadValidationOptions {
  onChange?: (fileList: UploadFile[]) => void;
  maxCount?: number;
  disabled?: boolean;
  description?: React.ReactNode;
}

const DragUpload: React.FC<DragUploadProps> = ({
  onChange,
  maxCount,
  disabled = false,
  description = <Paragraph type="secondary">点击或拖拽文件到此区域上传，支持批量上传。</Paragraph>,
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
    <Upload.Dragger
      accept={accept}
      disabled={disabled}
      multiple
      beforeUpload={beforeUpload}
      customRequest={customRequest}
      onChange={handleChange}
      fileList={fileList}
      maxCount={maxCount}
    >
      <p className="ant-upload-drag-icon">
        <InboxOutlined />
      </p>
      <p className="ant-upload-text">点击或拖动文件到此上传</p>
      {description}
    </Upload.Dragger>
  );
};

export default DragUpload;
