import React from 'react';
import { Space, Typography, Table, Image, Tooltip, Progress } from 'antd';
import { ProCard } from '@ant-design/pro-components';
import type { ColumnsType } from 'antd/es/table';
import { AnalyticsTypes } from '@/api/mall/analytics';
import { formatCurrency, formatNumber, formatPercent } from '../utils';
import styles from '../index.module.less';

const { Text } = Typography;

interface SalesRankingTableProps {
  topSellingProducts?: AnalyticsTypes.AnalyticsTopSellingProductVo[];
  returnRefundRiskProducts?: AnalyticsTypes.AnalyticsReturnRefundRiskProductVo[];
}

const SalesRankingTable: React.FC<SalesRankingTableProps> = ({
  topSellingProducts = [],
  returnRefundRiskProducts = [],
}) => {
  const topSellingColumns: ColumnsType<AnalyticsTypes.AnalyticsTopSellingProductVo> = [
    {
      title: '排名',
      width: 60,
      align: 'center',
      render: (_value, _record, index) => {
        const rank = index + 1;
        let color = '#8c8c8c';
        if (rank === 1) color = '#f5222d';
        if (rank === 2) color = '#fa8c16';
        if (rank === 3) color = '#faad14';
        return <Text style={{ color, fontWeight: 'bold' }}>{rank}</Text>;
      },
    },
    {
      title: '商品信息',
      dataIndex: 'productName',
      render: (_value, record) => (
        <Space>
          <Image
            width={40}
            height={40}
            src={record.productImage}
            className={styles.productImg}
            fallback="data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs="
            preview={false}
          />
          <div className={styles.productCell}>
            <Tooltip title={record.productName}>
              <Text strong style={{ width: 180 }} ellipsis>
                {record.productName || '-'}
              </Text>
            </Tooltip>
            <Text type="secondary" style={{ fontSize: 12 }}>
              ID: {record.productId || '-'}
            </Text>
          </div>
        </Space>
      ),
    },
    {
      title: '累计销量',
      dataIndex: 'soldQuantity',
      align: 'right',
      width: 100,
      sorter: (a, b) => Number(a.soldQuantity || 0) - Number(b.soldQuantity || 0),
      render: (value) => <Text strong>{formatNumber(value)}</Text>,
    },
    {
      title: '成交金额',
      dataIndex: 'paidAmount',
      align: 'right',
      width: 120,
      sorter: (a, b) => Number(a.paidAmount || 0) - Number(b.paidAmount || 0),
      render: (value) => (
        <Text strong className={styles.moneyText}>
          {formatCurrency(value)}
        </Text>
      ),
    },
  ];

  const riskColumns: ColumnsType<AnalyticsTypes.AnalyticsReturnRefundRiskProductVo> = [
    {
      title: '商品信息',
      dataIndex: 'productName',
      render: (_value, record) => (
        <Space>
          <Image
            width={40}
            height={40}
            src={record.productImage}
            className={styles.productImg}
            fallback="data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs="
            preview={false}
          />
          <div className={styles.productCell}>
            <Tooltip title={record.productName}>
              <Text strong style={{ width: 150 }} ellipsis>
                {record.productName || '-'}
              </Text>
            </Tooltip>
            <Text type="secondary" style={{ fontSize: 12 }}>
              ID: {record.productId || '-'}
            </Text>
          </div>
        </Space>
      ),
    },
    {
      title: '销量',
      dataIndex: 'soldQuantity',
      width: 90,
      align: 'right',
      render: (value) => formatNumber(value),
    },
    {
      title: '退货退款件数',
      dataIndex: 'returnRefundQuantity',
      width: 120,
      align: 'right',
      render: (value) => <Text type="danger">{formatNumber(value)}</Text>,
    },
    {
      title: '退货退款率',
      dataIndex: 'returnRefundRate',
      width: 220,
      render: (value) => {
        const percent = Number(value || 0) * 100;
        return (
          <Space direction="vertical" size={0} style={{ width: '100%' }}>
            <Progress
              percent={Number(percent.toFixed(1))}
              size="small"
              strokeColor={percent >= 20 ? '#f5222d' : percent >= 10 ? '#fa8c16' : '#1677ff'}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              {formatPercent(value)}
            </Text>
          </Space>
        );
      },
    },
    {
      title: '退款金额',
      dataIndex: 'refundAmount',
      align: 'right',
      width: 110,
      render: (value) => (
        <Text strong className={styles.moneyText}>
          {formatCurrency(value)}
        </Text>
      ),
    },
  ];

  return (
    <div className={styles.tableSection} style={{ marginTop: 16 }}>
      <ProCard
        className={styles.mainCard}
        headerBordered
        title="热销商品排行"
        extra={<Text type="secondary">按支付时间统计</Text>}
      >
        <div className={styles.tableScrollArea}>
          <Table
            rowKey="productId"
            columns={topSellingColumns}
            dataSource={topSellingProducts}
            pagination={false}
            size="small"
            scroll={{ y: 350 }}
            locale={{ emptyText: '暂无数据' }}
          />
        </div>
      </ProCard>

      <ProCard
        className={styles.mainCard}
        headerBordered
        title="退货退款风险预警"
        extra={<Text type="secondary">异常率商品重点关注</Text>}
      >
        <div className={styles.tableScrollArea}>
          <Table
            rowKey="productId"
            columns={riskColumns}
            dataSource={returnRefundRiskProducts}
            pagination={false}
            size="small"
            scroll={{ y: 350 }}
            rowClassName={(record) =>
              Number(record.returnRefundRate || 0) >= 0.2 ? styles.highRiskRow : ''
            }
            locale={{ emptyText: '暂无数据' }}
          />
        </div>
      </ProCard>
    </div>
  );
};

export default SalesRankingTable;
