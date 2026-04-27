import { ArrowLeftOutlined, EyeOutlined, ReloadOutlined, UndoOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, Popconfirm, Tag, Typography, message } from 'antd';
import React, { useCallback, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import {
  getPromptHistory,
  rollbackPromptConfig,
  type SystemModelTypes,
} from '@/api/llm-manage/systemModels';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { routePaths } from '@/router/paths';
import PromptPreviewDrawer from '../PromptPreviewDrawer';
import './index.less';

/**
 * 格式化时间文案。
 *
 * @param rawValue 原始时间值。
 * @returns 格式化后的时间文案。
 */
function formatDateTime(rawValue?: string) {
  if (!rawValue) {
    return '-';
  }
  const date = new Date(rawValue);
  if (Number.isNaN(date.getTime())) {
    return rawValue;
  }
  return date.toLocaleString();
}

/**
 * 提示词历史页。
 */
const PromptHistoryPage: React.FC = () => {
  const actionRef = useRef<ActionType | null>(null);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const promptKey = String(searchParams.get('promptKey') || '').trim();
  const [previewRecord, setPreviewRecord] = useState<SystemModelTypes.PromptHistoryItem>();
  const [rollbackVersion, setRollbackVersion] = useState<number>();

  /**
   * 统一提取错误文案。
   *
   * @param error 原始异常对象。
   * @param fallback 默认错误提示。
   * @returns 可展示给用户的错误文案。
   */
  const getErrorMessage = useCallback((error: unknown, fallback: string) => {
    if (error instanceof Error && error.message) {
      return error.message;
    }
    return fallback;
  }, []);

  /**
   * 执行指定版本回滚。
   *
   * @param targetVersion 目标版本号。
   */
  const handleRollback = useCallback(
    async (targetVersion: number) => {
      if (!promptKey || targetVersion <= 0) {
        return;
      }
      setRollbackVersion(targetVersion);
      try {
        await rollbackPromptConfig({
          promptKey,
          targetVersion,
        });
        message.success(`已回滚到 v${targetVersion}（并生成新版本）`);
        actionRef.current?.reload();
      } catch (error) {
        message.error(getErrorMessage(error, '回滚失败'));
      } finally {
        setRollbackVersion(undefined);
      }
    },
    [getErrorMessage, promptKey],
  );

  /**
   * 历史表格列定义。
   */
  const columns: ProColumns<SystemModelTypes.PromptHistoryItem>[] = [
    {
      title: '版本',
      dataIndex: 'promptVersion',
      width: 120,
      align: 'center',
      render: (_, record) =>
        record.promptVersion ? <Tag bordered={false}>v{record.promptVersion}</Tag> : '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 200,
      render: (_, record) => formatDateTime(record.createdAt),
    },
    {
      title: '创建人',
      dataIndex: 'createdBy',
      width: 160,
      render: (_, record) => record.createdBy || '-',
    },
    {
      title: '内容预览',
      dataIndex: 'promptContent',
      ellipsis: true,
      render: (_, record) => (
        <Typography.Paragraph ellipsis={{ rows: 2, expandable: false }} style={{ marginBottom: 0 }}>
          {record.promptContent || '-'}
        </Typography.Paragraph>
      ),
    },
    {
      title: '操作',
      dataIndex: 'option',
      valueType: 'option',
      width: 220,
      fixed: 'right',
      align: 'center',
      render: (_, record) => {
        const targetVersion = Number(record.promptVersion || 0);
        const canRollback = targetVersion > 0;
        return (
          <TableActionGroup>
            <Button type="link" icon={<EyeOutlined />} onClick={() => setPreviewRecord(record)}>
              预览
            </Button>
            <Popconfirm
              title={`确认回滚到 v${targetVersion} 吗？`}
              description="回滚会创建新的当前版本。"
              okText="确认回滚"
              cancelText="取消"
              disabled={!canRollback}
              onConfirm={() => handleRollback(targetVersion)}
            >
              <PermissionButton
                type="link"
                icon={<UndoOutlined />}
                access={ADMIN_PERMISSIONS.agentPrompt.rollback}
                disabled={!canRollback}
                loading={rollbackVersion === targetVersion}
              >
                回滚
              </PermissionButton>
            </Popconfirm>
          </TableActionGroup>
        );
      },
    },
  ];

  return (
    <PageContainer
      className="prompt-manage-history-page"
      header={{
        title: '提示词历史',
        extra: [
          <Button
            key="back"
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate(routePaths.llmPromptManage)}
          >
            返回提示词管理
          </Button>,
        ],
      }}
    >
      <ProTable<SystemModelTypes.PromptHistoryItem>
        headerTitle="历史版本列表"
        actionRef={actionRef}
        rowKey={(record) => `${record.promptVersion || ''}-${record.createdAt || ''}`}
        search={false}
        columns={columns}
        request={async () => {
          if (!promptKey) {
            return {
              data: [],
              success: true,
              total: 0,
            };
          }
          const data = (await getPromptHistory(promptKey)) || [];
          return {
            data,
            success: true,
            total: data.length,
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
        ]}
        pagination={false}
      />

      <PromptPreviewDrawer
        open={Boolean(previewRecord)}
        promptKey={promptKey}
        promptVersion={previewRecord?.promptVersion}
        promptContent={previewRecord?.promptContent}
        onClose={() => setPreviewRecord(undefined)}
      />
    </PageContainer>
  );
};

export default PromptHistoryPage;
