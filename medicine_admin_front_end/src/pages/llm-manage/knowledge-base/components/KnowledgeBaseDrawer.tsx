import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { LoadingOutlined, PlusOutlined } from '@ant-design/icons';
import { Button, Drawer, Form, Input, Select, Switch, Upload, message } from 'antd';
import type { UploadProps } from 'antd';
import ImgCrop from 'antd-img-crop';
import React, { useCallback, useEffect, useMemo, useState } from 'react';

import {
  addKnowledgeBase,
  getKnowledgeBaseById,
  updateKnowledgeBase,
} from '@/api/llm-manage/knowledgeBase';
import { getEmbeddingModelOptions, type SystemModelTypes } from '@/api/llm-manage/systemModels';
import {
  buildBeforeUpload,
  createServiceUploader,
  type CustomUploadRequestOption,
  type RcFile,
  type UploadChangeParam,
} from '@/components/Upload/utils';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import styles from './KnowledgeBaseDrawer.module.less';

const { TextArea } = Input;

interface KnowledgeBaseDrawerProps {
  open: boolean;
  editId?: number | null;
  onClose: () => void;
  onSuccess: () => void;
}

const KnowledgeBaseDrawer: React.FC<KnowledgeBaseDrawerProps> = ({
  open,
  editId,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [coverLoading, setCoverLoading] = useState(false);
  const [coverUrl, setCoverUrl] = useState<string>();
  const [detailLoading, setDetailLoading] = useState(false);
  const [embeddingModelOptions, setEmbeddingModelOptions] = useState<
    SystemModelTypes.ModelOption[]
  >([]);
  const [embeddingModelLoading, setEmbeddingModelLoading] = useState(false);

  const isEdit = editId != null;

  useEffect(() => {
    if (!open) {
      form.resetFields();
      setCoverUrl(undefined);
      return;
    }
    if (isEdit && editId) {
      setDetailLoading(true);
      getKnowledgeBaseById(editId)
        .then((data) => {
          form.setFieldsValue({
            displayName: data.displayName,
            knowledgeName: data.knowledgeName,
            cover: data.cover,
            description: data.description,
            embeddingModel: data.embeddingModel,
            embeddingDim: data.embeddingDim,
            status: data.status === 0,
          });
          setCoverUrl(data.cover);
        })
        .finally(() => setDetailLoading(false));
    }
  }, [open, editId, isEdit, form]);

  useEffect(() => {
    if (!open || isEdit) {
      return;
    }
    let active = true;
    setEmbeddingModelLoading(true);
    getEmbeddingModelOptions()
      .then((options) => {
        if (!active) {
          return;
        }
        setEmbeddingModelOptions(options || []);
        const currentModel = form.getFieldValue('embeddingModel');
        if (currentModel && !(options || []).some((item) => item.value === currentModel)) {
          form.setFieldValue('embeddingModel', undefined);
        }
      })
      .catch(() => {
        if (!active) {
          return;
        }
        setEmbeddingModelOptions([]);
        message.error('加载向量模型选项失败');
      })
      .finally(() => {
        if (active) {
          setEmbeddingModelLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [open, isEdit, form]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      if (isEdit) {
        await updateKnowledgeBase({
          id: editId!,
          displayName: values.displayName,
          cover: values.cover,
          description: values.description,
          status: values.status ? 0 : 1,
        });
        message.success('更新成功');
      } else {
        await addKnowledgeBase({
          knowledgeName: values.knowledgeName,
          displayName: values.displayName,
          cover: values.cover,
          description: values.description,
          embeddingModel: values.embeddingModel,
          embeddingDim: values.embeddingDim,
          status: values.status ? 0 : 1,
        });
        message.success('创建成功');
      }
      onSuccess();
      onClose();
    } finally {
      setLoading(false);
    }
  };

  const beforeUpload = useMemo(
    () =>
      buildBeforeUpload({
        allowedTypes: ['image/*'],
        maxSizeMB: 2,
      }),
    [],
  );

  const customRequest = useMemo(() => {
    const uploader = createServiceUploader();
    return async (options: CustomUploadRequestOption) => {
      setCoverLoading(true);
      await uploader({ ...options, file: options.file as RcFile });
    };
  }, []);

  const handleCoverChange: UploadProps['onChange'] = useCallback(
    (info: UploadChangeParam) => {
      const { file } = info;
      if (file.status === 'uploading') {
        setCoverLoading(true);
        return;
      }
      if (file.status === 'done') {
        setCoverLoading(false);
        const url = file.response?.fileUrl || file.url;
        setCoverUrl(url);
        form.setFieldValue('cover', url);
      }
      if (file.status === 'error') {
        setCoverLoading(false);
      }
    },
    [form],
  );

  const uploadButton = (
    <div>
      {coverLoading ? <LoadingOutlined /> : <PlusOutlined />}
      <div style={{ marginTop: 8 }}>上传封面</div>
    </div>
  );

  return (
    <Drawer
      title={isEdit ? '编辑知识库' : '新增知识库'}
      width={640}
      open={open}
      onClose={onClose}
      loading={detailLoading}
      footer={
        <div className={styles.footer}>
          <Button onClick={onClose}>取消</Button>
          <PermissionButton
            type="primary"
            loading={loading}
            access={
              isEdit ? ADMIN_PERMISSIONS.knowledgeBase.update : ADMIN_PERMISSIONS.knowledgeBase.add
            }
            onClick={handleSubmit}
          >
            {isEdit ? '保存' : '创建'}
          </PermissionButton>
        </div>
      }
    >
      <Form form={form} layout="vertical" initialValues={{ status: true, embeddingDim: 1024 }}>
        <Form.Item label="封面" name="cover">
          <div className={styles.coverUploadWrap}>
            <ImgCrop rotationSlider aspect={1} modalTitle="裁剪封面">
              <Upload
                accept="image/*"
                listType="picture-card"
                showUploadList={false}
                beforeUpload={beforeUpload}
                customRequest={customRequest}
                onChange={handleCoverChange}
              >
                {coverUrl ? (
                  <img className={styles.coverImage} src={coverUrl} alt="封面" />
                ) : (
                  uploadButton
                )}
              </Upload>
            </ImgCrop>
          </div>
        </Form.Item>

        <Form.Item
          label="展示名称"
          name="displayName"
          rules={[{ required: true, message: '请输入知识库展示名称' }]}
        >
          <Input
            autoComplete={TEXT_INPUT_AUTOCOMPLETE}
            placeholder="请输入知识库展示名称"
            maxLength={50}
          />
        </Form.Item>

        {!isEdit && (
          <Form.Item
            label="业务名称"
            name="knowledgeName"
            rules={[{ required: true, message: '请输入知识库业务名称' }]}
            tooltip="知识库唯一标识，创建后不可修改"
          >
            <Input
              autoComplete={TEXT_INPUT_AUTOCOMPLETE}
              placeholder="请输入知识库业务名称（英文/数字/下划线）"
              maxLength={50}
            />
          </Form.Item>
        )}

        <Form.Item label="描述" name="description">
          <TextArea rows={3} placeholder="请输入知识库描述" maxLength={200} showCount />
        </Form.Item>

        {!isEdit && (
          <>
            <Form.Item
              label="向量模型"
              name="embeddingModel"
              rules={[{ required: true, message: '请选择向量模型' }]}
              extra={
                embeddingModelLoading
                  ? '正在加载当前激活提供商的向量模型'
                  : embeddingModelOptions.length
                    ? '仅展示当前激活提供商下的可用向量模型'
                    : '当前激活提供商暂无可用向量模型'
              }
            >
              <Select
                placeholder="请选择向量模型"
                loading={embeddingModelLoading}
                options={embeddingModelOptions.map((item) => ({
                  label: item.label,
                  value: item.value,
                }))}
                disabled={embeddingModelLoading || embeddingModelOptions.length === 0}
              />
            </Form.Item>

            <Form.Item
              label="向量维度"
              name="embeddingDim"
              rules={[{ required: true, message: '请选择向量维度' }]}
            >
              <Select placeholder="请选择向量维度">
                <Select.Option value={2048}>2048</Select.Option>
                <Select.Option value={1536}>1536</Select.Option>
                <Select.Option value={1024}>1024</Select.Option>
                <Select.Option value={768}>768</Select.Option>
                <Select.Option value={512}>512</Select.Option>
                <Select.Option value={256}>256</Select.Option>
                <Select.Option value={128}>128</Select.Option>
                <Select.Option value={64}>64</Select.Option>
              </Select>
            </Form.Item>
          </>
        )}

        <Form.Item label="状态" name="status" valuePropName="checked">
          <Switch checkedChildren="启用" unCheckedChildren="停用" />
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default KnowledgeBaseDrawer;
