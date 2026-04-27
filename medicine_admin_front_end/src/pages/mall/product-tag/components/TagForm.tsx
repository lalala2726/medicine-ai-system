import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { ModalForm, ProFormDigit, ProFormSelect, ProFormText } from '@ant-design/pro-components';
import { message } from 'antd';
import React from 'react';
import {
  addProductTag,
  updateProductTag,
  updateProductTagStatus,
  type MallProductTagTypes,
} from '@/api/mall/productTag';
import type { MallProductTagTypeTypes } from '@/api/mall/productTagType';

interface TagFormValues {
  /** 标签名称 */
  name?: string;
  /** 标签类型 ID */
  typeId?: string;
  /** 排序值 */
  sort?: number;
  /** 状态 */
  status?: number;
}

export interface TagFormProps {
  /** 弹窗是否打开 */
  open: boolean;
  /** 弹窗显隐回调 */
  onOpenChange: (open: boolean) => void;
  /** 提交成功回调 */
  onFinish: () => void;
  /** 当前编辑记录 */
  initialValues?: MallProductTagTypes.MallProductTagAdminVo;
  /** 可选标签类型 */
  typeOptions: MallProductTagTypeTypes.MallProductTagTypeVo[];
  /** 固定标签类型ID */
  fixedTypeId?: string;
  /** 是否锁定标签类型选择 */
  lockTypeSelection?: boolean;
}

/**
 * 商品标签表单。
 */
const TagForm: React.FC<TagFormProps> = ({
  open,
  onOpenChange,
  onFinish,
  initialValues,
  typeOptions,
  fixedTypeId,
  lockTypeSelection = false,
}) => {
  const isEdit = !!initialValues?.id;

  return (
    <ModalForm<TagFormValues>
      key={initialValues?.id ?? 'create-tag'}
      title={isEdit ? '编辑商品标签' : '新建商品标签'}
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
        typeId: initialValues?.typeId ? String(initialValues.typeId) : fixedTypeId,
      }}
      onFinish={async (values) => {
        try {
          if (isEdit && initialValues?.id) {
            await updateProductTag({
              id: initialValues.id,
              name: String(values.name || '').trim(),
              typeId: String(values.typeId || fixedTypeId || ''),
              sort: Number(values.sort ?? 0),
            });

            if (
              values.status !== undefined &&
              Number(values.status) !== Number(initialValues.status)
            ) {
              await updateProductTagStatus({
                id: initialValues.id,
                status: Number(values.status),
              });
            }
          } else {
            await addProductTag({
              name: String(values.name || '').trim(),
              typeId: String(values.typeId || fixedTypeId || ''),
              sort: Number(values.sort ?? 0),
              status: Number(values.status ?? 1),
            });
          }

          message.success(isEdit ? '商品标签更新成功' : '商品标签创建成功');
          onFinish();
          return true;
        } catch {
          message.error(isEdit ? '商品标签更新失败，请重试' : '商品标签创建失败，请重试');
          return false;
        }
      }}
    >
      <ProFormSelect
        name="typeId"
        label="标签类型"
        placeholder="请选择标签类型"
        options={typeOptions.map((option) => ({
          label: `${option.name}${option.code ? ` (${option.code})` : ''}`,
          value: option.id,
        }))}
        disabled={lockTypeSelection}
        rules={[{ required: true, message: '请选择标签类型' }]}
      />

      <ProFormText
        fieldProps={{ autoComplete: TEXT_INPUT_AUTOCOMPLETE }}
        name="name"
        label="标签名称"
        placeholder="请输入标签名称"
        rules={[
          { required: true, message: '请输入标签名称' },
          { max: 64, message: '标签名称最多 64 个字符' },
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
    </ModalForm>
  );
};

export default TagForm;
