import { Card, Divider, Empty, Image, Space, Typography } from 'antd';
import React from 'react';
import type { MallOrderTypes } from '@/api/mall/order';

const { Text } = Typography;

interface ProductInfoProps {
  orderDetail: MallOrderTypes.OrderDetailVo | null;
}

const ProductInfo: React.FC<ProductInfoProps> = ({ orderDetail }) => {
  if (!orderDetail?.productInfo?.length) {
    return <Empty description="暂无商品信息" />;
  }

  return (
    <div>
      {orderDetail.productInfo.map((product) => (
        <Card
          key={product.productId || product.productName}
          size="small"
          style={{ marginBottom: 12 }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 16,
            }}
          >
            <Image
              src={product.productImage}
              alt={product.productName}
              width={80}
              height={80}
              style={{ objectFit: 'cover', borderRadius: 4 }}
              preview={{
                mask: '预览',
              }}
            />
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 500, marginBottom: 8, fontSize: 15 }}>
                {product.productName}
              </div>
              <Space split={<Divider type="vertical" />}>
                <Text type="secondary">
                  单价: {product.productPrice ? `¥${Number(product.productPrice).toFixed(2)}` : '-'}
                </Text>
                <Text type="secondary">数量: {product.productQuantity}</Text>
                <Text type="secondary">
                  优惠:{' '}
                  {product.couponDeductAmount
                    ? `-¥${Number(product.couponDeductAmount).toFixed(2)}`
                    : '¥0.00'}
                </Text>
                <Text type="secondary">
                  应付:{' '}
                  {product.payableAmount ? `¥${Number(product.payableAmount).toFixed(2)}` : '-'}
                </Text>
                <Text strong>
                  总价:{' '}
                  {product.productTotalAmount
                    ? `¥${Number(product.productTotalAmount).toFixed(2)}`
                    : '-'}
                </Text>
              </Space>
            </div>
          </div>
        </Card>
      ))}
    </div>
  );
};

export default ProductInfo;
