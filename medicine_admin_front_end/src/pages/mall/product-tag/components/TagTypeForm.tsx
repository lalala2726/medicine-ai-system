import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { ModalForm, ProFormDigit, ProFormSelect, ProFormText } from '@ant-design/pro-components';
import { message } from 'antd';
import React from 'react';
import {
  addProductTagType,
  updateProductTagType,
  type MallProductTagTypeTypes,
} from '@/api/mall/productTagType';

export interface TagTypeFormProps {
  /** 弹窗是否打开 */
  open: boolean;
  /** 弹窗显隐回调 */
  onOpenChange: (open: boolean) => void;
  /** 提交成功回调 */
  onFinish: () => void;
  /** 当前编辑记录 */
  initialValues?: MallProductTagTypeTypes.MallProductTagTypeAdminVo;
}

interface TagTypeFormValues {
  /** 标签类型编码 */
  code?: string;
  /** 标签类型名称 */
  name?: string;
  /** 排序值 */
  sort?: number;
  /** 状态 */
  status?: number;
}

/**
 * 商品标签类型表单。
 */
const TagTypeForm: React.FC<TagTypeFormProps> = ({
  open,
  onOpenChange,
  onFinish,
  initialValues,
}) => {
  const isEdit = !!initialValues?.id;

  return (
    <ModalForm<TagTypeFormValues>
      key={initialValues?.id ?? 'create-tag-type'}
      title={isEdit ? '编辑标签类型' : '新建标签类型'}
      width={560}
      open={open}
      onOpenChange={onOpenChange}
      modalProps={{
        destroyOnHidden: true,
        onCancel: () => onOpenChange(false),
      }}
      initialValues={{
        status: 1,
        sort: 0,
        ...initialValues,
      }}
      onFinish={async (values) => {
        try {
          if (isEdit && initialValues?.id) {
            await updateProductTagType({
              id: initialValues.id,
              name: String(values.name || ''),
              sort: Number(values.sort ?? 0),
            });
          } else {
            await addProductTagType({
              code: String(values.code || '')
                .trim()
                .toUpperCase(),
              name: String(values.name || '').trim(),
              sort: Number(values.sort ?? 0),
              status: Number(values.status ?? 1),
            });
          }

          message.success(isEdit ? '标签类型更新成功' : '标签类型创建成功');
          onFinish();
          return true;
        } catch (_error) {
          message.error(isEdit ? '标签类型更新失败，请重试' : '标签类型创建失败，请重试');
          return false;
        }
      }}
    >
      <ProFormText
        fieldProps={{ autoComplete: TEXT_INPUT_AUTOCOMPLETE }}
        name="code"
        label="类型编码"
        placeholder="请输入标签类型编码"
        disabled={isEdit}
        rules={[
          { required: true, message: '请输入标签类型编码' },
          {
            pattern: /^[A-Z][A-Z0-9_]*$/,
            message: '类型编码需为大写英文、数字或下划线，且必须以字母开头',
          },
        ]}
        tooltip="编码创建后不可修改，例如：EFFICACY、CROWD"
      />

      <ProFormText
        fieldProps={{ autoComplete: TEXT_INPUT_AUTOCOMPLETE }}
        name="name"
        label="类型名称"
        placeholder="请输入标签类型名称"
        rules={[
          { required: true, message: '请输入标签类型名称' },
          { max: 64, message: '类型名称最多 64 个字符' },
        ]}
      />

      <ProFormDigit
        name="sort"
        label="排序值"
        placeholder="请输入排序值"
        initialValue={0}
        fieldProps={{
          autoComplete: TEXT_INPUT_AUTOCOMPLETE,
          min: 0,
          precision: 0,
          style: { width: '100%' },
        }}
      />

      {!isEdit && (
        <ProFormSelect
          name="status"
          label="状态"
          initialValue={1}
          options={[
            { label: '启用', value: 1 },
            { label: '禁用', value: 0 },
          ]}
          rules={[{ required: true, message: '请选择状态' }]}
        />
      )}
    </ModalForm>
  );
};

export default TagTypeForm;
