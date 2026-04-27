import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import { Card, Empty, Space, Spin, Tag, Timeline, Typography } from 'antd';
import React, { useEffect, useState } from 'react';
import { getOrderTimeline, type MallOrderTypes } from '@/api/mall/order';

const { Text } = Typography;

interface OrderTimelineProps {
  orderId: string;
  visible: boolean;
}

const OrderTimeline: React.FC<OrderTimelineProps> = ({ orderId, visible }) => {
  const [timelineData, setTimelineData] = useState<MallOrderTypes.MallOrderTimelineVo[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (visible && orderId) {
      fetchTimeline();
    }
  }, [visible, orderId]);

  const fetchTimeline = async () => {
    if (!orderId) return;

    try {
      setLoading(true);
      const result = await getOrderTimeline(orderId);
      // 确保返回的是数组
      if (Array.isArray(result)) {
        setTimelineData(result);
      } else if (result?.data && Array.isArray(result.data)) {
        setTimelineData(result.data);
      } else {
        setTimelineData([]);
      }
    } catch (error) {
      console.error('获取订单时间线失败:', error);
      setTimelineData([]);
    } finally {
      setLoading(false);
    }
  };

  // 根据事件类型和状态获取图标
  const getTimelineIcon = (eventType?: string, eventStatus?: string) => {
    const type = eventType?.toUpperCase();
    const status = eventStatus?.toUpperCase();

    // 根据状态返回不同的图标
    if (status === 'SUCCESS' || status === 'COMPLETED') {
      return <CheckCircleOutlined style={{ fontSize: 16 }} />;
    }
    if (status === 'FAILED' || status === 'CANCELLED') {
      return <CloseCircleOutlined style={{ fontSize: 16 }} />;
    }
    if (status === 'PENDING' || status === 'PROCESSING') {
      return <SyncOutlined spin style={{ fontSize: 16 }} />;
    }

    // 根据事件类型返回默认图标
    if (type?.includes('CREATE') || type?.includes('SUBMIT')) {
      return <CheckCircleOutlined style={{ fontSize: 16 }} />;
    }
    if (type?.includes('PAY')) {
      return <CheckCircleOutlined style={{ fontSize: 16 }} />;
    }
    if (type?.includes('SHIP') || type?.includes('DELIVER')) {
      return <SyncOutlined style={{ fontSize: 16 }} />;
    }
    if (type?.includes('RECEIVE') || type?.includes('COMPLETE')) {
      return <CheckCircleOutlined style={{ fontSize: 16 }} />;
    }
    if (type?.includes('REFUND') || type?.includes('CANCEL')) {
      return <CloseCircleOutlined style={{ fontSize: 16 }} />;
    }

    return <ClockCircleOutlined style={{ fontSize: 16 }} />;
  };

  // 根据事件类型和状态获取时间线颜色
  const getTimelineColor = (_eventType?: string, eventStatus?: string) => {
    const status = eventStatus?.toUpperCase();

    if (status === 'SUCCESS' || status === 'COMPLETED') {
      return 'green';
    }
    if (status === 'FAILED' || status === 'CANCELLED') {
      return 'red';
    }
    if (status === 'PENDING' || status === 'PROCESSING') {
      return 'blue';
    }

    return 'gray';
  };

  // 根据操作方类型获取标签
  const getOperatorTag = (operatorType?: string) => {
    const type = operatorType?.toUpperCase();
    switch (type) {
      case 'USER':
        return <Tag color="blue">用户操作</Tag>;
      case 'ADMIN':
        return <Tag color="orange">管理员</Tag>;
      case 'SYSTEM':
        return <Tag color="green">系统</Tag>;
      default:
        return <Tag>{operatorType || '未知'}</Tag>;
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '40px 0' }}>
        <Spin tip="加载订单流程中..." />
      </div>
    );
  }

  if (!timelineData || timelineData.length === 0) {
    return <Empty description="暂无订单流程记录" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  return (
    <Card
      bordered={false}
      style={{
        boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
        borderRadius: 8,
      }}
      bodyStyle={{ padding: '32px 24px' }}
    >
      <Timeline
        items={timelineData.map((item, index) => ({
          key: item.id || index,
          color: getTimelineColor(item.eventType, item.eventStatus),
          dot: getTimelineIcon(item.eventType, item.eventStatus),
          children: (
            <div
              style={{
                paddingBottom: index === timelineData.length - 1 ? 0 : 24,
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                  marginBottom: 8,
                }}
              >
                <div style={{ flex: 1 }}>
                  <Space size={8} style={{ marginBottom: 8 }}>
                    <Text
                      strong
                      style={{
                        fontSize: 15,
                        color: '#262626',
                      }}
                    >
                      {item.description || '订单事件'}
                    </Text>
                    {getOperatorTag(item.operatorType)}
                  </Space>
                  {item.eventType && (
                    <div style={{ marginTop: 4 }}>
                      <Text type="secondary" style={{ fontSize: 13, display: 'block' }}>
                        事件类型: {item.eventType}
                      </Text>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ),
        }))}
      />
    </Card>
  );
};

export default OrderTimeline;
