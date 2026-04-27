import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { useRequest } from 'ahooks';
import { Modal, message, Tag } from 'antd';
import React, { useCallback, useRef, useState } from 'react';
import {
  clearLoginLogs,
  type LoginLogListVo,
  type LoginLogQueryRequest,
  listLoginLogs,
} from '@/api/systemLog/loginLog';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { LOG_TABLE_DEFAULT_PAGE_SIZE, LOG_TABLE_PAGE_SIZE_OPTIONS } from '../constants';
import LoginLogDetail from './components/LoginLogDetail';

// 登录来源映射
const loginSourceMap: Record<string, { text: string; color: string }> = {
  admin: { text: '管理端', color: 'blue' },
  client: { text: '客户端', color: 'green' },
};

// 登录方式映射
const loginTypeMap: Record<string, { text: string; color: string }> = {
  PASSWORD: { text: '密码登录', color: 'default' },
  SMS: { text: '短信登录', color: 'cyan' },
  WECHAT: { text: '微信登录', color: 'green' },
  AUTO: { text: '自动登录', color: 'purple' },
};

const LoginLogManagement: React.FC = () => {
  const actionRef = useRef<ActionType | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentLogId, setCurrentLogId] = useState<string | number | null>(null);
  const [messageApi, contextHolder] = message.useMessage();

  const { run: clearRun, loading: clearLoading } = useRequest(clearLoginLogs, {
    manual: true,
    onSuccess: () => {
      messageApi.success('清空登录日志成功');
      actionRef.current?.reload();
    },
  });

  const columns: ProColumns<LoginLogListVo>[] = [
    {
      title: '日志ID',
      dataIndex: 'id',
      width: 100,
    },
    {
      title: '登录账号',
      dataIndex: 'username',
      width: 120,
      ellipsis: true,
    },
    {
      title: '登录来源',
      dataIndex: 'loginSource',
      width: 100,
      align: 'center',
      valueType: 'select',
      valueEnum: Object.fromEntries(
        Object.entries(loginSourceMap).map(([value, { text }]) => [value, { text }]),
      ),
      render: (_, record) => {
        const source = loginSourceMap[record.loginSource || ''];
        return source ? <Tag color={source.color}>{source.text}</Tag> : record.loginSource || '-';
      },
    },
    {
      title: '登录方式',
      dataIndex: 'loginType',
      width: 100,
      align: 'center',
      valueType: 'select',
      valueEnum: Object.fromEntries(
        Object.entries(loginTypeMap).map(([value, { text }]) => [value, { text }]),
      ),
      render: (_, record) => {
        const type = loginTypeMap[record.loginType || ''];
        return type ? <Tag color={type.color}>{type.text}</Tag> : record.loginType || '-';
      },
    },
    {
      title: 'IP地址',
      dataIndex: 'ipAddress',
      width: 140,
      copyable: true,
    },
    {
      title: '登录状态',
      dataIndex: 'loginStatus',
      width: 100,
      align: 'center',
      valueType: 'select',
      valueEnum: {
        1: { text: '成功', status: 'Success' },
        0: { text: '失败', status: 'Error' },
      },
      render: (_, record) => (
        <Tag color={record.loginStatus === 1 ? 'success' : 'error'}>
          {record.loginStatus === 1 ? '成功' : '失败'}
        </Tag>
      ),
    },
    {
      title: '登录时间',
      dataIndex: 'loginTime',
      width: 180,
      valueType: 'dateTime',
      sorter: true,
    },
    {
      title: '登录时间',
      dataIndex: 'loginTimeRange',
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
            access={ADMIN_PERMISSIONS.systemLog.loginQuery}
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
      content: '确定要清空所有登录日志吗？此操作无法撤销。',
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
      <ProTable<LoginLogListVo, LoginLogQueryRequest>
        headerTitle="登录日志"
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
            access={ADMIN_PERMISSIONS.systemLog.loginDelete}
            onClick={handleClearLogs}
          >
            清空日志
          </PermissionButton>,
        ]}
        request={async (params) => {
          const { current, pageSize, loginTimeRange, ...rest } = params as any;
          const requestParams: LoginLogQueryRequest = {
            ...rest,
            pageNum: Number(current ?? 1),
            pageSize: Number(pageSize ?? 10),
          };
          // 处理时间范围
          if (loginTimeRange && loginTimeRange.length === 2) {
            requestParams.startTime = loginTimeRange[0];
            requestParams.endTime = loginTimeRange[1];
          }
          const result = await listLoginLogs(requestParams);
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

      <LoginLogDetail
        open={detailVisible}
        logId={currentLogId}
        onClose={() => setDetailVisible(false)}
      />
    </PageContainer>
  );
};

export default LoginLogManagement;
