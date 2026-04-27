import { PlusOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Modal, Switch, message } from 'antd';
import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  deleteProductTagType,
  getProductTagTypeById,
  listProductTagTypes,
  updateProductTagTypeStatus,
  type MallProductTagTypeTypes,
} from '@/api/mall/productTagType';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { usePermission } from '@/hooks/usePermission';
import { buildMallProductTagByTypePath } from '@/router/paths';
import TagTypeForm from './TagTypeForm';

/**
 * 商品标签类型表格。
 */
const TagTypeTable: React.FC = () => {
  const actionRef = React.useRef<ActionType | null>(null);
  const navigate = useNavigate();
  const [createOpen, setCreateOpen] = React.useState(false);
  const [updateOpen, setUpdateOpen] = React.useState(false);
  const [currentRow, setCurrentRow] =
    React.useState<MallProductTagTypeTypes.MallProductTagTypeAdminVo>();
  const [editingTypeId, setEditingTypeId] = React.useState<string | null>(null);
  const { canAccess } = usePermission();

  /**
   * 刷新表格并通知外层类型列表变更。
   */
  const reloadTable = React.useCallback(() => {
    actionRef.current?.reload();
  }, []);

  /**
   * 处理标签类型状态切换。
   *
   * @param record 当前记录
   * @param checked 开关状态
   */
  const handleStatusChange = React.useCallback(
    async (record: MallProductTagTypeTypes.MallProductTagTypeAdminVo, checked: boolean) => {
      if (!record.id) {
        return;
      }

      try {
        await updateProductTagTypeStatus({
          id: record.id,
          status: checked ? 1 : 0,
        });
        message.success('标签类型状态更新成功');
        reloadTable();
      } catch {
        message.error('标签类型状态更新失败，请重试');
      }
    },
    [reloadTable],
  );

  /**
   * 处理删除标签类型。
   *
   * @param record 当前记录
   */
  const handleDelete = React.useCallback(
    (record: MallProductTagTypeTypes.MallProductTagTypeAdminVo) => {
      if (!record.id) {
        return;
      }

      Modal.confirm({
        title: '确认删除标签类型',
        content: `确定要删除标签类型“${record.name || '-'}”吗？`,
        okButtonProps: { danger: true },
        okText: '确认删除',
        cancelText: '取消',
        onOk: async () => {
          try {
            await deleteProductTagType(record.id!);
            message.success('标签类型删除成功');
            reloadTable();
          } catch {
            message.error('标签类型删除失败，请重试');
          }
        },
      });
    },
    [reloadTable],
  );

  /**
   * 处理编辑标签类型。
   * 编辑前根据类型 ID 拉取详情，避免使用列表行数据。
   * @param record 当前记录。
   * @returns 无返回值。
   */
  const handleEdit = React.useCallback(
    async (record: MallProductTagTypeTypes.MallProductTagTypeAdminVo) => {
      if (!record.id) {
        message.warning('缺少标签类型ID，无法编辑');
        return;
      }

      try {
        setEditingTypeId(record.id);
        const detail = await getProductTagTypeById(record.id);
        setCurrentRow(detail);
        setUpdateOpen(true);
      } catch {
        message.error('加载标签类型详情失败，请重试');
      } finally {
        setEditingTypeId(null);
      }
    },
    [],
  );

  const columns = React.useMemo<ProColumns<MallProductTagTypeTypes.MallProductTagTypeAdminVo>[]>(
    () => [
      {
        title: '类型名称',
        dataIndex: 'name',
        width: 180,
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
        width: 220,
        fixed: 'right',
        align: 'center',
        render: (_, record) => (
          <TableActionGroup>
            <a
              onClick={() => {
                if (!record.id) {
                  return;
                }
                navigate(
                  buildMallProductTagByTypePath({
                    typeId: record.id,
                    typeName: record.name,
                  }),
                );
              }}
            >
              查看标签
            </a>
            <PermissionButton
              type="link"
              access={ADMIN_PERMISSIONS.mallProductTag.edit}
              loading={record.id === editingTypeId}
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
    [canAccess, editingTypeId, handleDelete, handleEdit, handleStatusChange, navigate],
  );

  return (
    <>
      <ProTable<
        MallProductTagTypeTypes.MallProductTagTypeAdminVo,
        MallProductTagTypeTypes.MallProductTagTypeListQueryRequest
      >
        headerTitle="标签类型列表"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 96 }}
        request={async (params) => {
          try {
            const data = await listProductTagTypes({
              name: params.name,
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
            新建标签类型
          </PermissionButton>,
        ]}
      />

      <TagTypeForm
        open={createOpen}
        onOpenChange={setCreateOpen}
        onFinish={() => {
          setCreateOpen(false);
          reloadTable();
        }}
      />

      <TagTypeForm
        open={updateOpen}
        onOpenChange={(open) => {
          setUpdateOpen(open);
          if (!open) {
            setCurrentRow(undefined);
          }
        }}
        initialValues={currentRow}
        onFinish={() => {
          setUpdateOpen(false);
          setCurrentRow(undefined);
          reloadTable();
        }}
      />
    </>
  );
};

export default TagTypeTable;
