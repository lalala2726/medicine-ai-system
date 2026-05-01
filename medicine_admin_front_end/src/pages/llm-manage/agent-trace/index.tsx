import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Modal, Tag, message } from 'antd';
import React, { useCallback, useRef, useState } from 'react';
import { deleteAgentTrace, listAgentTraceRuns, type AgentTraceTypes } from '@/api/agent/trace';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { useAgentObservabilitySecondaryMenu } from '@/pages/llm-manage/agent-observability/shared';
import AgentTraceDetailDrawer from './AgentTraceDetailDrawer';
import TraceTextPreview from './components/TraceTextPreview';

/** Trace 表格默认分页大小。 */
const TRACE_TABLE_DEFAULT_PAGE_SIZE = 20;

/** Trace 表格分页选项。 */
const TRACE_TABLE_PAGE_SIZE_OPTIONS = ['10', '20', '50', '100'];

/** Trace 耗时绿色阈值秒数。 */
const TRACE_DURATION_FAST_SECONDS = 10;

/** Trace 耗时黄色阈值秒数。 */
const TRACE_DURATION_WARN_SECONDS = 30;

/** Trace 状态展示配置。 */
const TRACE_STATUS_META: Record<string, { text: string; color: string; status: string }> = {
  running: { text: '运行中', color: 'processing', status: 'Processing' },
  success: { text: '成功', color: 'success', status: 'Success' },
  error: { text: '异常', color: 'error', status: 'Error' },
  cancelled: { text: '已取消', color: 'warning', status: 'Warning' },
};

/** 会话类型展示配置。 */
const CONVERSATION_TYPE_META: Record<string, { text: string; color: string }> = {
  admin: { text: '管理端', color: 'blue' },
  client: { text: '客户端', color: 'green' },
};

/**
 * 渲染 Trace 状态标签。
 *
 * @param status 状态值。
 * @returns 状态标签。
 */
function renderStatusTag(status?: string) {
  const meta = TRACE_STATUS_META[status || ''] || { text: status || '-', color: 'default' };
  return <Tag color={meta.color}>{meta.text}</Tag>;
}

/**
 * 渲染会话类型标签。
 *
 * @param conversationType 会话类型。
 * @returns 会话类型标签。
 */
function renderConversationTypeTag(conversationType?: string) {
  const meta = CONVERSATION_TYPE_META[conversationType || ''] || {
    text: conversationType || '-',
    color: 'default',
  };
  return <Tag color={meta.color}>{meta.text}</Tag>;
}

/**
 * 解析耗时毫秒数。
 *
 * @param durationMs 耗时毫秒。
 * @returns 耗时毫秒数；非法时返回 null。
 */
function resolveDurationMs(durationMs?: number) {
  if (typeof durationMs === 'number' && Number.isFinite(durationMs)) {
    return Math.max(durationMs, 0);
  }
  return null;
}

/**
 * 格式化秒级耗时文本。
 *
 * @param durationMs 耗时毫秒。
 * @returns 秒级耗时文本。
 */
function formatDurationSeconds(durationMs: number) {
  const seconds = durationMs / 1000;
  const fixedText = seconds >= 10 ? seconds.toFixed(1) : seconds.toFixed(2);
  return `${fixedText.replace(/\.?0+$/, '')} s`;
}

/**
 * 解析耗时颜色。
 *
 * @param durationMs 耗时毫秒。
 * @returns Ant Design Tag 颜色。
 */
function resolveDurationColor(durationMs: number) {
  const seconds = durationMs / 1000;
  if (seconds <= TRACE_DURATION_FAST_SECONDS) {
    return 'success';
  }
  if (seconds <= TRACE_DURATION_WARN_SECONDS) {
    return 'warning';
  }
  return 'error';
}

/**
 * 渲染秒级耗时标签。
 *
 * @param durationMs 耗时毫秒。
 * @returns 秒级耗时标签。
 */
function renderDurationTag(durationMs?: number) {
  const resolvedDurationMs = resolveDurationMs(durationMs);
  if (resolvedDurationMs === null) {
    return '-';
  }
  return (
    <Tag
      color={resolveDurationColor(resolvedDurationMs)}
      style={{ marginInlineEnd: 0, fontVariantNumeric: 'tabular-nums' }}
    >
      {formatDurationSeconds(resolvedDurationMs)}
    </Tag>
  );
}

/**
 * 智能体跟踪列表页面。
 *
 * @returns 智能体跟踪页面节点。
 */
