import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { Button, Drawer, Form, message, Space, Spin, Tabs } from 'antd';
import type { UploadFile } from 'antd/es/upload/interface';
import React, { useCallback, useEffect, useState } from 'react';
import { tree as getCategoryTree, type MallCategoryTypes } from '@/api/mall/category';
import {
  addMallProduct,
  getMallProductById,
  type MallProductTypes,
  updateMallProduct,
} from '@/api/mall/product';
import { buildMallProductFormValues } from '@/pages/mall/components/productFormUtils';
import {
  loadEnabledProductTagSelectGroups,
  type ProductTagLike,
  type ProductTagSelectGroup,
} from '@/pages/mall/components/productTagUtils';
import BasicInfo from './BasicInfo';
import DrugDetail from './DrugDetail';

interface ProductEditDrawerProps {
  /** 是否显示抽屉 */
  visible: boolean;
  /** 商品ID */
  productId?: string;
  /** 关闭抽屉回调 */
  onClose: () => void;
  /** 保存成功回调 */
  onSuccess?: () => void;
}

const ProductEditDrawer: React.FC<ProductEditDrawerProps> = ({
  visible,
  productId,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();
  const [activeTab, setActiveTab] = useState('basic');

  // 商品分类相关
  const [categoryTree, setCategoryTree] = useState<MallCategoryTypes.MallCategoryTree[]>([]);
  const [categoryLoading, setCategoryLoading] = useState(false);
  const [imageListFiles, setImageListFiles] = useState<UploadFile[]>([]);
  const [tagGroups, setTagGroups] = useState<ProductTagSelectGroup[]>([]);
  const [tagGroupLoading, setTagGroupLoading] = useState(false);
  const [selectedTags, setSelectedTags] = useState<ProductTagLike[]>([]);

  // 配送方式选项
  const deliveryTypeOptions = [
    { value: 0, label: '咨询商家' },
    { value: 1, label: '自提' },
    { value: 2, label: '快递' },
    { value: 3, label: '同城配送' },
    { value: 4, label: '药店自送' },
    { value: 5, label: '冷链配送' },
    { value: 6, label: '智能取药柜' },
  ];

  // 加载分类树形数据
  const loadCategoryTree = useCallback(async () => {
    try {
      setCategoryLoading(true);
      const response = await getCategoryTree();
      setCategoryTree(response);
    } catch (error) {
      console.error('加载分类数据失败:', error);
      messageApi.error('加载商品分类数据失败');
    } finally {
      setCategoryLoading(false);
    }
  }, [messageApi]);

  /**
   * 加载商品标签分组数据。
   */
  const loadTagGroups = useCallback(async () => {
    try {
      setTagGroupLoading(true);
      const groups = await loadEnabledProductTagSelectGroups();
      setTagGroups(groups);
    } catch (error) {
      console.error('加载商品标签数据失败:', error);
      messageApi.error('加载商品标签数据失败');
    } finally {
      setTagGroupLoading(false);
    }
  }, [messageApi]);

  // 将树形数据转换为 TreeSelect 格式
  const treeSelectData = React.useMemo(() => {
    const transformNodes = (nodes: MallCategoryTypes.MallCategoryTree[]): any[] => {
      return nodes.map((node) => ({
        title: node.name || '未命名分类',
        value: String(node.id),
        key: String(node.id),
        children: node.children ? transformNodes(node.children) : undefined,
      }));
    };

    if (!categoryTree?.length) {
      return [];
    }
    return transformNodes(categoryTree);
  }, [categoryTree]);

  // 加载商品详情
  const fetchProductDetail = useCallback(async () => {
    if (!productId) return;
    setDetailLoading(true);
    try {
      const detail: MallProductTypes.MallProductVo = await getMallProductById(productId);
      if (detail) {
        form.setFieldsValue(buildMallProductFormValues(detail));
        setSelectedTags(detail.tags || []);
        // 设置图片列表
        if (detail.images && detail.images.length > 0) {
          const imageFiles: UploadFile[] = detail.images.map((url: string, index: number) => ({
            uid: `existing-${index}`,
            name: `图片${index + 1}`,
            status: 'done',
            url: url,
            thumbUrl: url,
          }));
          setImageListFiles(imageFiles);
        }
      }
    } catch (error) {
      messageApi.error('获取商品详情失败');
      console.error('fetchProductDetail error:', error);
    } finally {
      setDetailLoading(false);
    }
  }, [form, messageApi, productId]);

  // 初始化数据
  useEffect(() => {
    if (visible) {
      void loadCategoryTree();
      void loadTagGroups();
      if (productId) {
        void fetchProductDetail();
      } else {
        form.resetFields();
        form.setFieldsValue({ tagIds: [] });
        setImageListFiles([]);
        setSelectedTags([]);
        setActiveTab('basic');
      }
    } else {
      // 关闭时重置表单
      form.resetFields();
      setImageListFiles([]);
      setSelectedTags([]);
      setActiveTab('basic');
    }
  }, [fetchProductDetail, form, loadCategoryTree, loadTagGroups, productId, visible]);

  // 提交表单（新增/编辑共用）
  const handleSubmit = async () => {
    try {
      setLoading(true);
      const values = await form.validateFields();

      const images = imageListFiles
        .filter((file) => file.status === 'done' && file.url)
        .map((file) => file.url as string);

      const commonPayload = {
        ...values,
        images,
        stock: values.stock ?? 0,
        categoryIds: (values.categoryIds || []).filter(Boolean),
        tagIds: (values.tagIds || []).filter(Boolean),
      };

      if (productId) {
        const data: MallProductTypes.MallProductUpdateRequest = {
          ...commonPayload,
          id: productId,
        };
        await updateMallProduct(data);
        messageApi.success('更新商品成功');
      } else {
        const data: MallProductTypes.MallProductAddRequest = {
          ...commonPayload,
        };
        await addMallProduct(data);
        messageApi.success('新增商品成功');
      }

      onSuccess?.();
      onClose();
    } catch (error) {
      console.error('handleSubmit error:', error);
      if (error && typeof error === 'object' && 'errorFields' in error) {
        const errorFields = error as any;
        if (errorFields?.errorFields?.length > 0) {
          const firstErrorField = errorFields.errorFields[0];
          const fieldName = firstErrorField.name[0];
          if (
            [
              'name',
              'categoryIds',
              'unit',
              'price',
              'stock',
              'status',
              'sort',
              'deliveryType',
            ].includes(fieldName)
          ) {
            setActiveTab('basic');
          } else if (
            fieldName === 'drugDetail' ||
            (typeof fieldName === 'string' && fieldName.startsWith('drugDetail'))
          ) {
            setActiveTab('detail');
          }
        }
      } else {
        messageApi.error(productId ? '更新商品失败' : '新增商品失败');
      }
    } finally {
      setLoading(false);
    }
  };

  // 标签页切换
  const handleTabChange = (key: string) => {
    setActiveTab(key);
  };

  // 底部按钮
  const footer = (
    <Space>
      <Button onClick={onClose}>取消</Button>
      <Button onClick={() => setActiveTab('basic')} disabled={activeTab === 'basic'}>
        上一步
      </Button>
      <Button onClick={() => setActiveTab('detail')} disabled={activeTab === 'detail'}>
        下一步
      </Button>
      <Button type="primary" onClick={handleSubmit} loading={loading}>
        {productId ? '保存修改' : '保存新增'}
      </Button>
    </Space>
  );

  return (
    <>
      {contextHolder}
      <Drawer
        title={productId ? '编辑商品' : '新增商品'}
        width={900}
        open={visible}
        onClose={onClose}
        footer={footer}
        destroyOnHidden
      >
        <Spin spinning={detailLoading}>
          <Form form={form} layout="vertical" autoComplete={TEXT_INPUT_AUTOCOMPLETE}>
            <Tabs
              activeKey={activeTab}
              onChange={handleTabChange}
              items={[
                {
                  key: 'basic',
                  label: '商品信息',
                  children: (
                    <BasicInfo
                      categoryLoading={categoryLoading}
                      treeSelectData={treeSelectData}
                      imageListFiles={imageListFiles}
                      setImageListFiles={setImageListFiles}
                      deliveryTypeOptions={deliveryTypeOptions}
                      tagGroupLoading={tagGroupLoading}
                      tagGroups={tagGroups}
                      selectedTags={selectedTags}
                      onTagGroupsRefresh={loadTagGroups}
                    />
                  ),
                },
                {
                  key: 'detail',
                  label: '药品信息',
                  children: <DrugDetail />,
                },
              ]}
            />
          </Form>
        </Spin>
      </Drawer>
    </>
  );
};

export default ProductEditDrawer;
