import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { useRequest } from 'ahooks';
import type { MenuProps } from 'antd';
import { Dropdown, Image, Modal, message, theme } from 'antd';
import React, { useCallback, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { roleOption } from '@/api/system/role';
import { deleteUser, type UserTypes, userList } from '@/api/system/user';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { usePermission } from '@/hooks/usePermission';
import { buildSystemUserAssetPath } from '@/router/paths';
import UserAddModal from './components/UserAddModal';
import UserDetail from './components/UserDetail';
import UserWalletModal from './components/UserWalletModal';

/**
 * 用户表格操作列宽度。
 */
const USER_TABLE_ACTION_COLUMN_WIDTH = 190;

/**
 * 用户列表行操作回调集合。
 */
interface UserTableActionHandlers {
  /** 打开钱包抽屉回调。 */
  onOpenWallet: (record: UserTypes.UserListVo) => void;
  /** 打开资产明细页面回调。 */
  onOpenAssets: (record: UserTypes.UserListVo) => void;
  /** 删除用户回调。 */
  onDelete: (record: UserTypes.UserListVo) => void;
}

/**
 * 用户表格操作权限集合。
 */
interface UserTableActionPermissions {
  /** 是否允许查询用户。 */
  canQuery: boolean;
  /** 是否允许删除用户。 */
  canDelete: boolean;
}

/**
 * 构建用户列表更多操作菜单。
 * @param record 当前用户行数据。
 * @param handlers 用户行操作回调集合。
 * @param permissions 用户行操作权限集合。
 * @returns 更多操作菜单项。
 */
function buildUserMoreMenuItems(
  record: UserTypes.UserListVo,
  handlers: UserTableActionHandlers,
  permissions: UserTableActionPermissions,
): MenuProps['items'] {
  return [
    {
      key: 'wallet',
      label: '钱包',
      disabled: !permissions.canQuery,
      onClick: () => handlers.onOpenWallet(record),
    },
    {
      key: 'assets',
      label: '资产',
      disabled: !permissions.canQuery,
      onClick: () => handlers.onOpenAssets(record),
    },
    {
      type: 'divider',
    },
    {
      key: 'delete',
      label: '删除',
      danger: true,
      disabled: !permissions.canDelete,
      onClick: () => handlers.onDelete(record),
    },
  ];
}

/**
 * 用户管理页面。
 * @returns 用户管理页面节点。
 */
const UserManagement: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const { canAccess } = usePermission();
  const actionRef = useRef<ActionType | null>(null);
  const [selectedRowsState, setSelectedRows] = useState<UserTypes.UserListVo[]>([]);
  const [detailVisible, setDetailVisible] = useState<boolean>(false);
  const [walletVisible, setWalletVisible] = useState<boolean>(false);
  const [addModalVisible, setAddModalVisible] = useState<boolean>(false);
  const [currentUserRecord, setCurrentUserRecord] = useState<UserTypes.UserListVo | null>(null);
  const [detailAutoEdit, setDetailAutoEdit] = useState<boolean>(false);

  const [messageApi, contextHolder] = message.useMessage();

  const { runAsync: deleteRunAsync, loading: deleteLoading } = useRequest(deleteUser, {
    manual: true,
    onSuccess: () => {
      messageApi.success('删除成功');
      setSelectedRows([]);
      actionRef.current?.reload();
    },
  });
  /**
   * 用户角色筛选下拉选项。
   */
  const { data: roleOptions = [], loading: roleOptionsLoading } = useRequest(roleOption);

  /**
   * 打开用户详情抽屉。
   * @param record 当前用户行数据。
   * @returns 无返回值。
   */
  const handleOpenDetail = useCallback((record: UserTypes.UserListVo) => {
    setCurrentUserRecord(record);
    setDetailAutoEdit(false);
    setDetailVisible(true);
  }, []);

  /**
   * 打开用户编辑抽屉。
   * @param record 当前用户行数据。
   * @returns 无返回值。
   */
  const handleOpenEdit = useCallback((record: UserTypes.UserListVo) => {
    setCurrentUserRecord(record);
    setDetailAutoEdit(true);
    setDetailVisible(true);
  }, []);

  /**
   * 打开用户钱包抽屉。
   * @param record 当前用户行数据。
   * @returns 无返回值。
   */
  const handleOpenWallet = useCallback((record: UserTypes.UserListVo) => {
    setCurrentUserRecord(record);
    setWalletVisible(true);
  }, []);

  /**
   * 打开用户资产明细页面。
   * @param record 当前用户行数据。
   * @returns 无返回值。
   */
  const handleOpenAssets = useCallback(
    (record: UserTypes.UserListVo) => {
      navigate(buildSystemUserAssetPath(record.id as number));
    },
    [navigate],
  );

  /**
   * 关闭用户详情抽屉。
   * @returns 无返回值。
   */
  const handleCloseDetail = useCallback(() => {
    setDetailVisible(false);
    setDetailAutoEdit(false);
  }, []);

  /**
   * 打开新增用户弹窗。
   * @returns 无返回值。
   */
  const handleAdd = useCallback(() => {
    setAddModalVisible(true);
  }, []);

  /**
   * 删除选中的用户。
   * @param selectedRows 待删除的用户行数据。
   * @returns 无返回值。
   */
  const handleRemove = useCallback(
    (selectedRows: UserTypes.UserListVo[]) => {
      if (!selectedRows?.length) {
        messageApi.warning('请选择要删除的用户');
        return;
      }
      Modal.confirm({
        title: '确认删除',
        content: `确定要删除选中的 ${selectedRows.length} 个用户吗？`,
        onOk: () => {
          const ids = selectedRows.map((row) => row.id as number);
          return deleteRunAsync(ids);
        },
      });
    },
    [deleteRunAsync, messageApi],
  );

  /**
   * 用户列表列配置。
   */
  const columns: ProColumns<UserTypes.UserListVo>[] = useMemo(
    () => [
      {
        title: '用户头像',
        dataIndex: 'avatar',
        hideInSearch: true,
        render: (_, record) => {
          const src = record.avatar;
          return src ? (
            <Image
              width={40}
              height={40}
              src={src}
              alt="用户头像"
              style={{ objectFit: 'cover', borderRadius: '4px' }}
              preview={{
                mask: '预览',
              }}
            />
          ) : (
            <div
              style={{
                width: 40,
                height: 40,
                background: token.colorBgLayout,
                borderRadius: '4px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: token.colorTextTertiary,
                fontSize: 12,
              }}
            >
              暂无
            </div>
          );
        },
      },
      {
        title: '用户名',
        dataIndex: 'username',
        ellipsis: true,
      },
      {
        title: '昵称',
        dataIndex: 'nickname',
        ellipsis: true,
      },
      {
        title: '真实姓名',
        dataIndex: 'realName',
        ellipsis: true,
      },
      {
        title: '手机号',
        dataIndex: 'phoneNumber',
        ellipsis: true,
      },
      {
        title: '邮箱',
        dataIndex: 'email',
        ellipsis: true,
      },
      {
        title: '角色',
        dataIndex: 'roles',
        ellipsis: true,
        hideInSearch: true,
      },
      {
        title: '角色',
        dataIndex: 'roleId',
        valueType: 'select',
        hideInTable: true,
        fieldProps: {
          allowClear: true,
          loading: roleOptionsLoading,
          optionFilterProp: 'label',
          options: roleOptions,
          showSearch: true,
        },
      },
      {
        title: '身份证',
        dataIndex: 'idCard',
        hideInTable: true,
      },
      {
        title: '状态',
        dataIndex: 'status',
        align: 'center',
        valueType: 'select',
        valueEnum: {
          0: {
            text: '启用',
            status: 'Success',
          },
          1: {
            text: '禁用',
            status: 'Error',
          },
        },
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        valueType: 'dateTime',
        hideInSearch: true,
      },
      {
        width: USER_TABLE_ACTION_COLUMN_WIDTH,
        title: '操作',
        dataIndex: 'option',
        valueType: 'option',
        fixed: 'right',
        align: 'center',
        render: (_, record) => (
          <TableActionGroup>
            <PermissionButton
              type="link"
              access={ADMIN_PERMISSIONS.systemUser.query}
              onClick={() => handleOpenDetail(record)}
            >
              详情
            </PermissionButton>
            <PermissionButton
              type="link"
              access={ADMIN_PERMISSIONS.systemUser.update}
              onClick={() => handleOpenEdit(record)}
            >
              编辑
            </PermissionButton>
            <Dropdown
              menu={{
                items: buildUserMoreMenuItems(
                  record,
                  {
                    onOpenWallet: handleOpenWallet,
                    onOpenAssets: handleOpenAssets,
                    onDelete: (currentRecord) => handleRemove([currentRecord]),
                  },
                  {
                    canQuery: canAccess(ADMIN_PERMISSIONS.systemUser.query),
                    canDelete: canAccess(ADMIN_PERMISSIONS.systemUser.delete),
                  },
                ),
              }}
              trigger={['click']}
            >
              <PermissionButton
                type="link"
                access={[ADMIN_PERMISSIONS.systemUser.query, ADMIN_PERMISSIONS.systemUser.delete]}
              >
                更多
              </PermissionButton>
            </Dropdown>
          </TableActionGroup>
        ),
      },
    ],
    [
      handleOpenAssets,
      handleOpenDetail,
      handleOpenEdit,
      handleOpenWallet,
      handleRemove,
      canAccess,
      roleOptions,
      roleOptionsLoading,
      token.colorBgLayout,
      token.colorTextTertiary,
    ],
  );

  return (
    <PageContainer>
      {contextHolder}
      <ProTable<UserTypes.UserListVo, UserTypes.UserListQueryRequest>
        headerTitle={'用户管理'}
        actionRef={actionRef}
        rowKey="id"
        search={{
          labelWidth: 120,
        }}
        toolBarRender={() => [
          <PermissionButton
            key="batchDelete"
            loading={deleteLoading}
            access={ADMIN_PERMISSIONS.systemUser.delete}
            disabled={selectedRowsState.length === 0}
            onClick={() => {
              handleRemove(selectedRowsState);
            }}
            danger
          >
            批量删除 ({selectedRowsState.length})
          </PermissionButton>,
          <PermissionButton
            key="add"
            type="primary"
            access={ADMIN_PERMISSIONS.systemUser.add}
            onClick={handleAdd}
          >
            添加用户
          </PermissionButton>,
        ]}
        request={async (params) => {
          const result = await userList(params);
          return {
            data: result?.rows || [],
            success: true,
            total: result?.total || 0,
          };
        }}
        columns={columns}
        rowSelection={{
          onChange: (_, selectedRows) => {
            setSelectedRows(selectedRows);
          },
        }}
        tableAlertRender={false}
        tableAlertOptionRender={false}
        scroll={{ x: 'max-content' }}
        pagination={{ pageSize: 10 }}
        size="middle"
      />

      <UserDetail
        open={detailVisible}
        onClose={handleCloseDetail}
        userId={currentUserRecord?.id ?? null}
        username={currentUserRecord?.username}
        autoEdit={detailAutoEdit}
        onUpdateSuccess={() => {
          actionRef.current?.reload();
        }}
      />

      <UserWalletModal
        open={walletVisible}
        onClose={() => setWalletVisible(false)}
        userId={currentUserRecord?.id ?? null}
      />

      <UserAddModal
        visible={addModalVisible}
        onCancel={() => setAddModalVisible(false)}
        onSuccess={() => {
          setAddModalVisible(false);
          // 刷新列表
          actionRef.current?.reload();
        }}
      />
    </PageContainer>
  );
};

export default UserManagement;
