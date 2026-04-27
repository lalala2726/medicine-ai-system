import type { UploadFile, UploadProps } from 'antd';
import { Upload } from 'antd';
import ImgCrop from 'antd-img-crop';
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

export interface ImageCropUploadProps extends UploadValidationOptions {
  value?: UploadFile[];
  onChange?: (fileList: UploadFile[]) => void;
  maxCount?: number;
  disabled?: boolean;
  rotationSlider?: boolean;
  aspect?: number;
}

const ImageCropUpload: React.FC<ImageCropUploadProps> = ({
  value,
  onChange,
  maxCount = 5,
  disabled = false,
  rotationSlider = true,
  aspect,
  allowedTypes = ['image/*'],
  maxSizeMB,
  customValidator,
}) => {
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

  const handlePreview = useCallback(async (file: UploadFile) => {
    let src = file.url as string;
    if (!src && file.originFileObj) {
      src = await new Promise<string>((resolve) => {
        const reader = new FileReader();
        reader.readAsDataURL(file.originFileObj as RcFile);
        reader.onload = () => resolve(reader.result as string);
      });
    }

    if (!src) {
      return;
    }

    const image = new Image();
    image.src = src;
    const win = window.open(src);
    win?.document.write(image.outerHTML);
  }, []);

  return (
    <ImgCrop rotationSlider={rotationSlider} aspect={aspect} modalTitle="裁剪图片">
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
        {fileList.length >= maxCount ? null : '+ 上传'}
      </Upload>
    </ImgCrop>
  );
};

export default ImageCropUpload;
