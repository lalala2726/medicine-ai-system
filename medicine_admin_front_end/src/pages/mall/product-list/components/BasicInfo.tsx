import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { Col, Form, Input, InputNumber, Radio, Row, Select, TreeSelect } from 'antd';
import type { UploadFile } from 'antd/es/upload/interface';
import React from 'react';
import { ImageUploadList } from '@/components';
import ProductUnitSelect from '@/pages/mall/components/ProductUnitSelect';
import ProductTagDrawerSelect from '@/pages/mall/components/ProductTagDrawerSelect';
import type {
  ProductTagLike,
  ProductTagSelectGroup,
} from '@/pages/mall/components/productTagUtils';

interface BasicInfoProps {
  categoryLoading: boolean;
  treeSelectData: any[];
  imageListFiles: UploadFile[];
  setImageListFiles: React.Dispatch<React.SetStateAction<UploadFile[]>>;
  deliveryTypeOptions: Array<{ value: number; label: string }>;
  tagGroupLoading: boolean;
  tagGroups: ProductTagSelectGroup[];
  selectedTags?: ProductTagLike[];
  onTagGroupsRefresh?: () => Promise<void> | void;
}

const BasicInfo: React.FC<BasicInfoProps> = ({
  categoryLoading,
  treeSelectData,
  imageListFiles,
  setImageListFiles,
  deliveryTypeOptions,
  tagGroupLoading,
  tagGroups,
  selectedTags = [],
  onTagGroupsRefresh,
}) => {
  return (
    <Row gutter={16}>
      <Col span={12}>
        <Form.Item
          label="商品名称"
          name="name"
          rules={[{ required: true, message: '请输入商品名称' }]}
        >
          <Input
            autoComplete={TEXT_INPUT_AUTOCOMPLETE}
            placeholder="请输入商品名称"
            maxLength={100}
          />
        </Form.Item>
      </Col>
      <Col span={12}>
        <Form.Item
          label="商品分类"
          name="categoryIds"
          rules={[{ required: true, message: '请选择商品分类' }]}
        >
          <TreeSelect
            placeholder="请选择商品分类"
            allowClear
            multiple
            showSearch
            treeDefaultExpandAll={false}
            loading={categoryLoading}
            treeData={treeSelectData}
            treeNodeFilterProp="title"
            styles={{ popup: { root: { maxHeight: 400, overflow: 'auto' } } }}
          />
        </Form.Item>
      </Col>

      <Col span={12}>
        <Form.Item
          label="单位"
          name="unit"
          rules={[{ required: true, message: '请选择或新增商品单位' }]}
        >
          <ProductUnitSelect />
        </Form.Item>
      </Col>
      <Col span={12}>
        <Form.Item
          label="售价"
          name="price"
          rules={[{ required: true, message: '请输入商品售价' }]}
        >
          <InputNumber
            autoComplete={TEXT_INPUT_AUTOCOMPLETE}
            placeholder="请输入商品售价"
            min={0}
            precision={2}
            style={{ width: '100%' }}
            addonAfter="元"
          />
        </Form.Item>
      </Col>

      <Col span={12}>
        <Form.Item
          label="库存数量"
          name="stock"
          rules={[{ required: true, message: '请输入库存数量' }]}
        >
          <InputNumber
            autoComplete={TEXT_INPUT_AUTOCOMPLETE}
            placeholder="请输入库存数量"
            min={0}
            style={{ width: '100%' }}
          />
        </Form.Item>
      </Col>

      <Col span={12}>
        <Form.Item label="状态" name="status">
          <Radio.Group>
            <Radio value={1}>上架</Radio>
            <Radio value={0}>下架</Radio>
          </Radio.Group>
        </Form.Item>
      </Col>

      <Col span={12}>
        <Form.Item label="排序值" name="sort" tooltip="数值越小越靠前">
          <InputNumber
            autoComplete={TEXT_INPUT_AUTOCOMPLETE}
            placeholder="请输入排序值"
            min={0}
            style={{ width: '100%' }}
          />
        </Form.Item>
      </Col>

      <Col span={12}>
        <Form.Item
          label="配送方式"
          name="deliveryType"
          tooltip="请选择合适的配送方式，不同方式适用于不同类型的药品和场景"
        >
          <Select
            placeholder="请选择配送方式"
            allowClear
            showSearch
            filterOption={(input, option) =>
              String(option?.label ?? '')
                .toLowerCase()
                .includes(input.toLowerCase())
            }
            options={deliveryTypeOptions.map((option) => ({
              ...option,
              title: option.label,
            }))}
          />
        </Form.Item>
      </Col>

      <Col span={12}>
        <Form.Item
          label="允许用券"
          name="couponEnabled"
          rules={[{ required: true, message: '请选择是否允许使用优惠券' }]}
          tooltip="关闭后，该商品在客户端结算时将不会参与优惠券计算"
        >
          <Radio.Group>
            <Radio value={1}>允许</Radio>
            <Radio value={0}>不允许</Radio>
          </Radio.Group>
        </Form.Item>
      </Col>

      <Col span={24}>
        <Form.Item
          label="商品标签"
          name="tagIds"
          extra="点击上方区域打开抽屉选择标签，可为一个商品选择多个标签"
        >
          <ProductTagDrawerSelect
            groups={tagGroups}
            selectedTags={selectedTags}
            loading={tagGroupLoading}
            placeholder="请选择商品标签"
            onGroupsRefresh={onTagGroupsRefresh}
          />
        </Form.Item>
      </Col>

      <Col span={24}>
        <Form.Item label="商品图片">
          <ImageUploadList
            value={imageListFiles}
            onChange={setImageListFiles}
            tip="支持拖拽调整图片顺序，默认首张图片为主图，最多可上传8张图片"
            allowedTypes={['image/png', 'image/jpeg']}
            maxSizeMB={5}
          />
        </Form.Item>
      </Col>
    </Row>
  );
};

export default BasicInfo;
