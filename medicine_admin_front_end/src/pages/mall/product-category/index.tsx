import {
  DeleteOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import type { TableColumnsType } from 'antd';
import { Image, Modal, message, Table } from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import type { MallCategoryTypes } from '@/api/mall/category';
import {
  addCategory,
  deleteCategory,
  getCategoryById,
  tree,
  updateCategory,
} from '@/api/mall/category';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { normalizeRequestUrl } from '@/utils/request/config';
import CategoryForm from './components/CategoryForm';

const CategoryManage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<MallCategoryTypes.MallCategoryTree[]>([]);

  // 表单相关状态
  const [formOpen, setFormOpen] = useState(false);
  const [formTitle, setFormTitle] = useState('');
  const [formLoading, setFormLoading] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<MallCategoryTypes.MallCategoryVo>();

  // 转换树形数据格式
  const transformTreeData = useCallback(
    (items: MallCategoryTypes.MallCategoryTree[]): MallCategoryTypes.MallCategoryTree[] => {
      return items.map((item) => ({
        ...item,
        key: item.id ?? 0,
        children: item.children ? transformTreeData(item.children) : undefined,
      }));
    },
    [],
  );

  // 加载分类数据
  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const response = await tree();
      const transformedData = transformTreeData(response);
      setData(transformedData);
    } catch (error) {
      console.error('加载分类数据失败:', error);
      message.error('加载分类数据失败');
    } finally {
      setLoading(false);
    }
  }, [transformTreeData]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // 表格列定义
  const columns: TableColumnsType<MallCategoryTypes.MallCategoryTree> = [
    {
      title: '分类名称',
      dataIndex: 'name',
      key: 'name',
      render: (text) => text || '-',
    },
    {
      title: '封面',
      dataIndex: 'cover',
      key: 'cover',
      render: (text) =>
        text ? (
          <Image
            src={normalizeRequestUrl(text)}
            width={40}
            height={40}
            style={{ objectFit: 'cover', borderRadius: 4 }}
          />
        ) : (
          '-'
        ),
    },
    {
      title: '分类描述',
      dataIndex: 'description',
      key: 'description',
      render: (text) => text || '-',
    },
    {
      title: '操作',
      key: 'action',
      align: 'center',
      render: (_, record) => (
        <TableActionGroup>
          <PermissionButton
            type="link"
            icon={<PlusOutlined />}
            access={ADMIN_PERMISSIONS.mallCategory.add}
            onClick={() => handleAdd(record)}
            size="small"
          >
            新增子分类
          </PermissionButton>
          <PermissionButton
            type="link"
            icon={<EditOutlined />}
            access={ADMIN_PERMISSIONS.mallCategory.edit}
            onClick={() => handleEdit(record)}
            size="small"
          >
            编辑
          </PermissionButton>
          <PermissionButton
            type="link"
            danger
            icon={<DeleteOutlined />}
            access={ADMIN_PERMISSIONS.mallCategory.delete}
            onClick={() => handleDelete([record.id as string])}
            size="small"
          >
            删除
          </PermissionButton>
        </TableActionGroup>
      ),
    },
  ];

  // 新增分类
  const handleAdd = (parentRecord?: MallCategoryTypes.MallCategoryTree) => {
    setFormTitle(parentRecord ? `新增子分类（上级：${parentRecord.name}）` : '新增分类');
    setCurrentRecord(
      parentRecord
        ? ({
            parentId: String(parentRecord.id),
          } as MallCategoryTypes.MallCategoryVo)
        : undefined,
    );
    setFormOpen(true);
  };

  // 编辑分类
  const handleEdit = async (record: MallCategoryTypes.MallCategoryTree) => {
    try {
      setFormLoading(true);
      const response = await getCategoryById(String(record.id));
      setCurrentRecord(response);
      setFormTitle(`编辑分类：${record.name}`);
      setFormOpen(true);
    } catch (error) {
      console.error('获取分类详情失败:', error);
      message.error('获取分类详情失败');
    } finally {
      setFormLoading(false);
    }
  };

  // 删除分类
  const handleDelete = (ids: string[]) => {
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除选中的 ${ids.length} 个分类吗？删除后不可恢复。`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          await deleteCategory(ids);
          message.success('删除成功');
          await loadData();
        } catch (error) {
          console.error('删除分类失败:', error);
          message.error('删除分类失败');
        }
      },
    });
  };

  // 表单提交
  const handleFormSubmit = async (
    values: MallCategoryTypes.MallCategoryAddRequest | MallCategoryTypes.MallCategoryUpdateRequest,
  ) => {
    try {
      setFormLoading(true);
      if (currentRecord?.id) {
        // 编辑
        await updateCategory(values as MallCategoryTypes.MallCategoryUpdateRequest);
        message.success('编辑成功');
      } else {
        // 新增
        await addCategory(values as MallCategoryTypes.MallCategoryAddRequest);
        message.success('新增成功');
      }
      setFormOpen(false);
      await loadData();
    } catch (error) {
      console.error('保存分类失败:', error);
      message.error('保存分类失败');
    } finally {
      setFormLoading(false);
    }
  };

  return (
    <PageContainer title="商品分类">
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <div style={{ marginBottom: 16 }}>
          <PermissionButton
            type="primary"
            icon={<PlusOutlined />}
            access={ADMIN_PERMISSIONS.mallCategory.add}
            onClick={() => handleAdd()}
          >
            新增分类
          </PermissionButton>
        </div>

        {/* 表格 */}
        <div style={{ flex: 1 }}>
          <Table<MallCategoryTypes.MallCategoryTree>
            columns={columns}
            dataSource={data}
            loading={loading}
            pagination={false}
            expandable={{
              defaultExpandAllRows: false,
            }}
            size="middle"
            rowKey="id"
          />
        </div>
      </div>

      {/* 表单弹窗 */}
      <CategoryForm
        open={formOpen}
        title={formTitle}
        record={currentRecord}
        loading={formLoading}
        onSubmit={handleFormSubmit}
        onCancel={() => setFormOpen(false)}
      />
    </PageContainer>
  );
};

export default CategoryManage;
