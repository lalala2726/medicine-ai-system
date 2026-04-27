import { Card, Descriptions, Empty, Spin, Tag } from 'antd';
import React, { useEffect, useState } from 'react';
import { getOrderShipping, type MallOrderTypes } from '@/api/mall/order';
import { getDeliveryTypeText } from '@/types/orderStatus';

interface DeliveryRecordsProps {
  orderId: string;
  visible?: boolean;
}

const DeliveryRecords: React.FC<DeliveryRecordsProps> = ({ orderId, visible }) => {
  const [loading, setLoading] = useState(false);
  const [shippingData, setShippingData] = useState<MallOrderTypes.OrderShippingVo | null>(null);

  useEffect(() => {
    if (visible && orderId) {
      fetchShippingData();
    }
  }, [orderId, visible]);

  const fetchShippingData = async () => {
    try {
      setLoading(true);
      const data = await getOrderShipping(orderId);
      setShippingData(data);
    } catch (error) {
      console.error('获取物流信息失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 物流状态映射
  const getLogisticsStatusTag = (status?: string) => {
    const statusMap: Record<string, { text: string; color: string }> = {
      IN_TRANSIT: { text: '运输中', color: 'processing' },
      DELIVERED: { text: '已签收', color: 'success' },
      RETURNED: { text: '已退回', color: 'error' },
      EXCEPTION: { text: '异常', color: 'warning' },
    };

    if (!status) return null;
    const statusInfo = statusMap[status];
    return statusInfo ? (
      <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
    ) : (
      <span>{status}</span>
    );
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '40px 0' }}>
        <Spin tip="加载中..." />
      </div>
    );
  }

  if (!shippingData || !shippingData.logisticsCompany) {
    return <Empty description="暂无发货记录" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  return (
    <Card bordered={false}>
      <Descriptions column={2} bordered>
        <Descriptions.Item label="订单编号" span={2}>
          {shippingData.orderNo || '-'}
        </Descriptions.Item>

        <Descriptions.Item label="订单状态">
          {shippingData.orderStatusName || '-'}
        </Descriptions.Item>

        <Descriptions.Item label="物流状态">
          {getLogisticsStatusTag(shippingData.status)}
        </Descriptions.Item>

        <Descriptions.Item label="物流公司">
          {shippingData.logisticsCompany || '-'}
        </Descriptions.Item>

        <Descriptions.Item label="物流单号">{shippingData.trackingNumber || '-'}</Descriptions.Item>

        <Descriptions.Item label="发货时间" span={2}>
          {shippingData.deliverTime || '-'}
        </Descriptions.Item>

        <Descriptions.Item label="签收时间" span={2}>
          {shippingData.receiveTime || '-'}
        </Descriptions.Item>

        {shippingData.shipmentNote && (
          <Descriptions.Item label="发货备注" span={2}>
            {shippingData.shipmentNote}
          </Descriptions.Item>
        )}

        {shippingData.receiverInfo && (
          <>
            <Descriptions.Item label="收货人姓名">
              {shippingData.receiverInfo.receiverName || '-'}
            </Descriptions.Item>

            <Descriptions.Item label="收货人电话">
              {shippingData.receiverInfo.receiverPhone || '-'}
            </Descriptions.Item>

            <Descriptions.Item label="收货地址" span={2}>
              {shippingData.receiverInfo.receiverDetail || '-'}
            </Descriptions.Item>

            <Descriptions.Item label="配送方式" span={2}>
              {getDeliveryTypeText(shippingData.receiverInfo.deliveryType)}
            </Descriptions.Item>
          </>
        )}
      </Descriptions>
    </Card>
  );
};

export default DeliveryRecords;