const AgentTracePage: React.FC = () => {
  useAgentObservabilitySecondaryMenu();
  const actionRef = useRef<ActionType | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [currentTraceId, setCurrentTraceId] = useState<string | null>(null);
  const [deletingTraceId, setDeletingTraceId] = useState<string | null>(null);
  const [messageApi, contextHolder] = message.useMessage();

  /**
   * 打开 Trace 详情抽屉。
   *
   * @param traceId Trace 唯一标识。
   * @returns 无返回值。
   */
  const openDetail = useCallback((traceId?: string) => {
    if (!traceId) return;
    setCurrentTraceId(traceId);
    setDetailOpen(true);
  }, []);

  /**
   * 确认并删除 Trace。
   *
   * @param record Trace 列表行。
   * @returns 无返回值。
   */
  const handleDelete = useCallback(
    (record: AgentTraceTypes.RunListVo) => {
      if (!record.traceId) return;
      Modal.confirm({
        title: '确认删除',
        content: `确定要删除 Trace ${record.traceId} 吗？删除后 run 和 span 明细都会被移除。`,
        okText: '确定',
        cancelText: '取消',
        okType: 'danger',
        onOk: async () => {
          setDeletingTraceId(record.traceId || null);
          try {
            await deleteAgentTrace(record.traceId as string);
            messageApi.success('删除 Trace 成功');
            actionRef.current?.reload();
          } finally {
            setDeletingTraceId(null);
          }
        },
      });
    },
    [messageApi],
  );

  /** Trace 列表表格列配置。 */
  const columns: ProColumns<AgentTraceTypes.RunListVo>[] = [
    {
      title: 'Trace ID',
      dataIndex: 'traceId',
      width: 230,
      copyable: true,
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      align: 'center',
      valueType: 'select',
      valueEnum: Object.fromEntries(
        Object.entries(TRACE_STATUS_META).map(([key, item]) => [
          key,
          { text: item.text, status: item.status },
        ]),
      ),
      render: (_, record) => renderStatusTag(record.status),
    },
    {
      title: '会话类型',
      dataIndex: 'conversationType',
      width: 110,
      align: 'center',
      valueType: 'select',
      valueEnum: {
        admin: { text: '管理端' },
        client: { text: '客户端' },
      },
      render: (_, record) => renderConversationTypeTag(record.conversationType),
    },
    {
      title: '输入',
      dataIndex: 'inputText',
      width: 260,
      search: false,
      render: (_, record) => <TraceTextPreview text={record.inputText} />,
    },
    {
      title: '输出',
      dataIndex: 'outputText',
      width: 320,
      search: false,
      render: (_, record) => <TraceTextPreview text={record.outputText} />,
    },
    {
      title: '会话UUID',
      dataIndex: 'conversationUuid',
      width: 220,
      ellipsis: true,
      copyable: true,
      hideInTable: true,
    },
    {
      title: 'AI消息UUID',
      dataIndex: 'assistantMessageUuid',
      width: 220,
      ellipsis: true,
      copyable: true,
      hideInTable: true,
    },
    {
      title: '耗时',
      dataIndex: 'durationMs',
      width: 100,
      align: 'right',
      search: false,
      render: (_, record) => renderDurationTag(record.durationMs),
    },
    {
      title: 'Token',
      dataIndex: 'totalTokens',
      width: 100,
      align: 'right',
      search: false,
      render: (_, record) => record.totalTokens ?? 0,
    },
    {
      title: '开始时间',
      dataIndex: 'startedAt',
      width: 180,
      valueType: 'dateTime',
      search: false,
    },
    {
      title: '开始时间',
      dataIndex: 'startedAtRange',
      valueType: 'dateTimeRange',
      hideInTable: true,
      search: {
        transform: (value) => ({
          startTime: value[0],
          endTime: value[1],
        }),
      },
    },
    {
      title: '操作',
      dataIndex: 'option',
      valueType: 'option',
      width: 120,
      fixed: 'right',
      align: 'center',
      render: (_, record) => (
        <TableActionGroup>
          <PermissionButton
            type="link"
            access={ADMIN_PERMISSIONS.agentTrace.query}
            onClick={() => openDetail(record.traceId)}
          >
            详情
          </PermissionButton>
          <PermissionButton
            type="link"
            danger
            loading={deletingTraceId === record.traceId}
            access={ADMIN_PERMISSIONS.agentTrace.delete}
            onClick={() => handleDelete(record)}
          >
            删除
          </PermissionButton>
        </TableActionGroup>
      ),
    },
  ];

  return (
    <PageContainer>
      {contextHolder}
      <ProTable<AgentTraceTypes.RunListVo, AgentTraceTypes.RunListRequest>
        headerTitle="智能体跟踪"
        actionRef={actionRef}
        rowKey="traceId"
        search={{
          labelWidth: 110,
        }}
        request={async (params) => {
          const { current, pageSize, startedAtRange, ...rest } = params as any;
          const requestParams: AgentTraceTypes.RunListRequest = {
            ...rest,
            pageNum: Number(current ?? 1),
            pageSize: Number(pageSize ?? TRACE_TABLE_DEFAULT_PAGE_SIZE),
          };
          if (startedAtRange && startedAtRange.length === 2) {
            requestParams.startTime = startedAtRange[0];
            requestParams.endTime = startedAtRange[1];
          }
          const result = await listAgentTraceRuns(requestParams);
          return {
            data: result?.rows || [],
            success: true,
            total: Number(result?.total) || 0,
          };
        }}
        columns={columns}
        scroll={{ x: 'max-content' }}
        pagination={{
          showQuickJumper: true,
          showSizeChanger: true,
          defaultPageSize: TRACE_TABLE_DEFAULT_PAGE_SIZE,
          pageSizeOptions: TRACE_TABLE_PAGE_SIZE_OPTIONS,
        }}
        size="middle"
      />

      <AgentTraceDetailDrawer
        open={detailOpen}
        traceId={currentTraceId}
        onClose={() => setDetailOpen(false)}
      />
    </PageContainer>
  );
};

export default AgentTracePage;
