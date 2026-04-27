import { Descriptions, Image } from 'antd';
import React from 'react';
import type { MallAfterSaleTypes } from '@/api/mall/mallAfterSale';

interface ProductInfoTabProps {
  productInfo?: MallAfterSaleTypes.AfterSaleDetailVo.ProductInfo;
}

const ProductInfoTab: React.FC<ProductInfoTabProps> = ({ productInfo }) => {
  if (!productInfo) {
    return <div style={{ textAlign: 'center', padding: '40px 0' }}>暂无商品信息</div>;
  }

  return (
    <Descriptions column={1} bordered>
      <Descriptions.Item label="商品图片">
        {productInfo.productImage ? (
          <Image
            width={100}
            height={100}
            src={productInfo.productImage}
            alt="商品图片"
            style={{ objectFit: 'cover' }}
          />
        ) : (
          '-'
        )}
      </Descriptions.Item>
      <Descriptions.Item label="商品名称">{productInfo.productName}</Descriptions.Item>
      <Descriptions.Item label="商品ID">{productInfo.productId}</Descriptions.Item>
      <Descriptions.Item label="商品单价">
        ¥{Number(productInfo.productPrice || 0).toFixed(2)}
      </Descriptions.Item>
      <Descriptions.Item label="购买数量">{productInfo.quantity}</Descriptions.Item>
      <Descriptions.Item label="小计金额">
        ¥{Number(productInfo.totalPrice || 0).toFixed(2)}
      </Descriptions.Item>
    </Descriptions>
  );
};

export default ProductInfoTab;
