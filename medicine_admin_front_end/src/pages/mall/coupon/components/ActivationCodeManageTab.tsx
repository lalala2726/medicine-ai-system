import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import {
  Button,
  Descriptions,
  Divider,
  Drawer,
  Modal,
  Space,
  Tag,
  Typography,
  message,
} from 'antd';
import React, { useCallback, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  deleteActivationBatchById,
  getActivationCodeById,
  listActivationCodes,
  updateActivationCodeStatus,
  type MallCouponTypes,
} from '@/api/mall/coupon';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { buildMallCouponActivationCodesPath } from '@/router/paths';
import ActivationCodeGenerateModal from './ActivationCodeGenerateModal';

type ActivationBatchRecord = MallCouponTypes.ActivationCodeVo;

type ActivationBatchQuery = MallCouponTypes.ActivationCodeListRequest & {
  pageNum?: number;
  pageSize?: number;
  createTimeRange?: [string, string];
};

/**
 * 兑换规则选项。
 */
const ACTIVATION_CODE_MODE_OPTIONS = [
  { label: '共享码', value: 'SHARED_PER_USER_ONCE' },
  { label: '唯一码', value: 'UNIQUE_SINGLE_USE' },
] satisfies Array<{
  label: string;
  value: MallCouponTypes.ActivationRedeemRuleType;
}>;

/**
 * 激活码有效期类型选项。
 */
const ACTIVATION_VALIDITY_TYPE_OPTIONS = [
  { label: '一次性', value: 'ONCE' },
  { label: '激活后计算', value: 'AFTER_ACTIVATION' },
] satisfies Array<{
  label: string;
  value: MallCouponTypes.ActivationCodeValidityType;
}>;

/**
 * 激活码状态选项。
 */
const ACTIVATION_CODE_STATUS_OPTIONS = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'DISABLED' },
] satisfies Array<{
  label: string;
  value: MallCouponTypes.ActivationCodeStatus;
}>;

/**
 * 批次状态 Tag 颜色映射。
 */
const ACTIVATION_CODE_STATUS_TAG_COLOR: Record<MallCouponTypes.ActivationCodeStatus, string> = {
  ACTIVE: 'success',
  DISABLED: 'error',
};

/**
 * 激活码批次管理页签属性。
 */
interface ActivationCodeManageTabProps {
  /** 查看兑换日志回调 */
  onViewLogs?: (querySeed: Partial<MallCouponTypes.ActivationLogListRequest>) => void;
}

/**
 * 构建有效期显示文本。
 * @param record 激活码批次记录。
 * @returns 有效期显示文本。
 */
function buildActivationValidityDisplay(record: {
  validityType?: string;
  fixedEffectiveTime?: string;
  fixedExpireTime?: string;
  relativeValidDays?: number;
}): string {
  if (record.validityType === 'ONCE') {
    return `${record.fixedEffectiveTime || '-'} 至 ${record.fixedExpireTime || '-'}`;
  }
  return record.relativeValidDays ? `激活后${record.relativeValidDays}天` : '-';
}

/**
 * 解析模式文案。
 * @param value 模式编码。
 * @returns 模式文案。
 */
function activationCodeModeText(value?: string): string {
  return ACTIVATION_CODE_MODE_OPTIONS.find((item) => item.value === value)?.label || value || '-';
}

/**
 * 解析有效期文案。
 * @param value 有效期类型编码。
 * @returns 有效期文案。
 */
function activationValidityText(value?: string): string {
  return (
    ACTIVATION_VALIDITY_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || '-'
  );
}

/**
 * 解析状态文案。
 * @param value 状态编码。
 * @returns 状态文案。
 */
function activationCodeStatusText(value?: string): string {
  return ACTIVATION_CODE_STATUS_OPTIONS.find((item) => item.value === value)?.label || value || '-';
}

/**
 * 激活码批次管理页签。
 * @param props 组件属性。
 * @returns 页签节点。
 */
