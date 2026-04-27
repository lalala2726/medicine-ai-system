import type { ActionType, ProColumns, ProFormInstance } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { useNavigate } from 'react-router-dom';
import { FilterOutlined } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import {
  Badge,
  Button,
  Collapse,
  Image,
  message,
  Modal,
  Space,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import React, { useCallback, useMemo, useRef, useState } from 'react';
import { flushSync } from 'react-dom';
import { tree as getCategoryTree, type MallCategoryTypes } from '@/api/mall/category';
import { deleteMallProduct, listMallProduct, type MallProductTypes } from '@/api/mall/product';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import ProductTagFlatSelect from '@/pages/mall/components/ProductTagFlatSelect';
import {
  loadEnabledProductTagSelectGroups,
  type ProductTagSelectGroup,
} from '@/pages/mall/components/productTagUtils';
import { buildMallProductEditPath, routePaths } from '@/router/paths';
import ProductDetailDrawer from './components/ProductDetailDrawer';

type ProductRecord = MallProductTypes.MallProductListVo;
type ProductQuery = MallProductTypes.MallProductListQueryRequest & {
  pageNum?: number;
  pageSize?: number;
};

const MallProductListPage: React.FC = () => {
  const actionRef = useRef<ActionType | null>(null);
  const formRef = useRef<ProFormInstance>(undefined);
  const navigate = useNavigate();
  const [selectedRowsState, setSelectedRows] = useState<ProductRecord[]>([]);
  const [messageApi, contextHolder] = message.useMessage();
  const [activeStatus, setActiveStatus] = useState<number | undefined>(undefined);

  // 详情抽屉相关状态
  const [detailDrawerVisible, setDetailDrawerVisible] = useState(false);
  const [detailProductId, setDetailProductId] = useState<string>();

  // 获取商品分类树形结构
  const [categoryTree, setCategoryTree] = useState<MallCategoryTypes.MallCategoryTree[]>([]);
  const [categoryLoading, setCategoryLoading] = useState(false);
  const [tagGroups, setTagGroups] = useState<ProductTagSelectGroup[]>([]);
  const [tagGroupLoading, setTagGroupLoading] = useState(false);
  /** 当前已选中的商品标签筛选 ID 列表。 */
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>([]);

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
  const treeSelectData = useMemo(() => {
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

  // 初始化时加载分类数据
  React.useEffect(() => {
    void loadCategoryTree();
    void loadTagGroups();
  }, [loadCategoryTree, loadTagGroups]);

  const { run: deleteRun, loading: deleteLoading } = useRequest(deleteMallProduct, {
    manual: true,
    onSuccess: () => {
      actionRef.current?.reload();
      messageApi.success('删除成功');
    },
  });

  const handleAdd = useCallback(() => {
    navigate(routePaths.mallProductCreate);
  }, [navigate]);

  const handleEdit = useCallback(
    (record: ProductRecord) => {
      if (!record.id) {
        messageApi.warning('缺少商品ID，无法编辑');
        return;
      }
      navigate(buildMallProductEditPath(record.id));
    },
    [messageApi, navigate],
  );

  // 查看详情
  const handleViewDetail = useCallback(
    (record: ProductRecord) => {
      if (!record.id) {
        messageApi.warning('缺少商品ID，无法查看详情');
        return;
      }
      setDetailProductId(record.id);
      setDetailDrawerVisible(true);
    },
    [messageApi],
  );

  // 关闭详情抽屉
  const handleDetailDrawerClose = useCallback(() => {
    setDetailDrawerVisible(false);
    setDetailProductId(undefined);
  }, []);

  const handleRemove = useCallback(
    (rows: ProductRecord[]) => {
      if (!rows.length) {
        messageApi.warning('请选择要删除的商品');
        return;
      }
      Modal.confirm({
        title: '确认删除',
        content: `确定要删除选中的 ${rows.length} 个商品吗？`,
        onOk: () => {
          const ids = rows.map((item) => item.id).filter((id): id is string => Boolean(id));
          if (!ids.length) {
            messageApi.warning('未找到有效的商品ID');
            return;
          }
          return deleteRun(ids);
        },
      });
    },
    [deleteRun, messageApi],
  );

  /**
   * 处理商品标签筛选变化。
   * 选择标签后直接复用当前搜索条件刷新表格数据。
   * @param nextTagIds 最新选中的商品标签 ID 列表。
   * @returns 无返回值。
   */
  const handleTagFilterChange = useCallback((nextTagIds: string[]) => {
    setSelectedTagIds(nextTagIds);
  }, []);

  /**
   * 处理商品列表查询重置。
   * 同步清空顶部查询条件与商品标签筛选条件，并刷新表格数据。
   * @returns 无返回值。
   */
  const handleSearchReset = useCallback(() => {
    formRef.current?.resetFields();
    flushSync(() => {
      setSelectedTagIds([]);
    });
    actionRef.current?.reload();
  }, []);

  const columns: ProColumns<ProductRecord>[] = useMemo(
    () => [
      {
        title: '商品封面',
        dataIndex: 'coverImage',
        width: 80,
        hideInSearch: true,
        render: (text) => {
          const src = typeof text === 'string' ? text : undefined;
          return src ? (
            <Image
              width={40}
              height={40}
              src={src}
              alt="商品图片"
              style={{ objectFit: 'cover' }}
              preview={{
                mask: '预览',
              }}
            />
          ) : (
            '-'
          );
        },
      },

      {
        title: '商品名称',
        dataIndex: 'name',
        ellipsis: true,
        width: 120,
        colSize: 1,
      },
      {
        title: '商品分类',
        dataIndex: 'categoryId',
        hideInTable: true,
        width: 80,
        valueType: 'treeSelect',
        colSize: 1,
        fieldProps: {
          treeData: treeSelectData,
          showSearch: true,
          treeNodeFilterProp: 'title',
          placeholder: '请选择商品分类',
          allowClear: true,
          treeDefaultExpandAll: false,
          loading: categoryLoading,
          styles: { popup: { root: { maxHeight: 400, overflow: 'auto' } } },
        },
      },
      {
        title: '分类',
        dataIndex: 'categoryNames',
        width: 120,
        hideInSearch: true,
        render: (_, record) =>
          record.categoryNames && record.categoryNames.length > 0 ? (
            <Space size={[4, 4]} wrap>
              {record.categoryNames.map((categoryName) => (
                <Tag key={categoryName} color="blue" style={{ margin: 0 }}>
                  {categoryName}
                </Tag>
              ))}
            </Space>
          ) : (
            '-'
          ),
      },
      {
        title: '标签',
        dataIndex: 'tagNames',
        width: 220,
        hideInSearch: true,
        render: (_, record) => {
          const tagList = record.tags?.length
            ? record.tags.map((tag) => tag.name).filter(Boolean)
            : record.tagNames || [];

          if (!tagList.length) {
            return '-';
          }

          const maxDisplayCount = 4;
          const displayTags = tagList.slice(0, maxDisplayCount);
          const restTags = tagList.slice(maxDisplayCount);

          return (
            <div style={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: '4px 0' }}>
              <Space size={[4, 4]} wrap>
                {displayTags.map((tagName) => (
                  <Tag
                    key={`${record.id}-${tagName}`}
                    color="processing"
                    style={{
                      margin: 0,
                      maxWidth: 82,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                      verticalAlign: 'middle',
                    }}
                    title={tagName}
                  >
                    {tagName}
                  </Tag>
                ))}
                {restTags.length > 0 && (
                  <Tooltip
                    title={
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, padding: 4 }}>
                        {restTags.map((t) => (
                          <Tag
                            key={`${record.id}-rest-${t}`}
                            color="processing"
                            style={{ margin: 0 }}
                          >
                            {t}
                          </Tag>
                        ))}
                      </div>
                    }
                    color="var(--ant-color-bg-elevated, var(--ant-color-bg-container, #fff))"
                  >
                    <Tag style={{ margin: 0, cursor: 'pointer', verticalAlign: 'middle' }}>
                      +{restTags.length}
                    </Tag>
                  </Tooltip>
                )}
              </Space>
            </div>
          );
        },
      },
      {
        title: '单位',
        dataIndex: 'unit',
        width: 80,
        hideInSearch: true,
      },
      {
        title: '售价',
        dataIndex: 'price',
        width: 100,
        hideInSearch: true,
        render: (text) => (text ? `¥${Number(text).toFixed(2)}` : '-'),
      },
      {
        title: '销量',
        dataIndex: 'sales',
        width: 80,
        hideInSearch: true,
      },
      {
        title: '库存',
        dataIndex: 'stock',
        width: 80,
        hideInSearch: true,
      },
      {
        title: '状态',
        dataIndex: 'status',
        width: 80,
        hideInSearch: true,
        render: (status) => {
          if (status === 1) {
            return <Tag color="success">上架</Tag>;
          }
          return <Tag color="default">下架</Tag>;
        },
      },
      {
        title: '优惠券',
        dataIndex: 'couponEnabled',
        width: 96,
        hideInSearch: true,
        render: (couponEnabled) => {
          if (Number(couponEnabled) === 1) {
            return <Tag color="success">允许</Tag>;
          }
          return <Tag color="warning">禁用</Tag>;
        },
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        valueType: 'dateTime',
        width: 160,
        hideInSearch: true,
      },
      {
        title: '操作',
        dataIndex: 'option',
        valueType: 'option',
        width: 180,
        fixed: 'right',
        align: 'center',
        render: (_, record) => (
          <TableActionGroup>
            <PermissionButton
              type="link"
              access={ADMIN_PERMISSIONS.mallProduct.query}
              onClick={() => handleViewDetail(record)}
            >
              详情
            </PermissionButton>
            <PermissionButton
              type="link"
              access={ADMIN_PERMISSIONS.mallProduct.edit}
              onClick={() => handleEdit(record)}
            >
              编辑
            </PermissionButton>
            <PermissionButton
              type="link"
              danger
              access={ADMIN_PERMISSIONS.mallProduct.delete}
              onClick={() => {
                if (!record.id) {
                  messageApi.warning('缺少商品ID，无法删除');
                  return;
                }
                Modal.confirm({
                  title: '确认删除',
                  content: `确定要删除商品 "${record.name}" 吗？`,
                  onOk: () => deleteRun([record.id ?? '']),
                });
              }}
            >
              删除
            </PermissionButton>
          </TableActionGroup>
        ),
      },
    ],
    [treeSelectData, categoryLoading, deleteRun, handleEdit, handleViewDetail, messageApi],
  );

  // 状态标签配置
  const statusTabs = [
    { label: '全部', key: 'all', value: undefined },
    { label: '上架中', key: 'online', value: 1 },
    { label: '已下架', key: 'offline', value: 0 },
  ];

  return (
    <PageContainer>
      {contextHolder}

      <Tabs
        activeKey={activeStatus === undefined ? 'all' : activeStatus === 1 ? 'online' : 'offline'}
        onChange={(key) => {
          const tab = statusTabs.find((t) => t.key === key);
          setActiveStatus(tab?.value);
          actionRef.current?.reload();
        }}
        items={statusTabs}
        style={{ marginBottom: 16 }}
      />

      <ProTable<ProductRecord, ProductQuery>
        headerTitle="商品列表"
        actionRef={actionRef}
        formRef={formRef}
        params={{
          tagIds: selectedTagIds,
        }}
        rowKey="id"
        search={{
          labelWidth: 'auto',
          defaultCollapsed: false,
          optionRender: (_, __, dom) => [
            <Button key="reset" onClick={handleSearchReset}>
              重置
            </Button>,
            dom[1],
          ],
        }}
        form={{
          onReset: () => {
            setSelectedTagIds([]);
          },
        }}
        request={async (params) => {
          const { current, pageSize, tagIds, ...rest } = params as ProductQuery & {
            current?: number;
            pageSize?: number;
            tagIds?: string[];
          };
          const currentPage = Number(current ?? 1);
          const pageSizeValue = Number(pageSize ?? 10);
          const query: ProductQuery = {
            ...(rest as ProductQuery),
            status: activeStatus,
            tagIds: tagIds?.length ? tagIds : undefined,
            pageNum: currentPage,
            pageSize: pageSizeValue,
          };

          const result = await listMallProduct(query);
          const total = Number(result?.total ?? 0);

          return {
            data: result?.rows ?? [],
            success: true,
            total,
          };
        }}
        columns={columns}
        pagination={{
          showQuickJumper: true,
          showSizeChanger: true,
          defaultPageSize: 10,
        }}
        rowSelection={{
          onChange: (_, rows) => setSelectedRows(rows),
        }}
        toolBarRender={() => [
          <PermissionButton
            key="add"
            type="primary"
            access={ADMIN_PERMISSIONS.mallProduct.add}
            onClick={handleAdd}
          >
            添加商品
          </PermissionButton>,
          selectedRowsState.length > 0 ? (
            <PermissionButton
              key="batchDelete"
              danger
              loading={deleteLoading}
              access={ADMIN_PERMISSIONS.mallProduct.delete}
              onClick={() => handleRemove(selectedRowsState)}
            >
              批量删除
            </PermissionButton>
          ) : null,
        ]}
        tableExtraRender={() => (
          <div
            style={{
              backgroundColor: 'var(--ant-color-bg-container, #fff)',
              padding: '0 24px 8px',
              borderBottom: '1px solid var(--ant-color-border-secondary, #f0f0f0)',
            }}
          >
            <Collapse
              ghost
              expandIconPosition="end"
              items={[
                {
                  key: 'tag-filter',
                  label: (
                    <Space size={4}>
                      <FilterOutlined style={{ color: '#1890ff' }} />
                      <Typography.Text strong>商品标签筛选</Typography.Text>
                      {selectedTagIds.length > 0 && (
                        <Badge
                          count={selectedTagIds.length}
                          style={{ backgroundColor: '#1890ff', marginLeft: 4 }}
                        />
                      )}
                    </Space>
                  ),
                  children: (
                    <div style={{ padding: '0 12px 12px' }}>
                      <ProductTagFlatSelect
                        value={selectedTagIds}
                        onChange={handleTagFilterChange}
                        groups={tagGroups}
                        emptyText={tagGroupLoading ? '正在加载标签数据' : '暂无可筛选标签'}
                      />
                    </div>
                  ),
                },
              ]}
            />
          </div>
        )}
      />

      {/* 商品详情抽屉 */}
      <ProductDetailDrawer
        visible={detailDrawerVisible}
        productId={detailProductId}
        onClose={handleDetailDrawerClose}
      />
    </PageContainer>
  );
};

export default MallProductListPage;
