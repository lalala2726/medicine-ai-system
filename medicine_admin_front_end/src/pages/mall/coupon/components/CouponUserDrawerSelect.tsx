import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Avatar, Button, Drawer, Space, Typography } from 'antd';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { userList, type UserTypes } from '@/api/system/user';

/**
 * 用户列表默认分页大小。
 */
const USER_TABLE_PAGE_SIZE = 10;

/**
 * 抽屉宽度。
 */
const USER_SELECTOR_DRAWER_WIDTH = 1200;

/**
 * 用户选择组件属性。
 */
interface CouponUserDrawerSelectProps {
  /** 当前选中的用户ID列表 */
  value?: number[];
  /** 当前选中的用户基础信息 */
  selectedUsers?: UserTypes.UserListVo[];
  /** 用户切换回调 */
  onChange?: (value?: number[]) => void;
  /** 用户基础信息切换回调 */
  onUsersChange?: (users: UserTypes.UserListVo[]) => void;
  /** 是否禁用 */
  disabled?: boolean;
}

/**
 * 规范化用户ID列表。
 * @param userIds 原始用户ID列表。
 * @returns 去重后的用户ID列表。
 */
function normalizeUserIds(userIds?: number[]): number[] {
  if (!userIds || userIds.length === 0) {
    return [];
  }
  return Array.from(
    new Set(
      userIds
        .map((userId) => Number(userId))
        .filter((userId) => Number.isInteger(userId) && userId > 0),
    ),
  );
}

/**
 * 构建用户映射。
 * @param users 用户列表。
 * @returns 用户映射。
 */
function buildUserMap(users?: UserTypes.UserListVo[]): Record<number, UserTypes.UserListVo> {
  const userMap: Record<number, UserTypes.UserListVo> = {};
  (users || []).forEach((user) => {
    const uid = Number(user.id);
    if (uid) {
      userMap[uid] = user;
    }
  });
  return userMap;
}

/**
 * 生成用户头像占位文本。
 * @param user 用户信息。
 * @returns 头像占位文本。
 */
function getAvatarFallbackText(user: UserTypes.UserListVo): string {
  return user.nickname?.trim()?.charAt(0) || user.username?.trim()?.charAt(0) || 'U';
}

/**
 * 优惠券用户多选抽屉组件。
 * @param props 组件属性。
 * @returns 用户选择节点。
 */
