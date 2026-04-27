import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { useRequest } from 'ahooks';
import { Modal, message, Tag } from 'antd';
import React, { useCallback, useRef, useState } from 'react';
import {
  clearOperationLogs,
  listOperationLogs,
  type OperationLogListVo,
  type OperationLogQueryRequest,
} from '@/api/systemLog/operationLog';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { LOG_TABLE_DEFAULT_PAGE_SIZE, LOG_TABLE_PAGE_SIZE_OPTIONS } from '../constants';
import OperationLogDetail from './components/OperationLogDetail';

const OperationLogManagement: React.FC = () => {
  const actionRef = useRef<ActionType | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentLogId, setCurrentLogId] = useState<string | number | null>(null);
  const [messageApi, contextHolder] = message.useMessage();

  const { run: clearRun, loading: clearLoading } = useRequest(clearOperationLogs, {
    manual: true,
    onSuccess: () => {
      messageApi.success('清空操作日志成功');
      actionRef.current?.reload();
    },
  });

  const columns: ProColumns<OperationLogListVo>[] = [
    {
      title: '日志ID',
      dataIndex: 'id',
      width: 100,
    },
    {
      title: '业务模块',
      dataIndex: 'module',
      width: 120,
      ellipsis: true,
    },
    {
      title: '操作说明',
      dataIndex: 'action',
      width: 180,
      ellipsis: true,
    },
    {
      title: '操作人账号',
      dataIndex: 'username',
      width: 120,
      ellipsis: true,
    },
    {
      title: '请求IP',
      dataIndex: 'ip',
      width: 140,
      copyable: true,
    },
    {
      title: '执行状态',
      dataIndex: 'success',
      width: 100,
      align: 'center',
      valueType: 'select',
      valueEnum: {
        1: { text: '成功', status: 'Success' },
        0: { text: '失败', status: 'Error' },
      },
      render: (_, record) => (
        <Tag color={record.success === 1 ? 'success' : 'error'}>
          {record.success === 1 ? '成功' : '失败'}
        </Tag>
      ),
    },
    {
      title: '耗时',
      dataIndex: 'costTime',
      width: 100,
      align: 'right',
      render: (_, record) => (record.costTime ? `${record.costTime} ms` : '-'),
    },
    {
      title: '操作时间',
      dataIndex: 'createTime',
      width: 180,
      valueType: 'dateTime',
      sorter: true,
    },
    {
      title: '操作时间',
      dataIndex: 'createTimeRange',
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
      width: 80,
      fixed: 'right',
      align: 'center',
      render: (_, record) => (
        <TableActionGroup>
          <PermissionButton
            type="link"
            access={ADMIN_PERMISSIONS.systemLog.operationQuery}
            onClick={() => {
              if (!record.id) return;
              setCurrentLogId(record.id);
              setDetailVisible(true);
            }}
          >
            详情
          </PermissionButton>
        </TableActionGroup>
      ),
    },
  ];

  const handleClearLogs = useCallback(() => {
    Modal.confirm({
      title: '确认清空',
      content: '确定要清空所有操作日志吗？此操作无法撤销。',
      okText: '确定',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        await clearRun();
      },
    });
  }, [clearRun]);

  return (
    <PageContainer>
      {contextHolder}
      <ProTable<OperationLogListVo, OperationLogQueryRequest>
        headerTitle="操作日志"
        actionRef={actionRef}
        rowKey="id"
        search={{
          labelWidth: 120,
        }}
        toolBarRender={() => [
          <PermissionButton
            key="clear"
            danger
            loading={clearLoading}
            access={ADMIN_PERMISSIONS.systemLog.operationDelete}
            onClick={handleClearLogs}
          >
            清空日志
          </PermissionButton>,
        ]}
        request={async (params) => {
          const { current, pageSize, createTimeRange, ...rest } = params as any;
          const requestParams: OperationLogQueryRequest = {
            ...rest,
            pageNum: Number(current ?? 1),
            pageSize: Number(pageSize ?? 10),
          };
          // 处理时间范围
          if (createTimeRange && createTimeRange.length === 2) {
            requestParams.startTime = createTimeRange[0];
            requestParams.endTime = createTimeRange[1];
          }
          const result = await listOperationLogs(requestParams);
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
          defaultPageSize: LOG_TABLE_DEFAULT_PAGE_SIZE,
          pageSizeOptions: LOG_TABLE_PAGE_SIZE_OPTIONS,
        }}
        size="middle"
      />

      <OperationLogDetail
        open={detailVisible}
        logId={currentLogId}
        onClose={() => setDetailVisible(false)}
      />
    </PageContainer>
  );
};

export default OperationLogManagement;
