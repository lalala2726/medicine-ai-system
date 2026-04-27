import {
  DownOutlined,
  KeyOutlined,
  PlusOutlined,
  ReloadOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, Dropdown, Modal, Space, Tag, Typography, message } from 'antd';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import {
  deleteProvider,
  getProviderList,
  getProviderPresetList,
  updateProviderStatus,
  type ModelProviderTypes,
} from '@/api/llm-manage/modelProviders';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { usePermission } from '@/hooks/usePermission';
import { buildModelProviderCreatePath, buildModelProviderEditPath } from '@/router/paths';

import ApiKeyDrawer from './components/ApiKeyDrawer';
import ProviderSourceDrawer from './components/ProviderSourceDrawer';
import { buildProviderListRequest, PRESET_SOURCE, PROVIDER_TYPE_LABELS } from './shared';
import styles from './index.module.less';

const ModelProviders: React.FC = () => {
  const actionRef = useRef<ActionType | null>(null);
  const navigate = useNavigate();
  const { canAccess } = usePermission();
  const [messageApi, contextHolder] = message.useMessage();
  const [sourceDrawerOpen, setSourceDrawerOpen] = useState(false);
  const [presetLoading, setPresetLoading] = useState(false);
  const [presets, setPresets] = useState<ModelProviderTypes.ProviderPresetItem[]>([]);

  // API Key Drawer State
  const [apiKeyDrawerOpen, setApiKeyDrawerOpen] = useState(false);
  const [activeProvider, setActiveProvider] = useState<{ id: string; name: string } | null>(null);

  const loadPresets = useCallback(async () => {
    setPresetLoading(true);
    try {
      const nextPresets = await getProviderPresetList();
      setPresets(nextPresets || []);
    } catch (error) {
      console.error('加载预设提供商失败:', error);
    } finally {
      setPresetLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadPresets();
  }, [loadPresets]);

  const handleOpenCreate = useCallback(() => {
    setSourceDrawerOpen(true);
    if (presets.length === 0 && !presetLoading) {
      void loadPresets();
    }
  }, [loadPresets, presetLoading, presets.length]);

  const handleDelete = useCallback(
    (record: ModelProviderTypes.ProviderListVo) => {
      Modal.confirm({
        title: '确认删除提供商',
        content: `删除后会一并删除「${record.providerName}」下的全部模型，且无法恢复。`,
        okButtonProps: { danger: true },
        okText: '确认删除',
        cancelText: '取消',
        onOk: async () => {
          await deleteProvider(record.id);
          messageApi.success('删除成功');
          actionRef.current?.reload();
        },
      });
    },
    [messageApi],
  );

  const handleStatusChange = useCallback(
    (record: ModelProviderTypes.ProviderListVo) => {
      const isEnabling = record.status !== 1;
      const targetStatus = isEnabling ? 1 : 0;

      Modal.confirm({
        title: isEnabling ? '确认设置为主配置' : '确认取消主配置',
        content: '这将更改系统的模型配置，你确定要这么操作吗？',
        okText: '确认',
        cancelText: '取消',
        onOk: async () => {
          try {
            await updateProviderStatus({ id: record.id, status: targetStatus });
            messageApi.success('操作成功');
            actionRef.current?.reload();
          } catch (error) {
            console.error('修改状态失败:', error);
          }
        },
      });
    },
    [messageApi, actionRef],
  );

  const columns: ProColumns<ModelProviderTypes.ProviderListVo>[] = [
    {
      title: '提供商名称',
      dataIndex: 'providerName',
      width: 180,
      ellipsis: true,
      render: (_, record) => (
        <Space>
          {record.providerName}
          {record.status === 1 && <Tag color="success">主配置</Tag>}
        </Space>
      ),
    },
    {
      title: '类型',
      dataIndex: 'providerType',
      width: 120,
      search: false,
      align: 'center',
      render: (_, record) =>
        record.providerType ? (
          <Tag color="processing">{PROVIDER_TYPE_LABELS[record.providerType]}</Tag>
        ) : (
          '-'
        ),
    },
    {
      title: 'Base URL',
      dataIndex: 'baseUrl',
      width: 260,
      ellipsis: true,
      search: false,
      render: (_, record) => (
        <Typography.Text copyable={{ text: record.baseUrl }} ellipsis={{ tooltip: record.baseUrl }}>
          {record.baseUrl}
        </Typography.Text>
      ),
    },
    {
      title: '模型数量',
      dataIndex: 'modelCount',
      width: 240,
      search: false,
      align: 'center',
      render: (_, record) => (
        <div className={styles.countSummary}>
          <Tag color="blue">总计 {record.modelCount || '0'}</Tag>
          <Tag color="cyan">对话 {record.chatModelCount || '0'}</Tag>
          <Tag color="green">向量 {record.embeddingModelCount || '0'}</Tag>
          <Tag color="purple">重排 {record.rerankModelCount || '0'}</Tag>
        </div>
      ),
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 130,
      search: false,
      align: 'center',
      render: (_, record) => record.updatedAt || '-',
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
          <PermissionButton
            type="link"
            access={ADMIN_PERMISSIONS.llmProvider.update}
            onClick={() => navigate(buildModelProviderEditPath(record.id))}
          >
            编辑
          </PermissionButton>
          <PermissionButton
            type="link"
            access={ADMIN_PERMISSIONS.llmProvider.update}
            onClick={() => handleStatusChange(record)}
          >
            {record.status === 1 ? '取消主配置' : '设置为主配置'}
          </PermissionButton>
          <Dropdown
            menu={{
              items: [
                {
                  key: 'editApiKey',
                  label: '修改 API Key',
                  icon: <KeyOutlined />,
                  disabled: !canAccess(ADMIN_PERMISSIONS.llmProvider.update),
                  onClick: () => {
                    setActiveProvider({ id: record.id, name: record.providerName });
                    setApiKeyDrawerOpen(true);
                  },
                },
                {
                  type: 'divider',
                },
                {
                  key: 'delete',
                  label: '删除',
                  danger: true,
                  icon: <DeleteOutlined />,
                  disabled: !canAccess(ADMIN_PERMISSIONS.llmProvider.delete),
                  onClick: () => handleDelete(record),
                },
              ],
            }}
          >
            <PermissionButton
              type="link"
              access={[ADMIN_PERMISSIONS.llmProvider.update, ADMIN_PERMISSIONS.llmProvider.delete]}
              onClick={(e) => e.preventDefault()}
            >
              更多 <DownOutlined />
            </PermissionButton>
          </Dropdown>
        </TableActionGroup>
      ),
    },
  ];

  return (
    <PageContainer>
      {contextHolder}
      <ProTable<ModelProviderTypes.ProviderListVo, ModelProviderTypes.ProviderListRequest>
        headerTitle="模型提供商"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 88 }}
        columns={columns}
        request={async (params) => {
          const result = await getProviderList(
            buildProviderListRequest(
              params as ModelProviderTypes.ProviderListRequest & {
                current?: number;
              },
            ),
          );
          return {
            data: result?.rows || [],
            success: true,
            total: Number(result?.total || 0),
          };
        }}
        toolBarRender={() => [
          <Button
            key="refresh"
            icon={<ReloadOutlined />}
            onClick={() => actionRef.current?.reload()}
          >
            刷新
          </Button>,
          <PermissionButton
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            access={ADMIN_PERMISSIONS.llmProvider.add}
            onClick={handleOpenCreate}
          >
            新增提供商
          </PermissionButton>,
        ]}
        scroll={{ x: 'max-content' }}
        pagination={{ pageSize: 10 }}
        size="middle"
      />

      <ProviderSourceDrawer
        open={sourceDrawerOpen}
        loading={presetLoading}
        presets={presets}
        onCancel={() => setSourceDrawerOpen(false)}
        onSelectPreset={(providerKey) => {
          setSourceDrawerOpen(false);
          navigate(buildModelProviderCreatePath({ source: PRESET_SOURCE, providerKey }));
        }}
      />

      <ApiKeyDrawer
        open={apiKeyDrawerOpen}
        providerId={activeProvider?.id}
        providerName={activeProvider?.name}
        onClose={() => {
          setApiKeyDrawerOpen(false);
          setTimeout(() => setActiveProvider(null), 300);
        }}
        onSuccess={() => {
          setApiKeyDrawerOpen(false);
          setTimeout(() => setActiveProvider(null), 300);
          actionRef.current?.reload();
        }}
      />
    </PageContainer>
  );
};

export default ModelProviders;
