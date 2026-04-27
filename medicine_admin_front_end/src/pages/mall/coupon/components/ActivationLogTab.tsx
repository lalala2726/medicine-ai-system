import type { ActionType, ProColumns, ProFormInstance } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Descriptions, Modal, Tag, Typography } from 'antd';
import React, { useEffect, useMemo, useRef } from 'react';
import { listActivationLogs, type MallCouponTypes } from '@/api/mall/coupon';
import TableActionGroup from '@/components/TableActionGroup';

type ActivationRedeemLogRecord = MallCouponTypes.ActivationLogVo;

type ActivationRedeemLogQuery = MallCouponTypes.ActivationLogListRequest & {
  pageNum?: number;
  pageSize?: number;
  createTimeRange?: [string, string];
};

/**
 * 兑换规则选项。
 */
const ACTIVATION_REDEEM_RULE_OPTIONS = [
  { label: '共享码（每用户一次）', value: 'SHARED_PER_USER_ONCE' },
  { label: '唯一码（全局一次）', value: 'UNIQUE_SINGLE_USE' },
] satisfies Array<{
  label: string;
  value: MallCouponTypes.ActivationRedeemRuleType;
}>;

/**
 * 兑换结果状态选项。
 */
const ACTIVATION_LOG_RESULT_OPTIONS = [
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAIL' },
] satisfies Array<{
  label: string;
  value: MallCouponTypes.ActivationLogResultStatus;
}>;

/**
 * 发券状态颜色映射。
 */
const ACTIVATION_GRANT_STATUS_COLOR_MAP: Record<string, string> = {
  SUCCESS: 'success',
  FAIL: 'error',
};

/**
 * 解析兑换规则文案。
 * @param value 兑换规则编码。
 * @returns 兑换规则文案。
 */
function activationRedeemRuleText(value?: string): string {
  return ACTIVATION_REDEEM_RULE_OPTIONS.find((item) => item.value === value)?.label || value || '-';
}

/**
 * 解析结果状态文案。
 * @param value 结果状态编码。
 * @returns 结果状态文案。
 */
function activationLogResultText(value?: string): string {
  return ACTIVATION_LOG_RESULT_OPTIONS.find((item) => item.value === value)?.label || value || '-';
}

/**
 * 激活码日志页签属性。
 */
interface ActivationLogTabProps {
  /** 默认筛选参数 */
  querySeed?: Partial<MallCouponTypes.ActivationLogListRequest>;
}

/**
 * 激活码兑换日志页签。
 * @param props 组件属性。
 * @returns 页签节点。
 */
