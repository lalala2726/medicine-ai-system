import { PlusOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { useNavigate } from 'react-router-dom';
import { message, Popconfirm, Space, Tag } from 'antd';
import React, { useRef, useState } from 'react';
import { deleteRole, listRole, type RoleListQuery, type RoleListVo } from '@/api/system/role';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { buildSystemRolePermissionPath } from '@/router/paths';

import RoleForm from './components/RoleForm';

/**
 * 角色表格操作列宽度，保证固定列内能完整展示三个操作按钮。
 */
const ROLE_TABLE_ACTION_COLUMN_WIDTH = 220;

const RoleManagement: React.FC = () => {
  const navigate = useNavigate();
  const actionRef = useRef<ActionType>(null);
  const [createDrawerOpen, setCreateDrawerOpen] = useState<boolean>(false);
  const [updateDrawerOpen, setUpdateDrawerOpen] = useState<boolean>(false);
  const [currentRow, setCurrentRow] = useState<RoleListVo>();

  /**
   * 删除角色
   */
  const handleDelete = async (id: string) => {
    const hide = message.loading('正在删除');
    try {
      await deleteRole(id);
      hide();
      message.success('删除成功');
      actionRef.current?.reload();
      return true;
    } catch {
      hide();
      message.error('删除失败，请重试');
      return false;
    }
  };

  /**
   * 批量删除角色
   */
  const handleBatchDelete = async (selectedRows: RoleListVo[]) => {
    const hide = message.loading('正在删除');
    if (!selectedRows || selectedRows.length === 0) return true;
    try {
      const ids = selectedRows.map((row) => row.id).filter((id): id is string => !!id);
      await deleteRole(ids);
      hide();
      message.success('删除成功');
      actionRef.current?.reload();
      return true;
    } catch {
      hide();
      message.error('删除失败，请重试');
      return false;
    }
  };

  const columns: ProColumns<RoleListVo>[] = [
    {
      title: '角色标识',
      dataIndex: 'roleCode',
      ellipsis: true,
    },
    {
      title: '角色名称',
      dataIndex: 'roleName',
      ellipsis: true,
    },
    {
      title: '备注',
      dataIndex: 'remark',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: {
        0: { text: '启用', status: 'Success' },
        1: { text: '禁用', status: 'Error' },
      },
      render: (_, record) => (
        <Tag color={record.status === 0 ? 'green' : 'red'}>
          {record.status === 0 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      valueType: 'dateTime',
      width: 180,
    },
    {
      title: '操作',
      dataIndex: 'option',
      valueType: 'option',
      fixed: 'right',
      width: ROLE_TABLE_ACTION_COLUMN_WIDTH,
      align: 'center',
      render: (_, record) => (
        <TableActionGroup>
          <PermissionButton
            type="link"
            access={ADMIN_PERMISSIONS.systemRole.update}
            onClick={() => {
              if (!record.id) return;
              navigate(buildSystemRolePermissionPath(record.id));
            }}
          >
            分配权限
          </PermissionButton>
          <PermissionButton
            type="link"
            access={ADMIN_PERMISSIONS.systemRole.update}
            onClick={() => {
              setCurrentRow(record);
              setUpdateDrawerOpen(true);
            }}
          >
            编辑
          </PermissionButton>
          <Popconfirm
            title="确定要删除这个角色吗？"
            onConfirm={() => record.id && handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <PermissionButton type="link" danger access={ADMIN_PERMISSIONS.systemRole.delete}>
              删除
            </PermissionButton>
          </Popconfirm>
        </TableActionGroup>
      ),
    },
  ];

  return (
    <PageContainer>
      <ProTable<RoleListVo, RoleListQuery>
        headerTitle="角色列表"
        actionRef={actionRef}
        rowKey="id"
        search={{
          labelWidth: 120,
        }}
        toolBarRender={() => [
          <PermissionButton
            type="primary"
            key="primary"
            icon={<PlusOutlined />}
            access={ADMIN_PERMISSIONS.systemRole.add}
            onClick={() => {
              setCreateDrawerOpen(true);
            }}
          >
            新建
          </PermissionButton>,
        ]}
        request={async (params, _sort, _filter) => {
          try {
            const data = await listRole({
              ...params,
              pageNum: params.current,
              pageSize: params.pageSize,
            });

            return {
              data: data?.rows || [],
              success: true,
              total: Number(data?.total) || 0,
            };
          } catch {
            return {
              data: [],
              success: false,
              total: 0,
            };
          }
        }}
        columns={columns}
        scroll={{ x: 'max-content' }}
        rowSelection={{
          onChange: (_, _selectedRows) => {
            // 可以在这里处理选中的行
          },
        }}
        tableAlertRender={({ selectedRowKeys }) => (
          <Space size={24}>
            <span>已选 {selectedRowKeys.length} 项</span>
          </Space>
        )}
        tableAlertOptionRender={({ selectedRows }) => {
          return (
            <Space size={16}>
              <Popconfirm
                title="确定要删除选中的角色吗？"
                onConfirm={() => handleBatchDelete(selectedRows)}
                okText="确定"
                cancelText="取消"
              >
                <PermissionButton type="link" danger access={ADMIN_PERMISSIONS.systemRole.delete}>
                  批量删除
                </PermissionButton>
              </Popconfirm>
            </Space>
          );
        }}
      />

      {/* 新建角色表单 */}
      <RoleForm
        open={createDrawerOpen}
        onOpenChange={setCreateDrawerOpen}
        onFinish={async () => {
          setCreateDrawerOpen(false);
          actionRef.current?.reload();
        }}
      />

      {/* 编辑角色表单 */}
      <RoleForm
        open={updateDrawerOpen}
        onOpenChange={setUpdateDrawerOpen}
        initialValues={currentRow}
        onFinish={async () => {
          setUpdateDrawerOpen(false);
          setCurrentRow(undefined);
          actionRef.current?.reload();
        }}
      />
    </PageContainer>
  );
};

export default RoleManagement;
