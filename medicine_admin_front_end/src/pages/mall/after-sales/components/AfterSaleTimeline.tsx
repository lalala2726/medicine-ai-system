import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import { Empty, Tag, Timeline } from 'antd';
import React from 'react';
import type { MallAfterSaleTypes } from '@/api/mall/mallAfterSale';

interface AfterSaleTimelineProps {
  timeline?: MallAfterSaleTypes.AfterSaleTimelineVo[];
}

const AfterSaleTimeline: React.FC<AfterSaleTimelineProps> = ({ timeline }) => {
  if (!timeline || timeline.length === 0) {
    return <Empty description="暂无时间线记录" />;
  }

  // 根据事件状态获取图标
  const getEventIcon = (eventStatus?: string) => {
    const status = eventStatus?.toUpperCase();

    if (status === 'SUCCESS' || status === 'COMPLETED') {
      return <CheckCircleOutlined style={{ fontSize: 16, color: '#52c41a' }} />;
    }
    if (status === 'FAILED' || status === 'CANCELLED') {
      return <CloseCircleOutlined style={{ fontSize: 16, color: '#ff4d4f' }} />;
    }
    if (status === 'PROCESSING') {
      return <SyncOutlined spin style={{ fontSize: 16, color: '#1890ff' }} />;
    }

    return <ClockCircleOutlined style={{ fontSize: 16, color: '#8c8c8c' }} />;
  };

  // 根据事件状态获取时间线颜色
  const getTimelineColor = (eventStatus?: string) => {
    const status = eventStatus?.toUpperCase();

    if (status === 'SUCCESS' || status === 'COMPLETED') {
      return 'green';
    }
    if (status === 'FAILED' || status === 'CANCELLED') {
      return 'red';
    }
    if (status === 'PROCESSING') {
      return 'blue';
    }

    return 'gray';
  };

  // 根据操作人类型获取标签
  const getOperatorTag = (operatorType?: string, operatorTypeName?: string) => {
    const type = operatorType?.toUpperCase();
    const colorMap: Record<string, string> = {
      SYSTEM: 'blue',
      USER: 'green',
      ADMIN: 'orange',
      MERCHANT: 'purple',
    };

    return (
      <Tag color={colorMap[type || ''] || 'default'}>
        {operatorTypeName || operatorType || '未知'}
      </Tag>
    );
  };

  return (
    <Timeline
      items={timeline.map((item) => ({
        color: getTimelineColor(item.eventStatus),
        dot: getEventIcon(item.eventStatus),
        children: (
          <div>
            <div style={{ marginBottom: 8 }}>
              <span style={{ fontWeight: 500, marginRight: 8 }}>
                {item.eventTypeName || item.eventType || '未知事件'}
              </span>
              {getOperatorTag(item.operatorType, item.operatorTypeName)}
            </div>
            {item.description && (
              <div style={{ color: '#666', marginBottom: 4 }}>{item.description}</div>
            )}
            <div style={{ color: '#999', fontSize: 12 }}>{item.createTime}</div>
          </div>
        ),
      }))}
    />
  );
};

export default AfterSaleTimeline;