const CouponUserDrawerSelect: React.FC<CouponUserDrawerSelectProps> = ({
  value,
  selectedUsers,
  onChange,
  onUsersChange,
  disabled = false,
}) => {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [draftSelectedUserIds, setDraftSelectedUserIds] = useState<number[]>([]);
  const [draftSelectedUserMap, setDraftSelectedUserMap] = useState<
    Record<number, UserTypes.UserListVo>
  >({});
  const actionRef = useRef<ActionType | null>(null);

  /**
   * 当前已选用户ID列表。
   */
  const selectedUserIds = useMemo(() => normalizeUserIds(value), [value]);

  /**
   * 重置草稿选择状态。
   * @returns 无返回值。
   */
  const resetDraftSelection = useCallback(() => {
    setDraftSelectedUserIds(selectedUserIds);
    setDraftSelectedUserMap(buildUserMap(selectedUsers));
  }, [selectedUserIds, selectedUsers]);

  /**
   * 打开用户选择抽屉。
   * @returns 无返回值。
   */
  const handleOpenDrawer = useCallback(() => {
    resetDraftSelection();
    setDrawerOpen(true);
  }, [resetDraftSelection]);

  /**
   * 关闭用户选择抽屉。
   * @returns 无返回值。
   */
  const handleCloseDrawer = useCallback(() => {
    setDrawerOpen(false);
  }, []);

  /**
   * 同步草稿选择的用户ID列表。
   * @param keys 草稿用户ID列表。
   * @returns 无返回值。
   */
  const handleDraftSelectionChange = useCallback((keys: React.Key[]) => {
    setDraftSelectedUserIds(normalizeUserIds(keys.map((key) => Number(key))));
  }, []);

  /**
   * 处理单行勾选变化。
   * @param record 当前用户信息。
   * @param selected 当前是否选中。
   * @returns 无返回值。
   */
  const handleDraftSelect = useCallback((record: UserTypes.UserListVo, selected: boolean) => {
    const uid = Number(record.id);
    if (!uid) return;
    setDraftSelectedUserMap((previousMap) => {
      const nextMap = { ...previousMap };
      if (selected) {
        nextMap[uid] = record;
      } else {
        delete nextMap[uid];
      }
      return nextMap;
    });
  }, []);

  /**
   * 处理当前页全选变化。
   * @param selected 当前页是否全选。
   * @param changeRows 本次变更的用户列表。
   * @returns 无返回值。
   */
  const handleDraftSelectAll = useCallback(
    (selected: boolean, changeRows: UserTypes.UserListVo[]) => {
      setDraftSelectedUserMap((previousMap) => {
        const nextMap = { ...previousMap };
        changeRows.forEach((user) => {
          const uid = Number(user.id);
          if (!uid) return;
          if (selected) {
            nextMap[uid] = user;
          } else {
            delete nextMap[uid];
          }
        });
        return nextMap;
      });
    },
    [],
  );

  /**
   * 确认当前草稿选择。
   * @returns 无返回值。
   */
  const handleConfirmSelection = useCallback(() => {
    const normalizedUsers = draftSelectedUserIds
      .map((userId) => draftSelectedUserMap[userId])
      .filter((user): user is UserTypes.UserListVo => Boolean(user));
    onChange?.(draftSelectedUserIds);
    onUsersChange?.(normalizedUsers);
    setDrawerOpen(false);
  }, [draftSelectedUserIds, draftSelectedUserMap, onChange, onUsersChange]);

  /**
   * 用户表格列定义。
   */
  const userColumns: ProColumns<UserTypes.UserListVo>[] = useMemo(
    () => [
      {
        title: '头像',
        dataIndex: 'avatar',
        width: 80,
        hideInSearch: true,
        render: (_, record) => (
          <Avatar src={record.avatar} size={40}>
            {getAvatarFallbackText(record)}
          </Avatar>
        ),
      },
      {
        title: '用户ID',
        dataIndex: 'id',
        width: 100,
        valueType: 'digit',
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
    ],
    [],
  );

  useEffect(() => {
    if (drawerOpen) {
      return;
    }
    resetDraftSelection();
  }, [drawerOpen, resetDraftSelection]);

  return (
    <>
      <Space>
        <Button disabled={disabled} onClick={handleOpenDrawer}>
          {selectedUserIds.length > 0 ? '重新选择用户' : '选择用户'}
        </Button>
        <Typography.Text type="secondary">已选 {selectedUserIds.length} 人</Typography.Text>
      </Space>

      <Drawer
        title="选择用户"
        placement="right"
        width={USER_SELECTOR_DRAWER_WIDTH}
        open={drawerOpen}
        destroyOnClose
        onClose={handleCloseDrawer}
        footer={
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <Typography.Text>已选 {draftSelectedUserIds.length} 人</Typography.Text>
            <Space>
              <Button onClick={handleCloseDrawer}>取消</Button>
              <Button type="primary" onClick={handleConfirmSelection}>
                确认选择
              </Button>
            </Space>
          </div>
        }
      >
        <ProTable<UserTypes.UserListVo, UserTypes.UserListQueryRequest>
          rowKey={(record) => Number(record.id)}
          actionRef={actionRef}
          headerTitle="用户列表"
          search={{ labelWidth: 96 }}
          columns={userColumns}
          request={async (params) => {
            const { current, pageSize, ...rest } = params;
            const result = await userList({
              ...(rest as UserTypes.UserListQueryRequest),
              pageNum: Number(current ?? 1),
              pageSize: Number(pageSize ?? USER_TABLE_PAGE_SIZE),
            });
            setDraftSelectedUserMap((previousMap) => {
              const nextMap = { ...previousMap };
              (result?.rows || []).forEach((user) => {
                const uid = Number(user.id);
                if (uid && draftSelectedUserIds.includes(uid)) {
                  nextMap[uid] = user;
                }
              });
              return nextMap;
            });
            return {
              data: result?.rows || [],
              success: true,
              total: Number(result?.total || 0),
            };
          }}
          pagination={{
            showQuickJumper: true,
            showSizeChanger: true,
            defaultPageSize: USER_TABLE_PAGE_SIZE,
          }}
          rowSelection={{
            preserveSelectedRowKeys: true,
            selectedRowKeys: draftSelectedUserIds,
            onChange: (keys) => {
              handleDraftSelectionChange(keys);
            },
            onSelect: (record, selected) => {
              handleDraftSelect(record, selected);
            },
            onSelectAll: (selected, _selectedRows, changeRows) => {
              handleDraftSelectAll(selected, changeRows);
            },
          }}
        />
      </Drawer>
    </>
  );
};

export default CouponUserDrawerSelect;
