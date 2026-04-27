import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import {
  ModalForm,
  ProFormDigit,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProFormTreeSelect,
} from '@ant-design/pro-components';
import { message } from 'antd';
import React, { useEffect, useState } from 'react';
import type {
  PermissionAddRequest,
  PermissionTreeVo,
  PermissionUpdateRequest,
} from '@/api/system/permission';
import { addPermission, listPermission, updatePermission } from '@/api/system/permission';

export type PermissionFormProps = {
  open: boolean;
  onOpenChange: (visible: boolean) => void;
  onFinish: () => void;
  initialValues?: PermissionTreeVo;
};

const PermissionForm: React.FC<PermissionFormProps> = (props) => {
  const { open, onOpenChange, onFinish, initialValues } = props;
  const isEdit = !!initialValues?.id;
  const [permissionTree, setPermissionTree] = useState<PermissionTreeVo[]>([]);

  // 加载权限树数据
  useEffect(() => {
    if (open) {
      loadPermissionTree();
    }
  }, [open]);

  const loadPermissionTree = async () => {
    try {
      const data = await listPermission();
      // 添加一个根节点选项
      const treeData = [
        {
          id: '0',
          permissionName: '根权限',
          children: data || [],
        },
      ];
      setPermissionTree(treeData);
    } catch (_error) {
      message.error('加载权限树失败');
    }
  };

  // 转换树数据为TreeSelect需要的格式
  const convertToTreeSelectData = (data: PermissionTreeVo[]): any[] => {
    return data.map((item) => ({
      title: item.permissionName,
      value: item.id,
      children: item.children ? convertToTreeSelectData(item.children) : undefined,
    }));
  };

  return (
    <ModalForm<PermissionAddRequest | PermissionUpdateRequest>
      title={isEdit ? '编辑权限' : '新建权限'}
      width="600px"
      open={open}
      onOpenChange={onOpenChange}
      modalProps={{
        destroyOnHidden: true,
        onCancel: () => {
          onOpenChange(false);
        },
      }}
      onFinish={async (values) => {
        try {
          if (isEdit && initialValues.id) {
            // 编辑权限
            await updatePermission({
              ...values,
              id: initialValues.id,
            } as PermissionUpdateRequest);
          } else {
            // 新建权限
            await addPermission(values as PermissionAddRequest);
          }

          message.success(isEdit ? '编辑成功' : '新建成功');
          onFinish();
          return true;
        } catch (_error) {
          message.error(isEdit ? '编辑失败，请重试' : '新建失败，请重试');
          return false;
        }
      }}
      initialValues={initialValues}
    >
      <ProFormTreeSelect
        name="parentId"
        label="父权限"
        placeholder="请选择父权限"
        allowClear
        fieldProps={{
          treeData: convertToTreeSelectData(permissionTree),
          showSearch: true,
          treeDefaultExpandAll: true,
          treeNodeFilterProp: 'title',
        }}
        tooltip="不选择则为顶级权限"
      />

      <ProFormText
        fieldProps={{ autoComplete: TEXT_INPUT_AUTOCOMPLETE }}
        name="permissionCode"
        label="权限标识"
        placeholder="请输入权限标识"
        rules={[
          { required: true, message: '请输入权限标识' },
          { min: 2, max: 100, message: '权限标识长度为2-100个字符' },
          {
            pattern: /^[a-zA-Z0-9:_]+$/,
            message: '权限标识只能包含字母、数字、冒号和下划线',
          },
        ]}
        disabled={isEdit}
        tooltip="权限标识一旦创建不可修改，用于系统内部识别，如：system:user:add"
      />

      <ProFormText
        fieldProps={{ autoComplete: TEXT_INPUT_AUTOCOMPLETE }}
        name="permissionName"
        label="权限名称"
        placeholder="请输入权限名称"
        rules={[
          { required: true, message: '请输入权限名称' },
          { min: 2, max: 50, message: '权限名称长度为2-50个字符' },
        ]}
      />

      <ProFormDigit
        name="sortOrder"
        label="排序"
        placeholder="请输入排序号"
        fieldProps={{ autoComplete: TEXT_INPUT_AUTOCOMPLETE, precision: 0 }}
        tooltip="数字越小越靠前"
        initialValue={0}
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

      <ProFormTextArea
        name="remark"
        label="备注"
        placeholder="请输入备注信息"
        rules={[{ max: 200, message: '备注最多200个字符' }]}
        fieldProps={{ autoComplete: TEXT_INPUT_AUTOCOMPLETE, rows: 4 }}
      />
    </ModalForm>
  );
};

export default PermissionForm;
