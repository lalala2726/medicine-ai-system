import { Col, Divider, Row, Typography } from 'antd';
import React from 'react';
import type { MallOrderTypes } from '@/api/mall/order';

const { Title, Text } = Typography;

interface OrderInfoProps {
  orderDetail: MallOrderTypes.OrderDetailVo | null;
}

// 信息行组件
interface InfoRowProps {
  label: string;
  value: React.ReactNode;
}

const InfoRow: React.FC<InfoRowProps> = ({ label, value }) => (
  <Col span={8}>
    <div
      style={{
        marginBottom: 12,
        fontSize: 14,
        display: 'flex',
        alignItems: 'center',
      }}
    >
      <Text style={{ color: '#666', marginRight: 8 }}>{label}:</Text>
      <Text>{value || '-'}</Text>
    </div>
  </Col>
);

const OrderInfo: React.FC<OrderInfoProps> = ({ orderDetail }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* 用户信息 */}
      <div>
        <Title level={5} style={{ marginBottom: 16 }}>
          用户信息
        </Title>
        <Row gutter={[16, 16]}>
          <InfoRow label="用户UID" value={orderDetail?.userInfo?.userId} />
          <InfoRow label="用户昵称" value={orderDetail?.userInfo?.nickname} />
          <InfoRow label="绑定电话" value={orderDetail?.userInfo?.phoneNumber} />
        </Row>
      </div>

      <Divider />

      {/* 收货信息 */}
      <div>
        <Title level={5} style={{ marginBottom: 16 }}>
          收货信息
        </Title>
        <Row gutter={[16, 16]}>
          <InfoRow label="收货人" value={orderDetail?.deliveryInfo?.receiverName} />
          <InfoRow label="收货电话" value={orderDetail?.deliveryInfo?.receiverPhone} />
          <InfoRow label="收货地址" value={orderDetail?.deliveryInfo?.receiverAddress} />
        </Row>
      </div>

      <Divider />

      {/* 订单信息 */}
      <div>
        <Title level={5} style={{ marginBottom: 16 }}>
          订单信息
        </Title>
        <Row gutter={[16, 16]}>
          <InfoRow label="订单编号" value={orderDetail?.orderInfo?.orderNo} />
          <InfoRow label="商品总数" value={orderDetail?.productInfo?.length || 0} />
          <InfoRow
            label="商品原价"
            value={
              orderDetail?.orderInfo?.itemsAmount
                ? `¥ ${Number(orderDetail.orderInfo.itemsAmount).toFixed(2)}`
                : '-'
            }
          />
          <InfoRow
            label="订单总价"
            value={
              orderDetail?.orderInfo?.totalAmount
                ? `¥ ${Number(orderDetail.orderInfo.totalAmount).toFixed(2)}`
                : '-'
            }
          />
          <InfoRow
            label="优惠券"
            value={
              orderDetail?.orderInfo?.couponName ||
              (orderDetail?.orderInfo?.couponId ? '已使用' : '-')
            }
          />
          <InfoRow
            label="优惠抵扣"
            value={
              orderDetail?.orderInfo?.couponDeductAmount
                ? `¥ ${Number(orderDetail.orderInfo.couponDeductAmount).toFixed(2)}`
                : '-'
            }
          />
          <InfoRow label="支付方式" value={orderDetail?.orderInfo?.payType || '-'} />
        </Row>
      </div>

      <Divider />

      {/* 买家留言 */}
      <div>
        <Title level={5} style={{ marginBottom: 16 }}>
          买家留言
        </Title>
        <Text type="secondary">暂无买家留言</Text>
      </div>
    </div>
  );
};

export default OrderInfo;