const ActivationCodeManageTab: React.FC<ActivationCodeManageTabProps> = ({ onViewLogs }) => {
  const navigate = useNavigate();
  const [messageApi, contextHolder] = message.useMessage();
  const [generateDrawerOpen, setGenerateDrawerOpen] = useState(false);
  const [detailDrawerOpen, setDetailDrawerOpen] = useState(false);
  const [currentBatchDetail, setCurrentBatchDetail] = useState<
    MallCouponTypes.ActivationCodeDetailVo | undefined
  >(undefined);
  const actionRef = useRef<ActionType | null>(null);

  /**
   * 切换批次状态。
   * @param record 激活码批次记录。
   * @returns 无返回值。
   */
  const handleToggleStatus = useCallback(
    (record: ActivationBatchRecord) => {
      if (!record.id || !record.status) {
        messageApi.warning('缺少批次状态信息，无法更新');
        return;
      }
      const nextStatus: MallCouponTypes.ActivationCodeStatus =
        record.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
      Modal.confirm({
        title: nextStatus === 'ACTIVE' ? '确认启用批次' : '确认停用批次',
        content: `确定要将批次「${record.batchNo || record.id}」设置为${activationCodeStatusText(nextStatus)}吗？`,
        okText: '确认',
        cancelText: '取消',
        onOk: async () => {
          try {
            await updateActivationCodeStatus({
              id: record.id as number,
              status: nextStatus,
            });
            messageApi.success('批次状态更新成功');
            actionRef.current?.reload();
          } catch (error) {
            console.error('更新批次状态失败:', error);
            messageApi.error('更新批次状态失败');
          }
        },
      });
    },
    [messageApi],
  );

  /**
   * 删除批次并级联删除批次内激活码。
   * @param record 激活码批次记录。
   * @returns 无返回值。
   */
  const handleDeleteBatch = useCallback(
    (record: ActivationBatchRecord) => {
      if (!record.id) {
        messageApi.warning('缺少批次ID，无法删除');
        return;
      }
      Modal.confirm({
        title: '确认删除批次',
        content: `确定删除批次「${record.batchNo || record.id}」吗？删除后该批次下激活码会一并删除。`,
        okText: '确认删除',
        okButtonProps: { danger: true },
        cancelText: '取消',
        onOk: async () => {
          try {
            await deleteActivationBatchById(record.id as number);
            if (currentBatchDetail?.id === record.id) {
              setCurrentBatchDetail(undefined);
              setDetailDrawerOpen(false);
            }
            messageApi.success('批次删除成功');
            actionRef.current?.reload();
          } catch (error) {
            console.error('删除批次失败:', error);
            messageApi.error('删除批次失败');
          }
        },
      });
    },
    [currentBatchDetail, messageApi],
  );

  /**
   * 查看批次详情。
   * @param record 激活码批次记录。
   * @returns 无返回值。
   */
  const handleShowDetail = useCallback(
    async (record: ActivationBatchRecord) => {
      if (!record.id) {
        messageApi.warning('缺少批次ID，无法查看详情');
        return;
      }
      try {
        const detail = await getActivationCodeById(record.id);
        setCurrentBatchDetail(detail);
        setDetailDrawerOpen(true);
      } catch (error) {
        console.error('加载批次详情失败:', error);
        messageApi.error('加载批次详情失败');
      }
    },
    [messageApi],
  );

  /**
   * 跳转到批次激活码列表页面。
   * @param record 激活码批次记录。
   * @returns 无返回值。
   */
  const handleViewCodes = useCallback(
    (record: ActivationBatchRecord) => {
      if (!record.id) {
        messageApi.warning('缺少批次ID，无法查看激活码');
        return;
      }
      navigate(buildMallCouponActivationCodesPath(record.id));
    },
    [messageApi, navigate],
  );

  /**
   * 批次列表列定义。
   */
  const columns = useMemo<ProColumns<ActivationBatchRecord>[]>(
    () => [
      {
        title: '批次号',
        dataIndex: 'batchNo',
        width: 210,
      },
      {
        title: '模板名称',
        dataIndex: 'templateName',
        width: 180,
      },
      {
        title: '模板名称',
        dataIndex: 'templateName',
        hideInTable: true,
      },
      {
        title: '兑换规则',
        dataIndex: 'redeemRuleType',
        width: 120,
        valueType: 'select',
        valueEnum: ACTIVATION_CODE_MODE_OPTIONS.reduce<Record<string, { text: string }>>(
          (result, item) => {
            result[item.value] = { text: item.label };
            return result;
          },
          {},
        ),
        render: (_, record) => activationCodeModeText(record.redeemRuleType),
      },
      {
        title: '有效期类型',
        dataIndex: 'validityType',
        width: 140,
        valueType: 'select',
        valueEnum: ACTIVATION_VALIDITY_TYPE_OPTIONS.reduce<Record<string, { text: string }>>(
          (result, item) => {
            result[item.value] = { text: item.label };
            return result;
          },
          {},
        ),
        render: (_, record) => activationValidityText(record.validityType),
      },
      {
        title: '生成数量',
        dataIndex: 'generateCount',
        width: 100,
        hideInSearch: true,
      },
      {
        title: '已使用次数',
        dataIndex: 'successUseCount',
        width: 110,
        hideInSearch: true,
      },
      {
        title: '状态',
        dataIndex: 'status',
        width: 90,
        valueType: 'select',
        valueEnum: ACTIVATION_CODE_STATUS_OPTIONS.reduce<Record<string, { text: string }>>(
          (result, item) => {
            result[item.value] = { text: item.label };
            return result;
          },
          {},
        ),
        render: (_, record) => {
          const status = record.status as MallCouponTypes.ActivationCodeStatus | undefined;
          const color = status ? ACTIVATION_CODE_STATUS_TAG_COLOR[status] : 'default';
          const text = activationCodeStatusText(status);
          return <Tag color={color}>{text}</Tag>;
        },
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
        valueType: 'option',
        fixed: 'right',
        width: 340,
        align: 'center',
        render: (_, record) => (
          <TableActionGroup>
            <PermissionButton
              type="link"
              access={ADMIN_PERMISSIONS.mallCoupon.activationBatchQuery}
              style={{ minWidth: 52 }}
              onClick={() => void handleShowDetail(record)}
            >
              详情
            </PermissionButton>
            <PermissionButton
              type="link"
              access={ADMIN_PERMISSIONS.mallCoupon.activationBatchQuery}
              onClick={() => handleViewCodes(record)}
            >
              查看激活码
            </PermissionButton>
            <PermissionButton
              type="link"
              access={ADMIN_PERMISSIONS.mallCoupon.activationLogList}
              onClick={() =>
                onViewLogs?.({
                  batchNo: record.batchNo,
                })
              }
            >
              查看日志
            </PermissionButton>
            <PermissionButton
              type="link"
              access={ADMIN_PERMISSIONS.mallCoupon.activationBatchStatus}
              onClick={() => handleToggleStatus(record)}
            >
              {record.status === 'ACTIVE' ? '停用' : '启用'}
            </PermissionButton>
            <PermissionButton
              type="link"
              danger
              access={ADMIN_PERMISSIONS.mallCoupon.activationBatchStatus}
              onClick={() => handleDeleteBatch(record)}
            >
              删除
            </PermissionButton>
          </TableActionGroup>
        ),
      },
    ],
    [handleDeleteBatch, handleShowDetail, handleToggleStatus, handleViewCodes, onViewLogs],
  );

  return (
    <>
      {contextHolder}
      <ProTable<ActivationBatchRecord, ActivationBatchQuery>
        rowKey="id"
        actionRef={actionRef}
        headerTitle="激活码批次"
        search={{ labelWidth: 108 }}
        request={async (params) => {
          const { current, pageSize, ...rest } = params;
          const query: ActivationBatchQuery = {
            ...(rest as ActivationBatchQuery),
            templateId: rest.templateId ? Number(rest.templateId) : undefined,
            pageNum: Number(current ?? 1),
            pageSize: Number(pageSize ?? 10),
          };
          const result = await listActivationCodes(query);
          return {
            data: result?.rows ?? [],
            success: true,
            total: Number(result?.total ?? 0),
          };
        }}
        columns={columns}
        pagination={{
          showQuickJumper: true,
          showSizeChanger: true,
          defaultPageSize: 10,
        }}
        toolBarRender={() => [
          <PermissionButton
            key="generate"
            type="primary"
            access={ADMIN_PERMISSIONS.mallCoupon.activationBatchGenerate}
            onClick={() => setGenerateDrawerOpen(true)}
          >
            生成激活码
          </PermissionButton>,
        ]}
      />

      <ActivationCodeGenerateModal
        open={generateDrawerOpen}
        onOpenChange={setGenerateDrawerOpen}
        onGenerated={() => {
          actionRef.current?.reload();
        }}
      />

      {/* 批次详情抽屉 */}
      <Drawer
        title="批次详情"
        open={detailDrawerOpen}
        width={660}
        destroyOnHidden
        onClose={() => setDetailDrawerOpen(false)}
        footer={
          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Space>
              {currentBatchDetail?.id && (
                <PermissionButton
                  type="primary"
                  access={ADMIN_PERMISSIONS.mallCoupon.activationBatchQuery}
                  onClick={() => {
                    setDetailDrawerOpen(false);
                    navigate(buildMallCouponActivationCodesPath(currentBatchDetail.id as number));
                  }}
                >
                  查看激活码
                </PermissionButton>
              )}
              <Button onClick={() => setDetailDrawerOpen(false)}>关闭</Button>
            </Space>
          </div>
        }
      >
        <div style={{ maxWidth: 580, margin: '0 auto' }}>
          {/* 基本信息 */}
          <Divider orientation="left" orientationMargin={0} style={{ marginTop: 0 }}>
            <Typography.Text type="secondary" style={{ fontSize: 13 }}>
              基本信息
            </Typography.Text>
          </Divider>
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="批次ID">{currentBatchDetail?.id ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="批次号">
              {currentBatchDetail?.batchNo ? (
                <Typography.Text copyable={{ text: currentBatchDetail.batchNo }}>
                  {currentBatchDetail.batchNo}
                </Typography.Text>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="模板ID">
              {currentBatchDetail?.templateId ?? '-'}
            </Descriptions.Item>
            <Descriptions.Item label="模板名称">
              {currentBatchDetail?.templateName || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="兑换规则">
              {activationCodeModeText(currentBatchDetail?.redeemRuleType)}
            </Descriptions.Item>
            <Descriptions.Item label="批次状态">
              {currentBatchDetail?.status ? (
                <Tag
                  color={
                    ACTIVATION_CODE_STATUS_TAG_COLOR[
                      currentBatchDetail.status as MallCouponTypes.ActivationCodeStatus
                    ]
                  }
                >
                  {activationCodeStatusText(currentBatchDetail.status)}
                </Tag>
              ) : (
                '-'
              )}
            </Descriptions.Item>
          </Descriptions>

          {/* 使用统计 */}
          <Divider orientation="left" orientationMargin={0}>
            <Typography.Text type="secondary" style={{ fontSize: 13 }}>
              使用统计
            </Typography.Text>
          </Divider>
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="生成数量">
              <Typography.Text strong>{currentBatchDetail?.generateCount ?? 0}</Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="成功使用次数">
              <Typography.Text
                strong
                style={{ color: currentBatchDetail?.successUseCount ? '#1677ff' : undefined }}
              >
                {currentBatchDetail?.successUseCount ?? 0}
              </Typography.Text>
            </Descriptions.Item>
          </Descriptions>

          {/* 有效期设置 */}
          <Divider orientation="left" orientationMargin={0}>
            <Typography.Text type="secondary" style={{ fontSize: 13 }}>
              有效期设置
            </Typography.Text>
          </Divider>
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="有效期类型">
              {activationValidityText(currentBatchDetail?.validityType)}
            </Descriptions.Item>
            <Descriptions.Item label="有效期配置">
              {buildActivationValidityDisplay(currentBatchDetail || {})}
            </Descriptions.Item>
          </Descriptions>

          {/* 其他信息 */}
          <Divider orientation="left" orientationMargin={0}>
            <Typography.Text type="secondary" style={{ fontSize: 13 }}>
              其他信息
            </Typography.Text>
          </Divider>
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="创建人">
              {currentBatchDetail?.createBy || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {currentBatchDetail?.createTime || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="备注" span={2}>
              {currentBatchDetail?.remark || (
                <Typography.Text type="secondary">暂无备注</Typography.Text>
              )}
            </Descriptions.Item>
          </Descriptions>
        </div>
      </Drawer>
    </>
  );
};

export default ActivationCodeManageTab;
