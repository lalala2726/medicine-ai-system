import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import {
  DrawerForm,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
} from '@ant-design/pro-components';
import { message } from 'antd';
import React from 'react';
import type { RoleAddRequest, RoleListVo, RoleUpdateRequest } from '@/api/system/role';
import { addRole, getRole, updateRole } from '@/api/system/role';

export type RoleFormProps = {
  open: boolean;
  onOpenChange: (visible: boolean) => void;
  onFinish: () => void;
  initialValues?: RoleListVo;
};

const RoleForm: React.FC<RoleFormProps> = (props) => {
  const { open, onOpenChange, onFinish, initialValues } = props;
  const isEdit = !!initialValues?.id;

  return (
    <DrawerForm<RoleAddRequest | RoleUpdateRequest>
      title={isEdit ? '编辑角色' : '新建角色'}
      width="600px"
      open={open}
      onOpenChange={onOpenChange}
      drawerProps={{
        destroyOnHidden: true,
        onClose: () => {
          onOpenChange(false);
        },
      }}
      request={async () => {
        if (isEdit && initialValues?.id) {
          try {
            const data = await getRole(initialValues.id);
            return data as RoleUpdateRequest;
          } catch (_error) {
            message.error('获取角色详情失败');
          }
        }
        return (initialValues || {}) as RoleAddRequest;
      }}
      onFinish={async (values) => {
        try {
          if (isEdit) {
            // 编辑角色
            await updateRole({
              ...values,
              id: initialValues.id!,
            } as RoleUpdateRequest);
          } else {
            // 新建角色
            await addRole(values as RoleAddRequest);
          }

          message.success(isEdit ? '编辑成功' : '新建成功');
          onFinish();
          return true;
        } catch (_error) {
          message.error(isEdit ? '编辑失败，请重试' : '新建失败，请重试');
          return false;
        }
      }}
    >
      <ProFormText
        fieldProps={{ autoComplete: TEXT_INPUT_AUTOCOMPLETE }}
        name="roleCode"
        label="角色标识"
        placeholder="请输入角色标识"
        rules={[
          { required: true, message: '请输入角色标识' },
          { min: 2, max: 50, message: '角色标识长度为2-50个字符' },
          {
            pattern: /^[a-zA-Z0-9_]+$/,
            message: '角色标识只能包含字母、数字 and 下划线',
          },
        ]}
        disabled={isEdit}
        tooltip="角色标识一旦创建不可修改，用于系统内部识别"
      />

      <ProFormText
        fieldProps={{ autoComplete: TEXT_INPUT_AUTOCOMPLETE }}
        name="roleName"
        label="角色名称"
        placeholder="请输入角色名称"
        rules={[
          { required: true, message: '请输入角色名称' },
          { min: 2, max: 50, message: '角色名称长度为2-50个字符' },
        ]}
      />

      <ProFormTextArea
        name="remark"
        label="备注"
        placeholder="请输入备注信息"
        rules={[{ max: 200, message: '备注最多200个字符' }]}
        fieldProps={{ autoComplete: TEXT_INPUT_AUTOCOMPLETE, rows: 4 }}
      />

      <ProFormSelect
        name="status"
        label="状态"
        options={[
          { label: '启用', value: 0 },
          { label: '禁用', value: 1 },
        ]}
        placeholder="请选择状态"
        rules={[{ required: true, message: '请选择状态' }]}
        initialValue={0}
      />
    </DrawerForm>
  );
};

export default RoleForm;
