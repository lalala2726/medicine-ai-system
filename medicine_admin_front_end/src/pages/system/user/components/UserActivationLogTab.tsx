import { Button, Empty, Modal, Spin, Table, Tag } from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { getUserActivationLogs, type UserTypes } from '@/api/system/user';
import TableActionGroup from '@/components/TableActionGroup';
import type { TableDataResult } from '@/types';

interface UserActivationLogTabProps {
  /** 用户ID。 */
  userId: number;
  /** 是否展示当前标签页。 */
  visible: boolean;
  /** 表格尺寸。 */
  tableSize?: 'small' | 'middle' | 'large';
}

/** 结果状态文本映射。 */
const ACTIVATION_RESULT_TEXT_MAP: Record<string, string> = {
  SUCCESS: '成功',
  FAIL: '失败',
};

/** 结果状态颜色映射。 */
const ACTIVATION_RESULT_COLOR_MAP: Record<string, string> = {
  SUCCESS: 'success',
  FAIL: 'error',
};

/**
 * 用户激活码日志标签页。
 * @param props 组件属性。
 * @returns 标签页节点。
 */
const UserActivationLogTab: React.FC<UserActivationLogTabProps> = ({
  userId,
  visible,
  tableSize = 'small',
}) => {
  const [logData, setLogData] = useState<TableDataResult<UserTypes.ActivationLogVo> | null>(null);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
  });

  /**
   * 查询用户激活码日志列表。
   * @param pageNum 页码。
   * @param pageSize 每页大小。
   * @returns 无返回值。
   */
  const fetchActivationLogList = useCallback(
    async (pageNum = 1, pageSize = 10) => {
      if (!userId) {
        return;
      }
      try {
        setLoading(true);
        const result = await getUserActivationLogs(userId, { pageNum, pageSize });
        setLogData(result);
        setPagination({ current: pageNum, pageSize });
      } finally {
        setLoading(false);
      }
    },
    [userId],
  );

  useEffect(() => {
    if (visible && userId) {
      void fetchActivationLogList();
    }
  }, [fetchActivationLogList, userId, visible]);

  /**
   * 查看激活码日志详情。
   * @param record 激活码日志记录。
   * @returns 无返回值。
   */
  const handleShowDetail = (record: UserTypes.ActivationLogVo) => {
    Modal.info({
      title: '激活码记录详情',
      width: 720,
      content: (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }}>
          <div>日志ID：{record.id ?? '-'}</div>
          <div>批次ID：{record.batchId ?? '-'}</div>
          <div>激活码ID：{record.activationCodeId ?? '-'}</div>
          <div>批次号：{record.batchNo || '-'}</div>
          <div>激活码：{record.plainCodeSnapshot || '-'}</div>
          <div>模板名称：{record.templateName || '-'}</div>
          <div>用户券ID：{record.couponId ?? '-'}</div>
          <div>
            结果状态：
            {ACTIVATION_RESULT_TEXT_MAP[record.resultStatus || ''] || record.resultStatus || '-'}
          </div>
          <div>发券方式：{record.grantMode || '-'}</div>
          <div>客户端IP：{record.clientIp || '-'}</div>
          <div>创建时间：{record.createTime || '-'}</div>
          <div style={{ gridColumn: '1 / span 2' }}>失败编码：{record.failCode || '-'}</div>
          <div style={{ gridColumn: '1 / span 2' }}>失败原因：{record.failMessage || '-'}</div>
        </div>
      ),
    });
  };

  /**
   * 表格列定义。
   */
  const columns = [
    {
      title: '结果状态',
      dataIndex: 'resultStatus',
      key: 'resultStatus',
      width: 110,
      render: (value: string) => (
        <Tag color={ACTIVATION_RESULT_COLOR_MAP[value] || 'default'}>
          {ACTIVATION_RESULT_TEXT_MAP[value] || value || '-'}
        </Tag>
      ),
    },
    {
      title: '批次号',
      dataIndex: 'batchNo',
      key: 'batchNo',
      width: 180,
      render: (value: string) => value || '-',
    },
    {
      title: '激活码',
      dataIndex: 'plainCodeSnapshot',
      key: 'plainCodeSnapshot',
      width: 160,
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
      title: '操作',
      key: 'option',
      width: 90,
      fixed: 'right' as const,
      align: 'center' as const,
      render: (_: unknown, record: UserTypes.ActivationLogVo) => (
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
            onChange: (page, size) => fetchActivationLogList(page, size),
            onShowSizeChange: (_current, size) => fetchActivationLogList(1, size),
          }}
          size={tableSize}
          scroll={{ x: 'max-content' }}
        />
      ) : (
        <Empty description="暂无激活码记录" />
      )}
    </Spin>
  );
};

export default UserActivationLogTab;
