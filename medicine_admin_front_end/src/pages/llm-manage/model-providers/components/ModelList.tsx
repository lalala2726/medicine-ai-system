import {
  DeleteOutlined,
  EditOutlined,
  HolderOutlined,
  PlusOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import {
  DndContext,
  type DragEndEvent,
  DragOverlay,
  type DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { rectSortingStrategy, SortableContext, useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Button, Divider, Empty, Form, Modal, Tag, Typography, type FormInstance } from 'antd';
import React, { useCallback, useState } from 'react';

import { createEmptyModel, MODEL_TYPE_LABELS, type ProviderFormValues } from '../shared';
import ModelEditDrawer from './ModelEditDrawer';
import styles from './model-list.module.less';

const { Title: AntTitle, Text } = Typography;

// ---- SortableModelCard ----
interface SortableModelCardProps {
  id: string;
  children: (isDragging: boolean) => React.ReactNode;
}

const SortableModelCard: React.FC<SortableModelCardProps> = ({ id, children }) => {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id,
  });

  const style: React.CSSProperties = {
    transform: CSS.Translate.toString(transform),
    transition,
    opacity: isDragging ? 0 : 1,
    position: 'relative',
  };

  return (
    <div ref={setNodeRef} style={style} {...attributes}>
      <div className={styles.dragHandle} {...listeners}>
        <HolderOutlined />
      </div>
      {children(isDragging)}
    </div>
  );
};

// ---- ModelCard content (shared between list and drag overlay) ----
interface ModelCardContentProps {
  model: ProviderFormValues['models'][number];
  disabled?: boolean;
  onEdit?: () => void;
  onDelete?: () => void;
}

const ModelCardContent: React.FC<ModelCardContentProps> = ({
  model,
  disabled,
  onEdit,
  onDelete,
}) => (
  <div className={styles.modelSummaryCard}>
    <div className={styles.cardTop}>
      <div className={styles.modelInfo}>
        <AntTitle level={5} className={styles.modelName} ellipsis>
          {model.modelName}
        </AntTitle>
      </div>
      <Tag color={model.enabled ? 'success' : 'default'} bordered={false}>
        {model.enabled ? '已启用' : '已禁用'}
      </Tag>
    </div>
    <div className={styles.cardMiddle}>
      <Tag color="blue">{MODEL_TYPE_LABELS[model.modelType as keyof typeof MODEL_TYPE_LABELS]}</Tag>
      {model.supportReasoning === 1 && <Tag color="purple">深度思考</Tag>}
      {model.supportVision === 1 && <Tag color="cyan">图片识别</Tag>}
    </div>
    <div className={styles.cardBottom}>
      <div className={styles.modelActions}>
        <Button
          type="text"
          size="small"
          icon={<EditOutlined />}
          disabled={disabled}
          onClick={onEdit}
        >
          编辑
        </Button>
        <Button
          type="text"
          size="small"
          danger
          disabled={disabled}
          icon={<DeleteOutlined />}
          onClick={onDelete}
        />
      </div>
    </div>
  </div>
);

// ---- ModelList ----
interface ModelListProps {
  form: FormInstance<ProviderFormValues>;
}

const ModelList: React.FC<ModelListProps> = ({ form }) => {
  const [modalForm] = Form.useForm();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [activeId, setActiveId] = useState<string | null>(null);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }));

  const models = Form.useWatch('models', form) || [];

  const handleOpenModal = (index: number | null = null) => {
    setEditingIndex(index);
    if (index !== null) {
      modalForm.setFieldsValue(models[index]);
    } else {
      modalForm.resetFields();
      modalForm.setFieldsValue(createEmptyModel());
    }
    setIsModalOpen(true);
  };

  const handleModalOk = async () => {
    try {
      const values = await modalForm.validateFields();
      const currentModels = form.getFieldValue('models') || [];
      const nextModels = [...currentModels];
      if (editingIndex !== null) {
        nextModels[editingIndex] = { ...nextModels[editingIndex], ...values };
      } else {
        nextModels.push(values);
      }
      form.setFieldsValue({ models: nextModels });
      setIsModalOpen(false);
    } catch {
      // 表单校验未通过
    }
  };

  const handleRemoveModel = (index: number) => {
    Modal.confirm({
      title: '删除模型',
      content: '确定要删除这个模型配置吗？',
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: () => {
        const currentModels = form.getFieldValue('models') || [];
        const nextModels = [...currentModels];
        nextModels.splice(index, 1);
        form.setFieldsValue({ models: nextModels });
      },
    });
  };

  const handleDragStart = useCallback((event: DragStartEvent) => {
    setActiveId(event.active.id as string);
  }, []);

  return (
    <div className={styles.stepContent}>
      <div className={styles.modelHeader}>
        <div>
          <AntTitle level={4} style={{ margin: 0 }}>
            <SettingOutlined /> 模型映射配置
          </AntTitle>
          <Text type="secondary">配置可用的模型，点击下方卡片或「添加模型」进行编辑。</Text>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => handleOpenModal()}
          shape="round"
        >
          添加模型
        </Button>
      </div>

      <Divider />

      <Form.List name="models">
        {(fields, { move }) => {
          if (fields.length === 0) {
            return (
              <div className={styles.emptyBlock}>
                <Empty description="暂无模型配置" />
                <Button type="dashed" icon={<PlusOutlined />} onClick={() => handleOpenModal()}>
                  立即添加第一个模型
                </Button>
              </div>
            );
          }

          const sortableIds = fields.map((field) => String(field.key));

          const onDragEnd = (event: DragEndEvent) => {
            const { active, over } = event;
            setActiveId(null);
            if (!over || active.id === over.id) return;
            const oldIndex = fields.findIndex((f) => String(f.key) === String(active.id));
            const newIndex = fields.findIndex((f) => String(f.key) === String(over.id));
            if (oldIndex !== -1 && newIndex !== -1) {
              move(oldIndex, newIndex);
            }
          };

          return (
            <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={onDragEnd}>
              <SortableContext items={sortableIds} strategy={rectSortingStrategy}>
                <div className={styles.modelGrid}>
                  {fields.map((field) => {
                    const model = form.getFieldValue(['models', field.name]);
                    if (!model) return null;
                    return (
                      <SortableModelCard key={field.key} id={String(field.key)}>
                        {(isDragging) => (
                          <ModelCardContent
                            model={model}
                            disabled={isDragging}
                            onEdit={() => handleOpenModal(field.name)}
                            onDelete={() => handleRemoveModel(field.name)}
                          />
                        )}
                      </SortableModelCard>
                    );
                  })}
                </div>
              </SortableContext>
              <DragOverlay>
                {activeId !== null
                  ? (() => {
                      const activeField = fields.find((f) => String(f.key) === activeId);
                      if (!activeField) return null;
                      const dragModel = form.getFieldValue(['models', activeField.name]);
                      if (!dragModel) return null;
                      return (
                        <div
                          style={{ boxShadow: '0 8px 24px rgba(0,0,0,0.15)', cursor: 'grabbing' }}
                        >
                          <ModelCardContent model={dragModel} disabled />
                        </div>
                      );
                    })()
                  : null}
              </DragOverlay>
            </DndContext>
          );
        }}
      </Form.List>

      <ModelEditDrawer
        open={isModalOpen}
        editingIndex={editingIndex}
        form={modalForm}
        onClose={() => setIsModalOpen(false)}
        onOk={handleModalOk}
      />
    </div>
  );
};

export default ModelList;
