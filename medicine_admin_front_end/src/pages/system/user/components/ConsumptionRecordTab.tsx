import { Empty, message, Space, Spin, Table, Typography } from 'antd';
import React, { useEffect, useState } from 'react';
import { getConsumeInfo, type UserTypes } from '@/api/system/user';
import type { TableDataResult } from '@/types';

const { Text } = Typography;

interface ConsumptionRecordTabProps {
  /** 用户ID。 */
  userId: number;
  /** 是否展示当前标签页。 */
  visible: boolean;
  /** 表格尺寸。 */
  tableSize?: 'small' | 'middle' | 'large';
}

const ConsumptionRecordTab: React.FC<ConsumptionRecordTabProps> = ({
  userId,
  visible,
  tableSize = 'small',
}) => {
  const [consumeInfoData, setConsumeInfoData] =
    useState<TableDataResult<UserTypes.UserConsumeInfo> | null>(null);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
  });
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    if (visible && userId) {
      fetchConsumeInfo();
    }
  }, [visible, userId]);

  const fetchConsumeInfo = async (pageNum = 1, pageSize = 10) => {
    if (!userId) return;

    try {
      setLoading(true);
      const result = await getConsumeInfo(userId, { pageNum, pageSize });
      setConsumeInfoData(result);
      setPagination({ current: pageNum, pageSize });
    } catch (error) {
      console.error('获取消费记录失败:', error);
      messageApi.error('获取消费记录失败');
      setConsumeInfoData(null);
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: '订单编号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 200,
      render: (orderNo: string) => {
        if (!orderNo || orderNo === '') {
          return '-';
        }
        return (
          <Space>
            <Text style={{ fontFamily: 'monospace' }}>{orderNo}</Text>
            <Text copyable={{ text: orderNo }} />
          </Space>
        );
      },
    },
    {
      title: '商品总价',
      dataIndex: 'totalPrice',
      key: 'totalPrice',
      width: 120,
      render: (price: string) => {
        if (!price || price === '') return '-';
        const num = Number(price);
        return isNaN(num) ? '-' : `¥${num.toFixed(2)}`;
      },
    },
    {
      title: '实付金额',
      dataIndex: 'payPrice',
      key: 'payPrice',
      width: 120,
      render: (price: string) => {
        if (!price || price === '') return '-';
        const num = Number(price);
        if (isNaN(num)) return '-';
        return <Text style={{ color: '#ff4d4f', fontWeight: 500 }}>¥{num.toFixed(2)}</Text>;
      },
    },
    {
      title: '完成时间',
      dataIndex: 'finishTime',
      key: 'finishTime',
      width: 160,
      render: (text: string) => text || '-',
    },
  ];

  return (
    <>
      {contextHolder}
      <Spin spinning={loading}>
        {consumeInfoData?.rows && consumeInfoData.rows.length > 0 ? (
          <Table
            columns={columns}
            dataSource={consumeInfoData.rows}
            rowKey="index"
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: consumeInfoData.total,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条记录`,
              onChange: (page, size) => fetchConsumeInfo(page, size),
              onShowSizeChange: (_current, size) => fetchConsumeInfo(1, size),
            }}
            size={tableSize}
            scroll={{ x: 'max-content' }}
          />
        ) : (
          <Empty />
        )}
      </Spin>
    </>
  );
};

export default ConsumptionRecordTab;
