import { PlusOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { message, Popconfirm, Space, Tag } from 'antd';
import React, { useRef, useState } from 'react';
import { deletePermission, listPermission, type PermissionTreeVo } from '@/api/system/permission';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import PermissionForm from './components/PermissionForm';

const PermissionManagement: React.FC = () => {
  const actionRef = useRef<ActionType>(null);
  const [createModalOpen, setCreateModalOpen] = useState<boolean>(false);
  const [updateModalOpen, setUpdateModalOpen] = useState<boolean>(false);
  const [currentRow, setCurrentRow] = useState<PermissionTreeVo>();

  /**
   * 删除权限
   */
  const handleDelete = async (id: string) => {
    const hide = message.loading('正在删除');
    try {
      await deletePermission(id);
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
   * 批量删除权限
   */
  const handleBatchDelete = async (selectedRows: PermissionTreeVo[]) => {
    const hide = message.loading('正在删除');
    if (!selectedRows || selectedRows.length === 0) return true;
    try {
      const ids = selectedRows.map((row) => row.id).filter((id): id is string => !!id);
      await deletePermission(ids);
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

  const columns: ProColumns<PermissionTreeVo>[] = [
    {
      title: '权限标识',
      dataIndex: 'permissionCode',
      copyable: true,
      ellipsis: true,
    },
    {
      title: '权限名称',
      dataIndex: 'permissionName',
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
      title: '备注',
      dataIndex: 'remark',
      ellipsis: true,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      valueType: 'dateTime',
    },
    {
      title: '操作',
      dataIndex: 'option',
      valueType: 'option',
      fixed: 'right',
      width: 180,
      align: 'center',
      render: (_, record) => (
        <TableActionGroup>
          <PermissionButton
            type="link"
            access={ADMIN_PERMISSIONS.systemPermission.add}
            onClick={() => {
              setCurrentRow({ parentId: record.id } as PermissionTreeVo);
              setCreateModalOpen(true);
            }}
          >
            添加
          </PermissionButton>
          <PermissionButton
            type="link"
            access={ADMIN_PERMISSIONS.systemPermission.update}
            onClick={() => {
              setCurrentRow(record);
              setUpdateModalOpen(true);
            }}
          >
            编辑
          </PermissionButton>
          <Popconfirm
            title="确定要删除这个权限吗？"
            onConfirm={() => record.id && handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <PermissionButton type="link" danger access={ADMIN_PERMISSIONS.systemPermission.delete}>
              删除
            </PermissionButton>
          </Popconfirm>
        </TableActionGroup>
      ),
    },
  ];

  return (
    <PageContainer>
      <ProTable<PermissionTreeVo>
        headerTitle="权限列表"
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
            access={ADMIN_PERMISSIONS.systemPermission.add}
            onClick={() => {
              setCreateModalOpen(true);
            }}
          >
            新建
          </PermissionButton>,
        ]}
        request={async (params) => {
          try {
            // 获取权限树
            let data = await listPermission();

            // 如果有搜索条件，进行过滤
            if (params.permissionCode || params.permissionName || params.status !== undefined) {
              const filterTree = (items: PermissionTreeVo[]): PermissionTreeVo[] => {
                return items.reduce((acc: PermissionTreeVo[], item) => {
                  let match = true;

                  if (
                    params.permissionCode &&
                    !item.permissionCode?.includes(params.permissionCode)
                  ) {
                    match = false;
                  }
                  if (
                    params.permissionName &&
                    !item.permissionName?.includes(params.permissionName)
                  ) {
                    match = false;
                  }
                  if (params.status !== undefined && item.status !== params.status) {
                    match = false;
                  }

                  const filteredChildren = item.children ? filterTree(item.children) : [];

                  if (match || filteredChildren.length > 0) {
                    acc.push({
                      ...item,
                      children: filteredChildren.length > 0 ? filteredChildren : item.children,
                    });
                  }

                  return acc;
                }, []);
              };

              data = filterTree(data);
            }

            return {
              data: data,
              success: true,
            };
          } catch {
            return {
              data: [],
              success: false,
            };
          }
        }}
        columns={columns}
        pagination={false}
        expandable={{
          defaultExpandAllRows: true,
        }}
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
                title="确定要删除选中的权限吗？"
                onConfirm={() => handleBatchDelete(selectedRows)}
                okText="确定"
                cancelText="取消"
              >
                <PermissionButton
                  type="link"
                  danger
                  access={ADMIN_PERMISSIONS.systemPermission.delete}
                >
                  批量删除
                </PermissionButton>
              </Popconfirm>
            </Space>
          );
        }}
      />

      {/* 新建权限表单 */}
      <PermissionForm
        open={createModalOpen}
        onOpenChange={(open) => {
          setCreateModalOpen(open);
          if (!open) {
            setCurrentRow(undefined);
          }
        }}
        initialValues={currentRow}
        onFinish={async () => {
          setCreateModalOpen(false);
          setCurrentRow(undefined);
          actionRef.current?.reload();
        }}
      />

      {/* 编辑权限表单 */}
      <PermissionForm
        open={updateModalOpen}
        onOpenChange={setUpdateModalOpen}
        initialValues={currentRow}
        onFinish={async () => {
          setUpdateModalOpen(false);
          setCurrentRow(undefined);
          actionRef.current?.reload();
        }}
      />
    </PageContainer>
  );
};

export default PermissionManagement;
