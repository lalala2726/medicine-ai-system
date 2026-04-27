import { UploadOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import { Button, Flex, message, Upload } from 'antd';
import React, { useCallback, useMemo, useState } from 'react';
import { upload as uploadService } from '@/api/core/file';
import type { RcFile } from '@/components/Upload/utils';
import {
  buildAcceptFromTypes,
  buildBeforeUpload,
  type UploadValidationOptions,
} from '@/components/Upload/utils';

export interface ManualUploadProps extends UploadValidationOptions {
  maxCount?: number;
  selectButtonText?: string;
  uploadButtonText?: string;
  disabled?: boolean;
}

const ManualUpload: React.FC<ManualUploadProps> = ({
  maxCount = 5,
  selectButtonText = '选择文件',
  uploadButtonText = '开始上传',
  disabled = false,
  allowedTypes,
  maxSizeMB,
  customValidator,
}) => {
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [uploading, setUploading] = useState(false);

  const beforeUploadValidator = useMemo(
    () =>
      buildBeforeUpload({
        allowedTypes,
        maxSizeMB,
        customValidator,
      }),
    [allowedTypes, customValidator, maxSizeMB],
  );

  const accept = useMemo(() => buildAcceptFromTypes(allowedTypes), [allowedTypes]);

  const handleUpload = useCallback(async () => {
    if (fileList.length === 0) {
      message.warning('请先选择文件');
      return;
    }

    try {
      setUploading(true);
      await Promise.all(
        fileList.map(async (file) => {
          const origin = (file.originFileObj as File) || (file as unknown as File);
          const response = await uploadService(origin, (percent) => {
            file.percent = percent;
          });
          file.status = 'done';
          file.url = response.fileUrl;
        }),
      );
      message.success('文件上传成功');
      setFileList([]);
    } catch (error: any) {
      message.error(error?.message || '文件上传失败，请稍后再试');
    } finally {
      setUploading(false);
    }
  }, [fileList]);

  const uploadProps: UploadProps = {
    fileList,
    accept,
    multiple: true,
    beforeUpload: async (file) => {
      if (fileList.length >= maxCount) {
        message.warning(`最多只能选择 ${maxCount} 个文件`);
        return Upload.LIST_IGNORE;
      }

      const valid = await beforeUploadValidator(file as RcFile);
      if (!valid) {
        return Upload.LIST_IGNORE;
      }

      setFileList((prev) => [...prev, file]);
      return false;
    },
    onRemove: (file) => {
      setFileList((prev) => prev.filter((item) => item.uid !== file.uid));
    },
    disabled,
  };

  return (
    <Flex vertical gap={16}>
      <Upload {...uploadProps}>
        <Button icon={<UploadOutlined />} disabled={disabled}>
          {selectButtonText}
        </Button>
      </Upload>
      <Button
        type="primary"
        onClick={handleUpload}
        disabled={disabled || fileList.length === 0}
        loading={uploading}
      >
        {uploadButtonText}
      </Button>
    </Flex>
  );
};

export default ManualUpload;
