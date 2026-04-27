import { Button, Empty, message, Modal, Spin, Table, Tag, Tooltip } from 'antd';
import React, { useEffect, useState } from 'react';
import { deleteUserCoupon, getUserCoupons, type UserTypes } from '@/api/system/user';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import type { TableDataResult } from '@/types';

interface UserCouponTabProps {
  /** 用户ID。 */
  userId: number;
  /** 是否展示当前标签页。 */
  visible: boolean;
  /** 表格尺寸。 */
  tableSize?: 'small' | 'middle' | 'large';
}

const STATUS_COLOR_MAP: Record<string, string> = {
  AVAILABLE: 'success',
  LOCKED: 'warning',
  USED: 'default',
  EXPIRED: 'error',
};

const STATUS_TEXT_MAP: Record<string, string> = {
  AVAILABLE: '可使用',
  LOCKED: '已锁定',
  USED: '已使用',
  EXPIRED: '已过期',
};

const formatCurrency = (value?: string) => {
  if (!value || value === '') {
    return '-';
  }
  return `¥${Number(value).toFixed(2)}`;
};

const UserCouponTab: React.FC<UserCouponTabProps> = ({ userId, visible, tableSize = 'small' }) => {
  const [couponData, setCouponData] = useState<TableDataResult<UserTypes.UserCouponVo> | null>(
    null,
  );
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
  });
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    if (visible && userId) {
      void fetchUserCouponList();
    }
  }, [visible, userId]);

  const fetchUserCouponList = async (pageNum = 1, pageSize = 10) => {
    if (!userId) {
      return;
    }

    try {
      setLoading(true);
      const result = await getUserCoupons(userId, { pageNum, pageSize });
      setCouponData(result);
      setPagination({ current: pageNum, pageSize });
    } catch (error) {
      console.error('获取用户优惠券失败:', error);
      messageApi.error('获取用户优惠券失败');
      setCouponData(null);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = (record: UserTypes.UserCouponVo) => {
    if (!record.couponId) {
      messageApi.warning('缺少优惠券ID，无法删除');
      return;
    }
    const couponId = record.couponId;
    if (record.couponStatus === 'LOCKED') {
      messageApi.warning('已锁定优惠券不允许删除');
      return;
    }

    Modal.confirm({
      title: '删除用户优惠券',
      content: `确定要删除优惠券「${record.couponName || record.couponId}」吗？`,
      okText: '确定删除',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        try {
          await deleteUserCoupon(userId, couponId);
          messageApi.success('删除用户优惠券成功');
          await fetchUserCouponList(pagination.current, pagination.pageSize);
        } catch (error) {
          console.error('删除用户优惠券失败:', error);
          messageApi.error('删除用户优惠券失败');
        }
      },
    });
  };

  const handleShowDetail = (record: UserTypes.UserCouponVo) => {
    Modal.info({
      title: '优惠券详情',
      width: 720,
      content: (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }}>
          <div>用户券ID：{record.couponId ?? '-'}</div>
          <div>模板ID：{record.templateId ?? '-'}</div>
          <div>优惠券名称：{record.couponName || '-'}</div>
          <div>
            状态：{STATUS_TEXT_MAP[record.couponStatus || ''] || record.couponStatus || '-'}
          </div>
          <div>门槛金额：{formatCurrency(record.thresholdAmount)}</div>
          <div>总金额：{formatCurrency(record.totalAmount)}</div>
          <div>可用金额：{formatCurrency(record.availableAmount)}</div>
          <div>锁定金额：{formatCurrency(record.lockedConsumeAmount)}</div>
          <div>是否允许继续使用：{record.continueUseEnabled === 1 ? '允许' : '不允许'}</div>
          <div>来源类型：{record.sourceType || '-'}</div>
          <div>生效时间：{record.effectiveTime || '-'}</div>
          <div>失效时间：{record.expireTime || '-'}</div>
          <div>锁定订单号：{record.lockOrderNo || '-'}</div>
          <div>锁定时间：{record.lockTime || '-'}</div>
          <div>创建时间：{record.createTime || '-'}</div>
        </div>
      ),
    });
  };

  const columns = [
    {
      title: '优惠券名称',
      dataIndex: 'couponName',
      key: 'couponName',
      width: 200,
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'couponStatus',
      key: 'couponStatus',
      width: 110,
      render: (status: string) => (
        <Tag color={STATUS_COLOR_MAP[status] || 'default'}>
          {STATUS_TEXT_MAP[status] || status || '未知'}
        </Tag>
      ),
    },
    {
      title: '可用金额',
      dataIndex: 'availableAmount',
      key: 'availableAmount',
      width: 120,
      render: (value: string) => formatCurrency(value),
    },
    {
      title: '使用门槛',
      dataIndex: 'thresholdAmount',
      key: 'thresholdAmount',
      width: 120,
      render: (value: string) => formatCurrency(value),
    },
    {
      title: '失效时间',
      dataIndex: 'expireTime',
      key: 'expireTime',
      width: 170,
      render: (value: string) => value || '-',
    },
    {
      title: '操作',
      key: 'option',
      width: 140,
      fixed: 'right' as const,
      align: 'center' as const,
      render: (_: unknown, record: UserTypes.UserCouponVo) => {
        const deleteButton = (
          <PermissionButton
            type="link"
            danger
            access={ADMIN_PERMISSIONS.systemUser.update}
            disabled={record.couponStatus === 'LOCKED'}
            onClick={() => handleDelete(record)}
          >
            删除
          </PermissionButton>
        );
        return (
          <TableActionGroup>
            <Button type="link" onClick={() => handleShowDetail(record)}>
              详情
            </Button>
            {record.couponStatus === 'LOCKED' ? (
              <Tooltip title="已锁定优惠券不允许删除">{deleteButton}</Tooltip>
            ) : (
              deleteButton
            )}
          </TableActionGroup>
        );
      },
    },
  ];

  return (
    <>
      {contextHolder}
      <Spin spinning={loading}>
        {couponData?.rows && couponData.rows.length > 0 ? (
          <Table
            columns={columns}
            dataSource={couponData.rows}
            rowKey="couponId"
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: couponData.total,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条记录`,
              onChange: (page, size) => fetchUserCouponList(page, size),
              onShowSizeChange: (_current, size) => fetchUserCouponList(1, size),
            }}
            size={tableSize}
            scroll={{ x: 'max-content' }}
          />
        ) : (
          <Empty description="暂无优惠券" />
        )}
      </Spin>
    </>
  );
};

export default UserCouponTab;
