import { PlusOutlined } from '@ant-design/icons';
import {
  DndContext,
  type DragEndEvent,
  DragOverlay,
  type DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { arrayMove, rectSortingStrategy, SortableContext, useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Image, Upload } from 'antd';
import type { UploadFile, UploadProps } from 'antd/es/upload/interface';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import type { CustomUploadRequestOption } from '@/components/Upload/utils';

import {
  buildAcceptFromTypes,
  buildBeforeUpload,
  createServiceUploader,
  mapResponseToFileList,
  type UploadChangeParam,
  type UploadValidationOptions,
} from '@/components/Upload/utils';

// 可拖拽的上传项组件
interface DraggableUploadListItemProps {
  originNode: React.ReactElement;
  file: UploadFile;
}

const DraggableUploadListItem: React.FC<DraggableUploadListItemProps> = ({ originNode, file }) => {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: file.uid,
  });

  const style: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition,
    cursor: 'move',
    opacity: isDragging ? 0.5 : 1, // 拖拽时半透明,显示预测位置
  };

  return (
    <div ref={setNodeRef} style={style} {...attributes} {...listeners}>
      {originNode}
    </div>
  );
};

export interface ImageUploadListProps extends UploadValidationOptions {
  value?: UploadFile[];
  onChange?: (fileList: UploadFile[]) => void;
  maxCount?: number;
  disabled?: boolean;
  tip?: React.ReactNode;
  multiple?: boolean;
}

const ImageUploadList: React.FC<ImageUploadListProps> = ({
  value,
  onChange,
  maxCount = 8,
  disabled = false,
  tip,
  allowedTypes,
  maxSizeMB,
  customValidator,
  multiple = true,
}) => {
  const [fileList, setFileList] = useState<UploadFile[]>(value ?? []);
  const [activeId, setActiveId] = useState<string | null>(null);

  // 配置传感器,只响应指针移动超过 8px 才开始拖拽
  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    }),
  );

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
    (nextList: UploadFile[]) => {
      setFileList(nextList);
      onChange?.(nextList);
    },
    [onChange],
  );

  const uploader = useMemo(() => createServiceUploader(), []);

  const handleCustomRequest = useCallback(
    async (options: CustomUploadRequestOption) => {
      const { onError } = options;
      try {
        await uploader(options);
      } catch (error) {
        onError?.(error as Error);
      }
    },
    [uploader],
  );

  const handleChange: UploadProps['onChange'] = (info: UploadChangeParam) => {
    // 过滤掉验证失败的文件（status 为 'error' 或没有响应且不是上传中的文件）
    const validFileList = info.fileList.filter((file) => {
      // 如果文件状态是 error，直接过滤掉
      if (file.status === 'error') {
        return false;
      }

      // 如果文件没有响应数据且不是正在上传或已完成的状态，可能是验证失败的文件
      if (
        !file.response &&
        file.status !== 'uploading' &&
        file.status !== 'done' &&
        file.status !== undefined
      ) {
        return false;
      }

      // 如果文件有 size 和 maxSizeMB 配置，检查大小
      return !(file.size && maxSizeMB && file.size / 1024 / 1024 > maxSizeMB);
    });

    triggerChange(mapResponseToFileList(validFileList));
  };

  // 处理拖拽开始
  const handleDragStart = useCallback((event: DragStartEvent) => {
    setActiveId(event.active.id as string);
  }, []);

  // 处理拖拽结束
  const handleDragEnd = useCallback(
    (event: DragEndEvent) => {
      const { active, over } = event;

      setActiveId(null);

      if (!over || active.id === over.id) {
        return;
      }

      const oldIndex = fileList.findIndex((item) => item.uid === active.id);
      const newIndex = fileList.findIndex((item) => item.uid === over.id);

      if (oldIndex !== -1 && newIndex !== -1) {
        const newFileList = arrayMove(fileList, oldIndex, newIndex);
        triggerChange(newFileList);
      }
    },
    [fileList, triggerChange],
  );

  // 处理图片预览
  const handlePreview = useCallback(
    (file: UploadFile) => {
      const index = fileList.findIndex((f) => f.uid === file.uid);
      if (index !== -1) {
        // 找到对应的隐藏 Image 组件并触发点击
        const hiddenImage = document.querySelector(
          `[data-preview-uid="${file.uid}"]`,
        ) as HTMLElement;
        if (hiddenImage) {
          hiddenImage.click();
        }
      }
    },
    [fileList],
  );

  const uploadButton = useMemo(
    () => (
      <div>
        <PlusOutlined />
        <div style={{ marginTop: 8 }}>上传图片</div>
      </div>
    ),
    [],
  );

  // 获取当前拖拽的文件
  const activeFile = useMemo(
    () => fileList.find((file) => file.uid === activeId),
    [activeId, fileList],
  );

  return (
    <>
      <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
        <SortableContext items={fileList.map((file) => file.uid)} strategy={rectSortingStrategy}>
          <Upload
            accept={accept}
            listType="picture-card"
            fileList={fileList}
            customRequest={handleCustomRequest}
            onChange={handleChange}
            onPreview={handlePreview}
            maxCount={maxCount}
            disabled={disabled}
            beforeUpload={beforeUpload}
            multiple={multiple}
            itemRender={(originNode, file) => (
              <DraggableUploadListItem originNode={originNode} file={file} />
            )}
          >
            {disabled || fileList.length >= maxCount ? null : uploadButton}
          </Upload>
        </SortableContext>

        {/* DragOverlay 显示拖拽时的浮动效果(正常显示,不透明) */}
        <DragOverlay dropAnimation={null}>
          {activeFile ? (
            <div
              style={{
                width: 104,
                height: 104,
                borderRadius: 8,
                overflow: 'hidden',
                boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
              }}
            >
              <img
                src={activeFile.url || activeFile.thumbUrl || undefined}
                alt={activeFile.name}
                style={{
                  width: '100%',
                  height: '100%',
                  objectFit: 'cover',
                }}
              />
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>
      {tip ? <div style={{ color: 'rgba(0, 0, 0, 0.45)' }}>{tip}</div> : null}

      {/* 使用 Ant Design Image.PreviewGroup 实现图片预览和键盘切换 */}
      <Image.PreviewGroup preview={{}}>
        {fileList.map((file) => (
          <Image
            key={file.uid}
            data-preview-uid={file.uid}
            src={file.url || file.thumbUrl || undefined}
            preview={{
              src: file.url || file.thumbUrl || undefined,
            }}
            style={{
              display: 'none',
              position: 'absolute',
              left: -9999,
              top: -9999,
              width: 1,
              height: 1,
            }} // 完全隐藏原图，只用于预览功能
          />
        ))}
      </Image.PreviewGroup>
    </>
  );
};

export default ImageUploadList;
