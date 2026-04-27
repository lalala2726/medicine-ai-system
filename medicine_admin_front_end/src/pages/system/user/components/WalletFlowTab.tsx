import { Empty, message, Spin, Table, Tag, Typography } from 'antd';
import React, { useEffect, useState } from 'react';
import { getUserWalletFlow, type UserTypes } from '@/api/system/user';
import type { TableDataResult } from '@/types';

const { Text } = Typography;

/**
 * 钱包流水表格属性。
 */
interface WalletFlowTabProps {
  /** 用户ID。 */
  userId: number;
  /** 是否展示当前标签页。 */
  visible: boolean;
  /** 表格尺寸。 */
  tableSize?: 'small' | 'middle' | 'large';
}

/**
 * 金额方向映射。
 */
const AMOUNT_DIRECTION_MAP: Record<number, { text: string; color: string }> = {
  1: { text: '收入', color: 'success' },
  2: { text: '支出', color: 'error' },
  3: { text: '冻结', color: 'warning' },
  4: { text: '解冻', color: 'processing' },
};

/**
 * 格式化金额文本。
 * @param value 原始金额。
 * @returns 格式化后的金额文本。
 */
function formatCurrency(value?: string): string {
  if (!value || value === '') {
    return '-';
  }
  const amountValue = Number(value);
  if (Number.isNaN(amountValue)) {
    return '-';
  }
  return `¥${amountValue.toFixed(2)}`;
}

/**
 * 钱包流水表格。
 * @param props 组件属性。
 * @returns 钱包流水表格节点。
 */
const WalletFlowTab: React.FC<WalletFlowTabProps> = ({ userId, visible, tableSize = 'small' }) => {
  const [walletFlowData, setWalletFlowData] =
    useState<TableDataResult<UserTypes.UserWalletFlowInfoVo> | null>(null);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
  });
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    if (visible && userId) {
      void fetchWalletFlow();
    }
  }, [visible, userId]);

  /**
   * 获取钱包流水列表。
   * @param pageNum 当前页码。
   * @param pageSize 每页条数。
   * @returns 无返回值。
   */
  const fetchWalletFlow = async (pageNum = 1, pageSize = 10) => {
    if (!userId) {
      return;
    }

    try {
      setLoading(true);
      const result = await getUserWalletFlow(userId, { pageNum, pageSize });
      setWalletFlowData(result);
      setPagination({ current: pageNum, pageSize });
    } catch (error) {
      console.error('获取钱包流水失败:', error);
      messageApi.error('获取钱包流水失败');
      setWalletFlowData(null);
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: '金额方向',
      dataIndex: 'amountDirection',
      key: 'amountDirection',
      width: 100,
      render: (direction: number) => {
        const directionMeta = AMOUNT_DIRECTION_MAP[direction];
        return directionMeta ? (
          <Tag color={directionMeta.color}>{directionMeta.text}</Tag>
        ) : (
          <Tag>未知</Tag>
        );
      },
    },
    {
      title: '变动金额',
      dataIndex: 'amount',
      key: 'amount',
      width: 140,
      render: (amount: string, record: UserTypes.UserWalletFlowInfoVo) => {
        if (!amount || amount === '') {
          return '-';
        }
        const amountValue = Number(amount);
        if (Number.isNaN(amountValue)) {
          return '-';
        }

        const isIncome = record.isIncome;
        const color = isIncome ? '#1677ff' : '#ff4d4f';
        const prefix = isIncome ? '+' : '-';

        return (
          <Text style={{ color, fontWeight: 600 }}>
            {prefix}¥{Math.abs(amountValue).toFixed(2)}
          </Text>
        );
      },
    },
    {
      title: '变动前余额',
      dataIndex: 'beforeBalance',
      key: 'beforeBalance',
      width: 140,
      render: (balance: string) => formatCurrency(balance),
    },
    {
      title: '变动后余额',
      dataIndex: 'afterBalance',
      key: 'afterBalance',
      width: 140,
      render: (balance: string) => formatCurrency(balance),
    },
    {
      title: '变动时间',
      dataIndex: 'changeTime',
      key: 'changeTime',
      width: 180,
      render: (text: string) => text || '-',
    },
  ];

  return (
    <>
      {contextHolder}
      <Spin spinning={loading}>
        {walletFlowData?.rows && walletFlowData.rows.length > 0 ? (
          <Table
            columns={columns}
            dataSource={walletFlowData.rows}
            rowKey="index"
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: walletFlowData.total,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条记录`,
              onChange: (page, size) => fetchWalletFlow(page, size),
              onShowSizeChange: (_current, size) => fetchWalletFlow(1, size),
            }}
            size={tableSize}
            scroll={{ x: 'max-content' }}
          />
        ) : (
          <Empty description="暂无钱包流水" />
        )}
      </Spin>
    </>
  );
};

export default WalletFlowTab;
