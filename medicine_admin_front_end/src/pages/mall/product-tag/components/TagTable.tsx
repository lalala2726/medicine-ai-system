import { PlusOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Modal, Switch, message } from 'antd';
import React from 'react';
import {
  deleteProductTag,
  getProductTagById,
  listProductTags,
  updateProductTagStatus,
  type MallProductTagTypes,
} from '@/api/mall/productTag';
import { listProductTagTypeOptions, type MallProductTagTypeTypes } from '@/api/mall/productTagType';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { usePermission } from '@/hooks/usePermission';
import TagForm from './TagForm';

interface TagTableProps {
  /** 固定标签类型ID */
  fixedTypeId?: string;
  /** 固定标签类型名称 */
  fixedTypeName?: string;
}

/**
 * 商品标签表格。
 */
const TagTable: React.FC<TagTableProps> = ({ fixedTypeId, fixedTypeName }) => {
  const actionRef = React.useRef<ActionType | null>(null);
  const [createOpen, setCreateOpen] = React.useState(false);
  const [updateOpen, setUpdateOpen] = React.useState(false);
  const [currentRow, setCurrentRow] = React.useState<MallProductTagTypes.MallProductTagAdminVo>();
  const [typeOptions, setTypeOptions] = React.useState<
    MallProductTagTypeTypes.MallProductTagTypeVo[]
  >([]);
  const [editingTagId, setEditingTagId] = React.useState<string | null>(null);
  const { canAccess } = usePermission();

  /**
   * 加载可用的标签类型下拉数据。
   */
  const loadTypeOptions = React.useCallback(async () => {
    try {
      const data = await listProductTagTypeOptions();
      setTypeOptions(data || []);
    } catch {
      message.error('加载标签类型下拉失败');
    }
  }, []);

  React.useEffect(() => {
    void loadTypeOptions();
  }, [loadTypeOptions]);

  /**
   * 刷新当前表格。
   */
  const reloadTable = React.useCallback(() => {
    actionRef.current?.reload();
  }, []);

  /**
   * 处理标签状态切换。
   *
   * @param record 当前记录
   * @param checked 开关状态
   */
  const handleStatusChange = React.useCallback(
    async (record: MallProductTagTypes.MallProductTagAdminVo, checked: boolean) => {
      if (!record.id) {
        return;
      }

      try {
        await updateProductTagStatus({
          id: record.id,
          status: checked ? 1 : 0,
        });
        message.success('商品标签状态更新成功');
        reloadTable();
      } catch {
        message.error('商品标签状态更新失败，请重试');
      }
    },
    [reloadTable],
  );

  /**
   * 处理删除商品标签。
   *
   * @param record 当前记录
   */
  const handleDelete = React.useCallback(
    (record: MallProductTagTypes.MallProductTagAdminVo) => {
      if (!record.id) {
        return;
      }

      Modal.confirm({
        title: '确认删除商品标签',
        content: `确定要删除标签“${record.name || '-'}”吗？`,
        okButtonProps: { danger: true },
        okText: '确认删除',
        cancelText: '取消',
        onOk: async () => {
          try {
            await deleteProductTag(record.id!);
            message.success('商品标签删除成功');
            reloadTable();
          } catch {
            message.error('商品标签删除失败，请重试');
          }
        },
      });
    },
    [reloadTable],
  );

  /**
   * 处理编辑商品标签。
   * 编辑前根据标签 ID 拉取详情，避免使用列表行数据。
   * @param record 当前记录。
   * @returns 无返回值。
   */
  const handleEdit = React.useCallback(
    async (record: MallProductTagTypes.MallProductTagAdminVo) => {
      if (!record.id) {
        message.warning('缺少标签ID，无法编辑');
        return;
      }

      try {
        setEditingTagId(record.id);
        const detail = await getProductTagById(record.id);
        setCurrentRow(detail);
        setUpdateOpen(true);
      } catch {
        message.error('加载商品标签详情失败，请重试');
      } finally {
        setEditingTagId(null);
      }
    },
    [],
  );

  const columns = React.useMemo<ProColumns<MallProductTagTypes.MallProductTagAdminVo>[]>(
    () => [
      {
        title: '标签名称',
        dataIndex: 'name',
        width: 180,
      },
      {
        title: '所属类型',
        dataIndex: 'typeId',
        width: 180,
        hideInSearch: Boolean(fixedTypeId),
        valueType: 'select',
        fieldProps: {
          options: typeOptions.map((option) => ({
            label: `${option.name}${option.code ? ` (${option.code})` : ''}`,
            value: option.id,
          })),
          allowClear: true,
          showSearch: true,
        },
        render: (_, record) => fixedTypeName || record.typeName || '-',
      },
      {
        title: '状态',
        dataIndex: 'status',
        width: 120,
        valueType: 'select',
        valueEnum: {
          1: { text: '启用', status: 'Success' },
          0: { text: '禁用', status: 'Default' },
        },
        render: (_, record) => (
          <Switch
            checked={record.status === 1}
            checkedChildren="启用"
            unCheckedChildren="禁用"
            disabled={!canAccess(ADMIN_PERMISSIONS.mallProductTag.edit)}
            onChange={(checked) => handleStatusChange(record, checked)}
          />
        ),
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        valueType: 'dateTime',
        width: 180,
        hideInSearch: true,
      },
      {
        title: '更新时间',
        dataIndex: 'updateTime',
        valueType: 'dateTime',
        width: 180,
        hideInSearch: true,
      },
      {
        title: '操作',
        dataIndex: 'option',
        valueType: 'option',
        width: 160,
        fixed: 'right',
        align: 'center',
        render: (_, record) => (
          <TableActionGroup>
            <PermissionButton
              type="link"
              access={ADMIN_PERMISSIONS.mallProductTag.edit}
              loading={record.id === editingTagId}
              onClick={() => {
                void handleEdit(record);
              }}
            >
              编辑
            </PermissionButton>
            <PermissionButton
              type="link"
              danger
              access={ADMIN_PERMISSIONS.mallProductTag.delete}
              onClick={() => handleDelete(record)}
            >
              删除
            </PermissionButton>
          </TableActionGroup>
        ),
      },
    ],
    [
      canAccess,
      editingTagId,
      fixedTypeId,
      fixedTypeName,
      handleDelete,
      handleEdit,
      handleStatusChange,
      typeOptions,
    ],
  );

  return (
    <>
      <ProTable<
        MallProductTagTypes.MallProductTagAdminVo,
        MallProductTagTypes.MallProductTagListQueryRequest
      >
        headerTitle={fixedTypeName ? `${fixedTypeName}标签列表` : '商品标签列表'}
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 96 }}
        request={async (params) => {
          try {
            const data = await listProductTags({
              name: params.name,
              typeId: fixedTypeId || params.typeId,
              status: params.status,
              pageNum: params.current,
              pageSize: params.pageSize,
            });

            return {
              data: data?.rows || [],
              success: true,
              total: Number(data?.total || 0),
            };
          } catch {
            return {
              data: [],
              success: false,
              total: 0,
            };
          }
        }}
        columns={columns}
        toolBarRender={() => [
          <PermissionButton
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            access={ADMIN_PERMISSIONS.mallProductTag.add}
            onClick={() => setCreateOpen(true)}
          >
            新建商品标签
          </PermissionButton>,
        ]}
      />

      <TagForm
        open={createOpen}
        onOpenChange={setCreateOpen}
        typeOptions={typeOptions}
        fixedTypeId={fixedTypeId}
        lockTypeSelection={Boolean(fixedTypeId)}
        onFinish={() => {
          setCreateOpen(false);
          reloadTable();
        }}
      />

      <TagForm
        open={updateOpen}
        onOpenChange={(open) => {
          setUpdateOpen(open);
          if (!open) {
            setCurrentRow(undefined);
          }
        }}
        initialValues={currentRow}
        typeOptions={typeOptions}
        fixedTypeId={fixedTypeId}
        lockTypeSelection={Boolean(fixedTypeId)}
        onFinish={() => {
          setUpdateOpen(false);
          setCurrentRow(undefined);
          reloadTable();
        }}
      />
    </>
  );
};

export default TagTable;
