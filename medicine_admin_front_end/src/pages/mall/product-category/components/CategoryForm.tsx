import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import type { UploadFile } from 'antd';
import { Form, Input, InputNumber, Modal, TreeSelect } from 'antd';
import React, { useEffect, useMemo, useState } from 'react';
import type { MallCategoryTypes } from '@/api/mall/category';
import { tree } from '@/api/mall/category';
import { ImageCropUpload } from '@/components';
import { normalizeRequestUrl } from '@/utils/request/config';

interface CategoryFormProps {
  open: boolean;
  title: string;
  record?: MallCategoryTypes.MallCategoryVo;
  loading: boolean;
  onSubmit: (
    values: MallCategoryTypes.MallCategoryAddRequest | MallCategoryTypes.MallCategoryUpdateRequest,
  ) => Promise<void>;
  onCancel: () => void;
}

const CategoryForm: React.FC<CategoryFormProps> = ({
  open,
  title,
  record,
  loading,
  onSubmit,
  onCancel,
}) => {
  const [form] = Form.useForm();
  const [categoryTree, setCategoryTree] = useState<MallCategoryTypes.MallCategoryTree[]>([]);
  const [optionsLoading, setOptionsLoading] = useState(false);

  // 加载分类树形数据
  const loadCategoryTree = async () => {
    try {
      setOptionsLoading(true);
      const response = await tree();
      setCategoryTree(response);
    } catch (error) {
      console.error('加载分类数据失败:', error);
    } finally {
      setOptionsLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      loadCategoryTree();
      form.resetFields();

      const coverFileList: UploadFile[] = record?.cover
        ? [{ uid: '-1', name: 'cover', status: 'done', url: normalizeRequestUrl(record.cover) }]
        : [];

      form.setFieldsValue({
        name: record?.name,
        parentId:
          record?.parentId === 0 || record?.parentId === '0'
            ? undefined
            : record?.parentId
              ? String(record.parentId)
              : undefined,
        description: record?.description,
        sort: record?.sort ?? 0,
        cover: coverFileList,
      });
    }
  }, [open, record, form]);

  // 将树形数据转换为 TreeSelect 格式
  const treeSelectData = useMemo(() => {
    const transformNodes = (nodes: MallCategoryTypes.MallCategoryTree[]): any[] => {
      return nodes.map((node) => ({
        title: node.name || '未命名分类',
        value: String(node.id),
        key: String(node.id),
        children: node.children ? transformNodes(node.children) : undefined,
      }));
    };

    if (!categoryTree?.length) {
      return [];
    }
    return transformNodes(categoryTree);
  }, [categoryTree]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();

      // 提取封面 URL
      const coverUrl = values.cover?.[0]?.url || values.cover?.[0]?.response?.fileUrl || '';

      const payload = {
        ...values,
        cover: coverUrl,
        parentId: values.parentId ? Number(values.parentId) : 0,
      } as MallCategoryTypes.MallCategoryAddRequest | MallCategoryTypes.MallCategoryUpdateRequest;

      if (record?.id) {
        (payload as MallCategoryTypes.MallCategoryUpdateRequest).id = Number(record.id);
      }

      await onSubmit(payload);
    } catch (error) {
      console.error('分类表单验证失败', error);
    }
  };

  return (
    <Modal
      title={title}
      open={open}
      onCancel={onCancel}
      onOk={handleSubmit}
      confirmLoading={loading}
      width={600}
      destroyOnHidden
      afterClose={() => form.resetFields()}
    >
      <Form form={form} layout="vertical" requiredMark={false}>
        <Form.Item label="上级分类" name="parentId" help="不选择则为顶级分类">
          <TreeSelect
            placeholder="请选择上级分类"
            allowClear
            showSearch
            treeDefaultExpandAll={false}
            loading={optionsLoading}
            treeData={treeSelectData}
            treeNodeFilterProp="title"
            styles={{ popup: { root: { maxHeight: 400, overflow: 'auto' } } }}
          />
        </Form.Item>
        <Form.Item
          label="分类名称"
          name="name"
          rules={[{ required: true, message: '请输入分类名称' }]}
        >
          <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入分类名称" />
        </Form.Item>

        <Form.Item
          label="分类封面"
          name="cover"
          rules={[{ required: true, message: '请上传分类封面' }]}
        >
          <ImageCropUpload maxCount={1} aspect={1} />
        </Form.Item>

        <Form.Item label="排序值" name="sort" help="数值越小排序越靠前">
          <InputNumber
            autoComplete={TEXT_INPUT_AUTOCOMPLETE}
            min={0}
            precision={0}
            placeholder="请输入排序值"
            style={{ width: '100%' }}
            type="number"
          />
        </Form.Item>

        <Form.Item label="分类描述" name="description">
          <Input.TextArea
            autoComplete={TEXT_INPUT_AUTOCOMPLETE}
            rows={4}
            placeholder="请输入分类描述"
            maxLength={200}
            showCount
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CategoryForm;