const ActivationLogTab: React.FC<ActivationLogTabProps> = ({ querySeed }) => {
  const redeemActionRef = useRef<ActionType | undefined>(undefined);
  const redeemFormRef = useRef<ProFormInstance<ActivationRedeemLogQuery> | undefined>(undefined);

  /**
   * 同步外部带入的查询种子到搜索表单。
   * @returns 无返回值。
   */
  useEffect(() => {
    if (!querySeed) {
      return;
    }
    redeemFormRef.current?.setFieldsValue(querySeed);
    redeemActionRef.current?.reload();
  }, [querySeed]);

  /**
   * 兑换日志列定义。
   */
  const redeemColumns = useMemo<ProColumns<ActivationRedeemLogRecord>[]>(
    () => [
      {
        title: '日志ID',
        dataIndex: 'id',
        width: 110,
        hideInSearch: true,
      },
      {
        title: '批次号',
        dataIndex: 'batchNo',
        width: 220,
      },
      {
        title: '激活码',
        dataIndex: 'plainCodeSnapshot',
        width: 170,
      },
      {
        title: '兑换规则',
        dataIndex: 'redeemRuleType',
        width: 180,
        valueType: 'select',
        valueEnum: ACTIVATION_REDEEM_RULE_OPTIONS.reduce<Record<string, { text: string }>>(
          (result, item) => {
            result[item.value] = { text: item.label };
            return result;
          },
          {},
        ),
        render: (_, record) => activationRedeemRuleText(record.redeemRuleType),
      },
      {
        title: '用户名',
        dataIndex: 'userId',
        width: 140,
        render: (_, record) => record.userName || record.userId || '-',
      },
      {
        title: '结果状态',
        dataIndex: 'resultStatus',
        width: 110,
        valueType: 'select',
        valueEnum: ACTIVATION_LOG_RESULT_OPTIONS.reduce<Record<string, { text: string }>>(
          (result, item) => {
            result[item.value] = { text: item.label };
            return result;
          },
          {},
        ),
        render: (_, record) => (
          <Tag color={record.resultStatus === 'SUCCESS' ? 'success' : 'error'}>
            {activationLogResultText(record.resultStatus)}
          </Tag>
        ),
      },
      {
        title: '发券状态',
        dataIndex: 'grantStatus',
        width: 110,
        hideInSearch: true,
        render: (_, record) => (
          <Tag color={ACTIVATION_GRANT_STATUS_COLOR_MAP[record.grantStatus || ''] || 'default'}>
            {record.grantStatus || '-'}
          </Tag>
        ),
      },
      {
        title: '用户券ID',
        dataIndex: 'couponId',
        width: 110,
        hideInSearch: true,
      },
      {
        title: '失败编码',
        dataIndex: 'failCode',
        width: 170,
        hideInSearch: true,
      },
      {
        title: '失败信息',
        dataIndex: 'failMessage',
        width: 220,
        ellipsis: true,
        hideInSearch: true,
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        width: 180,
        valueType: 'dateTime',
        hideInSearch: true,
      },
      {
        title: '创建时间',
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
        key: 'option',
        width: 80,
        fixed: 'right',
        valueType: 'option',
        align: 'center',
        render: (_, record) => (
          <TableActionGroup>
            <Button
              type="link"
              onClick={() => {
                Modal.info({
                  title: '兑换日志详情',
                  width: 760,
                  icon: null,
                  okText: '关闭',
                  content: (
                    <div style={{ marginTop: 16 }}>
                      <Descriptions bordered column={2} size="small">
                        <Descriptions.Item label="日志ID">{record.id ?? '-'}</Descriptions.Item>
                        <Descriptions.Item label="请求ID">
                          {record.requestId ? (
                            <Typography.Text
                              copyable={{ text: record.requestId }}
                              style={{ fontSize: 12 }}
                            >
                              {record.requestId}
                            </Typography.Text>
                          ) : (
                            '-'
                          )}
                        </Descriptions.Item>
                        <Descriptions.Item label="批次号" span={2}>
                          {record.batchNo ? (
                            <Typography.Text copyable={{ text: record.batchNo }}>
                              {record.batchNo}
                            </Typography.Text>
                          ) : (
                            '-'
                          )}
                        </Descriptions.Item>
                        <Descriptions.Item label="模板名称">
                          {record.templateName || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="兑换规则">
                          {activationRedeemRuleText(record.redeemRuleType)}
                        </Descriptions.Item>
                        <Descriptions.Item label="激活码">
                          {record.plainCodeSnapshot ? (
                            <Typography.Text copyable={{ text: record.plainCodeSnapshot }} code>
                              {record.plainCodeSnapshot}
                            </Typography.Text>
                          ) : (
                            '-'
                          )}
                        </Descriptions.Item>
                        <Descriptions.Item label="用户名">
                          {record.userName || record.userId || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="结果状态">
                          <Tag color={record.resultStatus === 'SUCCESS' ? 'success' : 'error'}>
                            {activationLogResultText(record.resultStatus)}
                          </Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="发券状态">
                          <Tag
                            color={
                              ACTIVATION_GRANT_STATUS_COLOR_MAP[record.grantStatus || ''] ||
                              'default'
                            }
                          >
                            {record.grantStatus || '-'}
                          </Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="用户券ID">
                          {record.couponId ?? '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="发券方式">
                          {record.grantMode || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="客户端IP">
                          {record.clientIp || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="创建时间" span={2}>
                          {record.createTime || '-'}
                        </Descriptions.Item>
                        {(record.failCode || record.failMessage) && (
                          <Descriptions.Item label="失败编码" span={2}>
                            <Typography.Text type="danger">
                              {record.failCode || '-'}
                            </Typography.Text>
                          </Descriptions.Item>
                        )}
                        {(record.failCode || record.failMessage) && (
                          <Descriptions.Item label="失败信息" span={2}>
                            <Typography.Text type="danger" style={{ wordBreak: 'break-all' }}>
                              {record.failMessage || '-'}
                            </Typography.Text>
                          </Descriptions.Item>
                        )}
                      </Descriptions>
                    </div>
                  ),
                });
              }}
            >
              详情
            </Button>
          </TableActionGroup>
        ),
      },
    ],
    [],
  );

  return (
    <ProTable<ActivationRedeemLogRecord, ActivationRedeemLogQuery>
      rowKey="id"
      actionRef={redeemActionRef}
      formRef={redeemFormRef}
      headerTitle="兑换日志"
      search={{ labelWidth: 108 }}
      form={{
        initialValues: querySeed,
      }}
      request={async (params) => {
        const { current, pageSize, ...rest } = params;
        const query: ActivationRedeemLogQuery = {
          ...(rest as ActivationRedeemLogQuery),
          batchId: rest.batchId ? Number(rest.batchId) : undefined,
          activationCodeId: rest.activationCodeId ? Number(rest.activationCodeId) : undefined,
          userId: rest.userId ? Number(rest.userId) : undefined,
          pageNum: Number(current ?? 1),
          pageSize: Number(pageSize ?? 10),
        };
        const result = await listActivationLogs(query);
        return {
          data: result?.rows ?? [],
          success: true,
          total: Number(result?.total ?? 0),
        };
      }}
      columns={redeemColumns}
      pagination={{
        showQuickJumper: true,
        showSizeChanger: true,
        defaultPageSize: 10,
      }}
    />
  );
};

export default ActivationLogTab;
