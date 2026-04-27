import { Button, Empty, Modal, Spin, Table, Tag } from 'antd';
import React, { useEffect, useState } from 'react';
import { getUserCouponLogs, type UserTypes } from '@/api/system/user';
import TableActionGroup from '@/components/TableActionGroup';
import type { TableDataResult } from '@/types';

interface UserCouponLogTabProps {
  /** 用户ID。 */
  userId: number;
  /** 是否展示当前标签页。 */
  visible: boolean;
  /** 表格尺寸。 */
  tableSize?: 'small' | 'middle' | 'large';
}

const CHANGE_TYPE_TEXT_MAP: Record<string, string> = {
  GRANT: '发券',
  LOCK: '锁券',
  CONSUME: '消耗',
  RELEASE: '释放锁券',
  RETURN: '返还',
  EXPIRE: '过期',
  MANUAL_ADJUST: '手工调整',
};

const CHANGE_TYPE_COLOR_MAP: Record<string, string> = {
  GRANT: 'success',
  LOCK: 'processing',
  CONSUME: 'error',
  RELEASE: 'warning',
  RETURN: 'cyan',
  EXPIRE: 'default',
  MANUAL_ADJUST: 'purple',
};

const formatCurrency = (value?: string) => {
  if (!value || value === '') {
    return '-';
  }
  return `¥${Number(value).toFixed(2)}`;
};

const UserCouponLogTab: React.FC<UserCouponLogTabProps> = ({
  userId,
  visible,
  tableSize = 'small',
}) => {
  const [logData, setLogData] = useState<TableDataResult<UserTypes.CouponLogVo> | null>(null);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
  });

  useEffect(() => {
    if (visible && userId) {
      void fetchCouponLogList();
    }
  }, [visible, userId]);

  const fetchCouponLogList = async (pageNum = 1, pageSize = 10) => {
    if (!userId) {
      return;
    }
    try {
      setLoading(true);
      const result = await getUserCouponLogs(userId, { pageNum, pageSize });
      setLogData(result);
      setPagination({ current: pageNum, pageSize });
    } finally {
      setLoading(false);
    }
  };

  const handleShowDetail = (record: UserTypes.CouponLogVo) => {
    Modal.info({
      title: '优惠券记录详情',
      width: 720,
      content: (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }}>
          <div>日志ID：{record.id ?? '-'}</div>
          <div>用户券ID：{record.couponId ?? '-'}</div>
          <div>优惠券名称：{record.couponName || '-'}</div>
          <div>用户ID：{record.userId ?? '-'}</div>
          <div>订单号：{record.orderNo || '-'}</div>
          <div>
            变更类型：{CHANGE_TYPE_TEXT_MAP[record.changeType || ''] || record.changeType || '-'}
          </div>
          <div>变更金额：{formatCurrency(record.changeAmount)}</div>
          <div>抵扣金额：{formatCurrency(record.deductAmount)}</div>
          <div>浪费金额：{formatCurrency(record.wasteAmount)}</div>
          <div>变更前可用金额：{formatCurrency(record.beforeAvailableAmount)}</div>
          <div>变更后可用金额：{formatCurrency(record.afterAvailableAmount)}</div>
          <div>来源类型：{record.sourceType || '-'}</div>
          <div>来源业务号：{record.sourceBizNo || '-'}</div>
          <div>操作人：{record.operatorId || '-'}</div>
          <div>创建时间：{record.createTime || '-'}</div>
          <div style={{ gridColumn: '1 / span 2' }}>备注：{record.remark || '-'}</div>
        </div>
      ),
    });
  };

  const columns = [
    {
      title: '变更类型',
      dataIndex: 'changeType',
      key: 'changeType',
      width: 120,
      render: (value: string) => (
        <Tag color={CHANGE_TYPE_COLOR_MAP[value] || 'default'}>
          {CHANGE_TYPE_TEXT_MAP[value] || value || '未知'}
        </Tag>
      ),
    },
    {
      title: '变更金额',
      dataIndex: 'changeAmount',
      key: 'changeAmount',
      width: 120,
      render: (value: string) => formatCurrency(value),
    },
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 180,
      render: (value: string) => value || '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (value: string) => value || '-',
    },
    {
      title: '操作人',
      dataIndex: 'operatorId',
      key: 'operatorId',
      width: 120,
      render: (value: string) => value || '-',
    },
    {
      title: '操作',
      key: 'option',
      width: 90,
      fixed: 'right' as const,
      align: 'center' as const,
      render: (_: unknown, record: UserTypes.CouponLogVo) => (
        <TableActionGroup>
          <Button type="link" onClick={() => handleShowDetail(record)}>
            详情
          </Button>
        </TableActionGroup>
      ),
    },
  ];

  return (
    <Spin spinning={loading}>
      {logData?.rows && logData.rows.length > 0 ? (
        <Table
          columns={columns}
          dataSource={logData.rows}
          rowKey="id"
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: logData.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条记录`,
            onChange: (page, size) => fetchCouponLogList(page, size),
            onShowSizeChange: (_current, size) => fetchCouponLogList(1, size),
          }}
          size={tableSize}
          scroll={{ x: 'max-content' }}
        />
      ) : (
        <Empty description="暂无优惠券记录" />
      )}
    </Spin>
  );
};

export default UserCouponLogTab;
