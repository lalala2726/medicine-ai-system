import { Button, Descriptions, Drawer, Image, Space, Spin, Tabs, Tag, Typography } from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { getMallProductById, type MallProductTypes } from '@/api/mall/product';
import { getDrugCategoryMeta } from '@/constants/drugCategory';
import { groupProductTagsByType } from '@/pages/mall/components/productTagUtils';

interface ProductDetailDrawerProps {
  /** 是否显示抽屉 */
  visible: boolean;
  /** 商品ID */
  productId?: string;
  /** 关闭抽屉回调 */
  onClose: () => void;
}

const ProductDetailDrawer: React.FC<ProductDetailDrawerProps> = ({
  visible,
  productId,
  onClose,
}) => {
  const [loading, setLoading] = useState(false);
  const [productDetail, setProductDetail] = useState<MallProductTypes.MallProductVo>();
  const [activeTab, setActiveTab] = useState('basic');

  const descriptionLabelStyle = {
    width: 140,
    whiteSpace: 'normal' as const,
    lineHeight: 1.6,
    padding: '12px 16px',
  };
  const descriptionContentStyle = {
    whiteSpace: 'pre-wrap' as const,
    wordBreak: 'break-word' as const,
    lineHeight: 1.6,
    padding: '12px 16px',
  };

  // 配送方式映射
  const deliveryTypeMap: Record<number, string> = {
    0: '咨询商家',
    1: '自提',
    2: '快递',
    3: '同城配送',
    4: '药店自送',
    5: '冷链配送',
    6: '智能取药柜',
  };

  /**
   * 加载商品详情数据。
   *
   * @returns {Promise<void>} 返回商品详情加载完成后的 Promise。
   */
  const fetchProductDetail = useCallback(async (): Promise<void> => {
    if (!productId) return;
    setLoading(true);
    try {
      const detail = await getMallProductById(productId);
      setProductDetail(detail);
    } catch (error) {
      console.error('获取商品详情失败:', error);
    } finally {
      setLoading(false);
    }
  }, [productId]);

  // 初始化数据
  useEffect(() => {
    if (visible && productId) {
      void fetchProductDetail();
      setActiveTab('basic');
    } else {
      setProductDetail(undefined);
    }
  }, [fetchProductDetail, productId, visible]);

  // 商品信息标签页内容
  const renderBasicInfo = () => {
    if (!productDetail) return null;
    const groupedTags = groupProductTagsByType(productDetail.tags);

    return (
      <Descriptions
        column={2}
        bordered
        labelStyle={descriptionLabelStyle}
        contentStyle={descriptionContentStyle}
      >
        <Descriptions.Item label="商品ID" span={2}>
          {productDetail.id || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="商品名称" span={2}>
          {productDetail.name || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="商品分类">
          {productDetail.categoryNames && productDetail.categoryNames.length > 0 ? (
            <Space size={[8, 8]} wrap>
              {productDetail.categoryNames.map((categoryName) => (
                <Tag key={categoryName} color="blue">
                  {categoryName}
                </Tag>
              ))}
            </Space>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="商品单位">{productDetail.unit || '-'}</Descriptions.Item>
        <Descriptions.Item label="商品标签" span={2}>
          {groupedTags.length > 0 ? (
            <Space direction="vertical" size={8}>
              {groupedTags.map((group) => (
                <Space key={group.typeId} size={[8, 8]} wrap>
                  <Typography.Text type="secondary">{group.typeName}：</Typography.Text>
                  {group.tags.map((tag) => (
                    <Tag key={tag.id} color="processing">
                      {tag.name}
                    </Tag>
                  ))}
                </Space>
              ))}
            </Space>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="商品图片" span={2}>
          {productDetail.images && productDetail.images.length > 0 ? (
            <Image.PreviewGroup>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {productDetail.images.map((url) => (
                  <Image
                    key={url}
                    width={80}
                    height={80}
                    src={url}
                    alt="商品图片"
                    style={{ objectFit: 'cover', borderRadius: 4 }}
                  />
                ))}
              </div>
            </Image.PreviewGroup>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="售价">
          {productDetail.price ? `¥${Number(productDetail.price).toFixed(2)}` : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="库存数量">{productDetail.stock ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="销量">
          {productDetail.sales ?? productDetail.salesVolume ?? '-'}
        </Descriptions.Item>
        <Descriptions.Item label="状态">
          {productDetail.status === 1 ? (
            <Tag color="success">上架</Tag>
          ) : (
            <Tag color="default">下架</Tag>
          )}
        </Descriptions.Item>
        <Descriptions.Item label="排序值">{productDetail.sort ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="配送方式">
          {productDetail.deliveryType !== undefined
            ? deliveryTypeMap[productDetail.deliveryType] || '-'
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="创建时间">{productDetail.createTime || '-'}</Descriptions.Item>
        <Descriptions.Item label="更新时间">{productDetail.updateTime || '-'}</Descriptions.Item>
        <Descriptions.Item label="创建者">{productDetail.createBy || '-'}</Descriptions.Item>
        <Descriptions.Item label="更新者">{productDetail.updateBy || '-'}</Descriptions.Item>
      </Descriptions>
    );
  };

  // 药品信息标签页内容
  const renderDrugDetail = () => {
    const DrugDetail = productDetail?.drugDetail;
    const drugCategoryMeta = getDrugCategoryMeta(DrugDetail?.drugCategory);
    if (!DrugDetail) {
      return (
        <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
          暂无药品详细信息
        </div>
      );
    }

    return (
      <Descriptions
        column={1}
        bordered
        labelStyle={descriptionLabelStyle}
        contentStyle={descriptionContentStyle}
      >
        <Descriptions.Item label="药品通用名">{DrugDetail.commonName || '-'}</Descriptions.Item>
        <Descriptions.Item label="品牌">{DrugDetail.brand || '-'}</Descriptions.Item>
        <Descriptions.Item label="包装规格">{DrugDetail.packaging || '-'}</Descriptions.Item>
        <Descriptions.Item label="生产单位">{DrugDetail.productionUnit || '-'}</Descriptions.Item>
        <Descriptions.Item label="批准文号">{DrugDetail.approvalNumber || '-'}</Descriptions.Item>
        <Descriptions.Item label="执行标准">
          {DrugDetail.executiveStandard || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="有效期">{DrugDetail.validityPeriod || '-'}</Descriptions.Item>
        <Descriptions.Item label="产地类型">{DrugDetail.originType || '-'}</Descriptions.Item>
        <Descriptions.Item label="药品分类">
          {drugCategoryMeta ? (
            <Tag color={drugCategoryMeta.tagColor}>
              {`${drugCategoryMeta.shortLabel} / ${drugCategoryMeta.name}`}
            </Tag>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="分类说明">
          {drugCategoryMeta ? drugCategoryMeta.description : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="是否外用药">
          {DrugDetail.isOutpatientMedicine ? (
            <Tag color="blue">是</Tag>
          ) : (
            <Tag color="default">否</Tag>
          )}
        </Descriptions.Item>
        <Descriptions.Item label="成分">{DrugDetail.composition || '-'}</Descriptions.Item>
        <Descriptions.Item label="性状">{DrugDetail.characteristics || '-'}</Descriptions.Item>
        <Descriptions.Item label="贮藏条件">
          {DrugDetail.storageConditions || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="功能主治">{DrugDetail.efficacy || '-'}</Descriptions.Item>
        <Descriptions.Item label="用法用量">{DrugDetail.usageMethod || '-'}</Descriptions.Item>
        <Descriptions.Item label="不良反应">{DrugDetail.adverseReactions || '-'}</Descriptions.Item>
        <Descriptions.Item label="注意事项">{DrugDetail.precautions || '-'}</Descriptions.Item>
        <Descriptions.Item label="禁忌">{DrugDetail.taboo || '-'}</Descriptions.Item>
        <Descriptions.Item label="药品说明书全文">
          {DrugDetail.instruction || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="温馨提示">{DrugDetail.warmTips || '-'}</Descriptions.Item>
      </Descriptions>
    );
  };

  // 底部按钮
  const footer = (
    <div style={{ textAlign: 'right' }}>
      <Button onClick={onClose}>关闭</Button>
    </div>
  );

  return (
    <Drawer
      title="商品详情"
      width={1100}
      open={visible}
      onClose={onClose}
      footer={footer}
      destroyOnHidden
    >
      <Spin spinning={loading}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'basic',
              label: '商品信息',
              children: renderBasicInfo(),
            },
            {
              key: 'detail',
              label: '药品信息',
              children: renderDrugDetail(),
            },
          ]}
        />
      </Spin>
    </Drawer>
  );
};

export default ProductDetailDrawer;
