import { PageContainer } from '@ant-design/pro-components';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Form, Steps, Space, Spin, Tabs, Typography, message, theme, Divider } from 'antd';
import {
  InfoCircleOutlined,
  MedicineBoxOutlined,
  LeftOutlined,
  RightOutlined,
  SaveOutlined,
} from '@ant-design/icons';
import type { UploadFile } from 'antd/es/upload/interface';
import React, { useCallback, useEffect, useState } from 'react';
import { createStyles } from 'antd-style';
import { tree as getCategoryTree, type MallCategoryTypes } from '@/api/mall/category';
import {
  addMallProduct,
  getMallProductById,
  type MallProductTypes,
  updateMallProduct,
} from '@/api/mall/product';
import {
  loadEnabledProductTagSelectGroups,
  type ProductTagLike,
  type ProductTagSelectGroup,
} from '@/pages/mall/components/productTagUtils';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { buildMallProductFormValues } from '@/pages/mall/components/productFormUtils';
import { routePaths } from '@/router/paths';
import BasicInfo from '@/pages/mall/product-list/components/BasicInfo';
import DrugDetail from '@/pages/mall/product-list/components/DrugDetail';

const useStyles = createStyles(({ token }) => ({
  container: {
    paddingBottom: 80,
  },
  stepsWrapper: {
    padding: '12px 0',
    background: token.colorBgContainer,
    borderRadius: token.borderRadiusLG,
    marginBottom: 16,
    boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
    border: `1px solid ${token.colorBorderSecondary}`,
  },
  steps: {
    maxWidth: 600,
    margin: '0 auto',
  },
  formContent: {
    background: token.colorBgContainer,
    borderRadius: token.borderRadiusLG,
    padding: '20px 24px',
    minHeight: 400,
    boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
    border: `1px solid ${token.colorBorderSecondary}`,
  },
  footer: {
    position: 'fixed',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: token.colorBgElevated,
    borderTop: `1px solid ${token.colorBorderSecondary}`,
    padding: '16px 24px',
    zIndex: 1000,
    backdropFilter: 'blur(8px)',
    boxShadow: '0 -2px 8px rgba(0,0,0,0.05)',
  },
  footerContent: {
    maxWidth: 1200,
    margin: '0 auto',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  sectionTitle: {
    marginBottom: 24,
    display: 'flex',
    alignItems: 'center',
    gap: 8,
  },
}));

const ProductEdit: React.FC = () => {
  const { token } = theme.useToken();
  const { styles } = useStyles();
  const { id } = useParams();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();
  const [activeTab, setActiveTab] = useState('basic');

  const isEdit = id && id !== '0';

  // 获取商品分类树形结构
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
    } finally {
      setCategoryLoading(false);
    }
  }, []);

  /**
   * 加载启用的商品标签分组数据。
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

  const fetchProductDetail = useCallback(async () => {
    if (!id) return;
    setDetailLoading(true);
    try {
      const detail: MallProductTypes.MallProductVo = await getMallProductById(id);
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
  }, [form, id, messageApi]);

  useEffect(() => {
    // 加载分类树形数据
    void loadCategoryTree();
    void loadTagGroups();

    if (isEdit) {
      void fetchProductDetail();
    } else {
      // 新增时设置默认值
      form.setFieldsValue({
        status: 1,
        stock: 0,
        sort: 0,
        couponEnabled: 1,
        tagIds: [],
      });
      setSelectedTags([]);
    }
  }, [fetchProductDetail, form, isEdit, loadCategoryTree, loadTagGroups]);

  const handleSubmit = async (values: MallProductTypes.MallProductAddRequest) => {
    setLoading(true);
    try {
      // 将 UploadFile[] 转换为 string[] (images)
      const images = imageListFiles
        .filter((file) => file.status === 'done' && file.url)
        .map((file) => file.url as string);

      const data = {
        ...values,
        images,
        stock: values.stock ?? 0,
        categoryIds: (values.categoryIds || []).filter(Boolean),
        tagIds: (values.tagIds || []).filter(Boolean),
      };

      if (isEdit && id) {
        await updateMallProduct({
          ...data,
          id,
        } as MallProductTypes.MallProductUpdateRequest);
        messageApi.success('更新商品成功');
      } else {
        await addMallProduct(data);
        messageApi.success('添加商品成功');
      }
      navigate(routePaths.mallProductList);
    } catch (error) {
      messageApi.error(isEdit ? '更新商品失败' : '添加商品失败');
      console.error('handleSubmit error:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    navigate(routePaths.mallProductList);
  };

  // 按钮操作函数
  const handlePrevious = () => {
    if (activeTab === 'detail') {
      setActiveTab('basic');
    }
  };

  const handleNext = () => {
    if (activeTab === 'basic') {
      setActiveTab('detail');
    }
  };

  const handleSave = async () => {
    try {
      // 验证所有表单字段，不仅仅是当前活跃的标签页
      const allValues = await form.validateFields();

      // 确保所有字段都有值，为未填写的字段设置默认值
      const completeValues = {
        ...allValues,
        // 基础信息
        name: allValues.name || '',
        categoryIds: allValues.categoryIds || [],
        unit: allValues.unit || '',
        price: allValues.price || '0',

        // 库存信息
        stock: allValues.stock ?? 0,

        // 其他信息 - 设置默认值
        sort: allValues.sort ?? 0,
        deliveryType: allValues.deliveryType ?? 0,
        shippingId: allValues.shippingId || '',
        status: allValues.status ?? 1,
        tagIds: allValues.tagIds || [],
      };

      // 手动触发提交，传入完整的表单数据
      await handleSubmit(completeValues);
    } catch (error) {
      console.error('表单验证失败:', error);
      // 验证失败时，切换到第一个有错误的标签页
      const errorFields = error as any;
      if (errorFields?.errorFields?.length > 0) {
        const firstErrorField = errorFields.errorFields[0];
        const fieldName = firstErrorField.name[0];

        // 根据字段名判断应该切换到哪个标签页
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
        } else if (fieldName === 'drugDetail' || fieldName.startsWith('drugDetail')) {
          setActiveTab('detail');
        }
      }
    }
  };

  // 判断按钮状态
  const isFirstTab = activeTab === 'basic';
  const pageTitle = isEdit ? '编辑商品' : '新增商品';

  const stepItems = [
    {
      key: 'basic',
      title: '基础信息',
      subTitle: '核心参数',
      icon: <InfoCircleOutlined />,
    },
    {
      key: 'detail',
      title: '药品详情',
      subTitle: '详细信息',
      icon: <MedicineBoxOutlined />,
    },
  ];

  const activeStepIndex = stepItems.findIndex((item) => item.key === activeTab);

  return (
    <PageContainer
      title={pageTitle}
      subTitle={isEdit ? `ID: ${id}` : '创建新的商城商品'}
      onBack={() => navigate(routePaths.mallProductList)}
      breadcrumb={{
        items: [{ title: '商城管理' }, { title: pageTitle }],
      }}
    >
      {contextHolder}
      <div className={styles.container}>
        <Spin spinning={detailLoading}>
          {/* 进度指示器 */}
          <div className={styles.stepsWrapper}>
            <Steps
              current={activeStepIndex}
              items={stepItems}
              className={styles.steps}
              size="small"
              onChange={(current) => setActiveTab(stepItems[current].key)}
            />
          </div>

          {/* 表单内容区 */}
          <div className={styles.formContent}>
            <Form form={form} layout="vertical" scrollToFirstError>
              <div className={styles.sectionTitle}>
                {activeTab === 'basic' ? (
                  <>
                    <InfoCircleOutlined style={{ color: token.colorPrimary }} />
                    <Typography.Title level={5} style={{ margin: 0 }}>
                      基础信息配置
                    </Typography.Title>
                  </>
                ) : (
                  <>
                    <MedicineBoxOutlined style={{ color: token.colorPrimary }} />
                    <Typography.Title level={5} style={{ margin: 0 }}>
                      药品详情
                    </Typography.Title>
                  </>
                )}
              </div>
              <Divider style={{ margin: '0 0 24px 0' }} />

              <Tabs
                activeKey={activeTab}
                renderTabBar={() => <div style={{ display: 'none' }} />} // 隐藏 TabBar，使用 Steps 导航
                items={[
                  {
                    key: 'basic',
                    label: '基础信息',
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
                    label: '药品详情',
                    children: <DrugDetail />,
                  },
                ]}
              />
            </Form>
          </div>
        </Spin>
      </div>

      {/* 固定底部按钮 */}
      <div className={styles.footer}>
        <div className={styles.footerContent}>
          <Space>
            <Typography.Text type="secondary">
              当前编辑：
              <Typography.Text strong style={{ color: token.colorPrimary }}>
                {activeTab === 'basic' ? '第一步：基础信息' : '第二步：药品详情'}
              </Typography.Text>
            </Typography.Text>
          </Space>
          <Space size="middle">
            <Button icon={<LeftOutlined />} onClick={handlePrevious} disabled={isFirstTab}>
              上一步
            </Button>

            {isFirstTab ? (
              <Button type="primary" onClick={handleNext} icon={<RightOutlined />}>
                下一步
              </Button>
            ) : (
              <PermissionButton
                type="primary"
                access={
                  isEdit ? ADMIN_PERMISSIONS.mallProduct.edit : ADMIN_PERMISSIONS.mallProduct.add
                }
                onClick={handleSave}
                loading={loading}
                icon={<SaveOutlined />}
              >
                {isEdit ? '保存修改' : '立即创建'}
              </PermissionButton>
            )}

            <Divider type="vertical" />

            <Button onClick={handleCancel}>取消</Button>
          </Space>
        </div>
      </div>
    </PageContainer>
  );
};

export default ProductEdit;
