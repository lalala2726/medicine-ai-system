import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Descriptions, Modal, Tag, Typography, message, Switch, Tooltip } from 'antd';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  deleteActivationCodeItemById,
  downloadActivationBatchCodesExcel,
  getActivationCodeById,
  listActivationBatchCodes,
  updateActivationCodeItemStatus,
  type MallCouponTypes,
} from '@/api/mall/coupon';
import PermissionButton, { NO_PERMISSION_BUTTON_TIP } from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { usePermission } from '@/hooks/usePermission';

type ActivationCodeRecord = MallCouponTypes.ActivationCodeGeneratedItemVo;

/**
 * 激活码 Excel 导出文件名前缀。
 */
const ACTIVATION_CODE_EXPORT_FILE_NAME_PREFIX = '激活码列表-';

/**
 * 激活码 Excel 导出文件后缀。
 */
const ACTIVATION_CODE_EXPORT_FILE_NAME_SUFFIX = '.xlsx';

/**
 * 激活码导出接口在业务异常场景下返回的 Blob 错误标识。
 */
const ACTIVATION_CODE_EXPORT_BLOB_ERROR = 'ACTIVATION_CODE_EXPORT_BLOB_ERROR';

/**
 * 兑换规则选项。
 */
const ACTIVATION_CODE_MODE_OPTIONS = [
  { label: '共享码', value: 'SHARED_PER_USER_ONCE' },
  { label: '唯一码', value: 'UNIQUE_SINGLE_USE' },
];

/**
 * 激活码有效期类型选项。
 */
const ACTIVATION_VALIDITY_TYPE_OPTIONS = [
  { label: '一次性', value: 'ONCE' },
  { label: '激活后计算', value: 'AFTER_ACTIVATION' },
];

/**
 * 批次状态配置。
 */
const BATCH_STATUS_CONFIG: Record<string, { color: string; text: string }> = {
  ACTIVE: { color: 'success', text: '启用' },
  DISABLED: { color: 'error', text: '停用' },
};

function activationCodeModeText(value?: string): string {
  return ACTIVATION_CODE_MODE_OPTIONS.find((item) => item.value === value)?.label ?? value ?? '-';
}

function activationValidityText(value?: string): string {
  return (
    ACTIVATION_VALIDITY_TYPE_OPTIONS.find((item) => item.value === value)?.label ?? value ?? '-'
  );
}

function buildValidityDisplay(record: {
  validityType?: string;
  fixedEffectiveTime?: string;
  fixedExpireTime?: string;
  relativeValidDays?: number;
}): string {
  if (record.validityType === 'ONCE') {
    return `${record.fixedEffectiveTime || '-'} 至 ${record.fixedExpireTime || '-'}`;
  }
  return record.relativeValidDays ? `激活后 ${record.relativeValidDays} 天` : '-';
}

/**
 * 构建激活码 Excel 文件名。
 * @param batchNo 激活码批次号。
 * @returns 激活码 Excel 文件名。
 */
function buildActivationCodeExcelFileName(batchNo: string): string {
  return `${ACTIVATION_CODE_EXPORT_FILE_NAME_PREFIX}${batchNo}${ACTIVATION_CODE_EXPORT_FILE_NAME_SUFFIX}`;
}

/**
 * 下载浏览器 Blob 文件。
 * @param blob 导出二进制数据。
 * @param fileName 导出文件名。
 * @returns 无返回值。
 */
function downloadBlobFile(blob: Blob, fileName: string): void {
  const downloadUrl = URL.createObjectURL(blob);
  const downloadLink = document.createElement('a');
  downloadLink.href = downloadUrl;
  downloadLink.download = fileName;
  document.body.appendChild(downloadLink);
  downloadLink.click();
  document.body.removeChild(downloadLink);
  URL.revokeObjectURL(downloadUrl);
}

/**
 * 解析最近一次激活用户展示文案。
 * @param record 激活码明细记录。
 * @returns 最近一次激活用户展示文案。
 */
function resolveLastSuccessUserText(record: ActivationCodeRecord): string {
  if (record.lastSuccessUserName) {
    return record.lastSuccessUserName;
  }
  if (record.lastSuccessUserId !== undefined && record.lastSuccessUserId !== null) {
    return String(record.lastSuccessUserId);
  }
  return '-';
}

/**
 * 激活码批次详情页面。
 * @returns 页面节点。
 */
const ActivationCodesPage: React.FC = () => {
  const { batchId } = useParams<{ batchId: string }>();
  const [messageApi, contextHolder] = message.useMessage();
  const { canAccess } = usePermission();

  const [batchDetail, setBatchDetail] = useState<MallCouponTypes.ActivationCodeDetailVo | null>(
    null,
  );
  const [loadingCodeId, setLoadingCodeId] = useState<number | null>(null);
  const [exportLoading, setExportLoading] = useState(false);

  const numericBatchId = batchId ? Number(batchId) : undefined;
  const actionRef = useRef<ActionType | undefined>(undefined);
  const canMaintainActivationCode = canAccess(ADMIN_PERMISSIONS.mallCoupon.activationBatchStatus);

  /**
   * 加载批次详情。
   */
  const loadBatchDetail = useCallback(async () => {
    if (!numericBatchId) return;
    try {
      const detail = await getActivationCodeById(numericBatchId);
      setBatchDetail(detail ?? null);
    } catch (error) {
      console.error('加载批次详情失败:', error);
    }
  }, [numericBatchId]);

  useEffect(() => {
    void loadBatchDetail();
  }, [loadBatchDetail]);

  /**
   * 导出当前批次下的全部激活码 Excel。
   */
  const handleExportExcel = useCallback(async () => {
    if (!numericBatchId || !batchDetail?.batchNo) {
      return;
    }
    try {
      setExportLoading(true);
      const blob = await downloadActivationBatchCodesExcel(numericBatchId);
      downloadBlobFile(blob, buildActivationCodeExcelFileName(batchDetail.batchNo));
    } catch (error) {
      if (error instanceof Error && error.message === ACTIVATION_CODE_EXPORT_BLOB_ERROR) {
        messageApi.error('导出激活码列表失败');
      }
    } finally {
      setExportLoading(false);
    }
  }, [batchDetail?.batchNo, messageApi, numericBatchId]);

  /**
   * 切换单个激活码状态。
   */
  const handleToggleCodeStatus = useCallback(
    (record: ActivationCodeRecord) => {
      if (!record.id || !record.status) {
        messageApi.warning('缺少激活码状态信息，无法更新');
        return;
      }
      if (record.status === 'USED') {
        messageApi.warning('已使用激活码不支持状态切换');
        return;
      }
      const nextStatus: MallCouponTypes.ActivationCodeStatus =
        record.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
      Modal.confirm({
        title: nextStatus === 'ACTIVE' ? '确认启用激活码' : '确认停用激活码',
        content: `确定要将激活码「${record.plainCode || record.id}」设置为${nextStatus === 'ACTIVE' ? '启用' : '停用'}吗？`,
        okText: '确认',
        cancelText: '取消',
        onOk: async () => {
          try {
            setLoadingCodeId(record.id as number);
            await updateActivationCodeItemStatus({
              id: record.id as number,
              status: nextStatus,
            });
            messageApi.success('激活码状态更新成功');
            actionRef.current?.reload();
          } catch (error) {
            console.error('更新激活码状态失败:', error);
            messageApi.error('更新激活码状态失败');
          } finally {
            setLoadingCodeId(null);
          }
        },
      });
    },
    [messageApi],
  );

  /**
   * 删除单个激活码。
   */
  const handleDeleteCode = useCallback(
    (record: ActivationCodeRecord) => {
      if (!record.id) {
        messageApi.warning('缺少激活码ID，无法删除');
        return;
      }
      Modal.confirm({
        title: '确认删除激活码',
        content: `确定删除激活码「${record.plainCode || record.id}」吗？删除后不可恢复。`,
        okText: '确认删除',
        okButtonProps: { danger: true },
        cancelText: '取消',
        onOk: async () => {
          try {
            setLoadingCodeId(record.id as number);
            await deleteActivationCodeItemById(record.id as number);
            messageApi.success('激活码删除成功');
            actionRef.current?.reload();
          } catch (error) {
            console.error('删除激活码失败:', error);
            messageApi.error('删除激活码失败');
          } finally {
            setLoadingCodeId(null);
          }
        },
      });
    },
    [messageApi],
  );

  const batchStatusConfig = BATCH_STATUS_CONFIG[batchDetail?.status ?? ''];

  const columns: ProColumns<ActivationCodeRecord>[] = [
    {
      title: '激活码',
      dataIndex: 'plainCode',
      copyable: true,
      ellipsis: true,
      render: (_, record) => (
        <Typography.Text
          copyable={record.plainCode ? { text: record.plainCode } : false}
          style={{
            fontFamily: "'SFMono-Regular','Consolas','Liberation Mono','Menlo',monospace",
            fontSize: 14,
            fontWeight: 600,
            letterSpacing: '0.04em',
          }}
          ellipsis={{ tooltip: record.plainCode }}
        >
          {record.plainCode || '-'}
        </Typography.Text>
      ),
    },
    {
      title: '使用次数',
      dataIndex: 'successUseCount',
      hideInSearch: true,
      render: (_, record) => `${record.successUseCount ?? 0} 次`,
    },
    {
      title: '最近激活时间',
      dataIndex: 'lastSuccessTime',
      hideInSearch: true,
      render: (_, record) => record.lastSuccessTime || '-',
    },
    {
      title: '最近激活 IP',
      dataIndex: 'lastSuccessClientIp',
      hideInSearch: true,
      render: (_, record) => record.lastSuccessClientIp || '-',
    },
    {
      title: '最近激活用户',
      dataIndex: 'lastSuccessUserName',
      hideInSearch: true,
      render: (_, record) => resolveLastSuccessUserText(record),
    },
    {
      title: '状态',
      dataIndex: 'status',
      render: (_, record) => {
        const isUsed = record.status === 'USED';
        if (isUsed) {
          return <Tag color="default">已使用</Tag>;
        }

        const isActive = record.status === 'ACTIVE';
        const codeLoading = loadingCodeId === record.id;

        return (
          <Tooltip title={canMaintainActivationCode ? undefined : NO_PERMISSION_BUTTON_TIP}>
            <span>
              <Switch
                checked={isActive}
                loading={codeLoading}
                disabled={!canMaintainActivationCode}
                checkedChildren="启用"
                unCheckedChildren="停用"
                onChange={() => handleToggleCodeStatus(record)}
              />
            </span>
          </Tooltip>
        );
      },
    },
    {
      title: '操作',
      valueType: 'option',
      fixed: 'right',
      width: 80,
      align: 'center',
      render: (_, record) => {
        const isUsed = record.status === 'USED';
        const codeLoading = loadingCodeId === record.id;

        return (
          <TableActionGroup>
            <PermissionButton
              type="link"
              size="small"
              danger
              loading={codeLoading}
              access={ADMIN_PERMISSIONS.mallCoupon.activationBatchStatus}
              disabled={!record.id || isUsed}
              onClick={() => handleDeleteCode(record)}
            >
              删除
            </PermissionButton>
          </TableActionGroup>
        );
      },
    },
  ];

  return (
    <PageContainer
      subTitle={batchDetail?.batchNo ? `批次号：${batchDetail.batchNo}` : undefined}
      extra={[
        <PermissionButton
          key="exportExcel"
          loading={exportLoading}
          access={ADMIN_PERMISSIONS.mallCoupon.activationBatchQuery}
          disabled={!numericBatchId || !batchDetail?.batchNo}
          onClick={() => void handleExportExcel()}
        >
          导出 Excel
        </PermissionButton>,
      ]}
      content={
        batchDetail && (
          <div
            style={{
              background: '#fff',
              padding: '24px 24px 8px 24px',
              borderRadius: '12px',
              boxShadow: '0 2px 16px -8px rgba(0,0,0,0.05)',
              marginBottom: '20px',
              border: '1px solid rgba(0,0,0,0.04)',
            }}
          >
            <Descriptions
              title={
                <div
                  style={{
                    fontSize: '16px',
                    fontWeight: 600,
                    color: '#1f2937',
                    marginBottom: '8px',
                  }}
                >
                  批次基础信息
                </div>
              }
              column={{ xs: 1, sm: 2, md: 3, lg: 4 }}
              size="middle"
            >
              <Descriptions.Item label="批次号">
                <Typography.Text
                  copyable={{ text: batchDetail.batchNo ?? '' }}
                  style={{ color: '#1890ff', fontWeight: 500 }}
                >
                  {batchDetail.batchNo || '-'}
                </Typography.Text>
              </Descriptions.Item>
              <Descriptions.Item label="关联模板">
                <span style={{ fontWeight: 500, color: '#374151' }}>
                  {batchDetail.templateName || '-'}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="兑换规则">
                {activationCodeModeText(batchDetail.redeemRuleType)}
              </Descriptions.Item>
              <Descriptions.Item label="有效期类型">
                {activationValidityText(batchDetail.validityType)}
              </Descriptions.Item>
              <Descriptions.Item label="有效期配置">
                {buildValidityDisplay(batchDetail)}
              </Descriptions.Item>
              <Descriptions.Item label="生成数量">
                <span style={{ fontSize: '15px', fontWeight: 600, color: '#374151' }}>
                  {batchDetail.generateCount ?? 0}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="成功使用">
                <span style={{ fontSize: '15px', fontWeight: 600, color: '#52c41a' }}>
                  {batchDetail.successUseCount ?? 0}
                </span>{' '}
                次
              </Descriptions.Item>
              <Descriptions.Item label="批次状态">
                {batchStatusConfig ? (
                  <Tag
                    color={batchStatusConfig.color}
                    style={{ borderRadius: '4px', padding: '0 8px' }}
                  >
                    {batchStatusConfig.text}
                  </Tag>
                ) : (
                  batchDetail.status || '-'
                )}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                <span style={{ color: '#6b7280' }}>{batchDetail.createTime || '-'}</span>
              </Descriptions.Item>
              {batchDetail.remark && (
                <Descriptions.Item label="备注" span={4}>
                  <span style={{ color: '#6b7280' }}>{batchDetail.remark}</span>
                </Descriptions.Item>
              )}
            </Descriptions>
          </div>
        )
      }
    >
      {contextHolder}

      <ProTable<ActivationCodeRecord>
        rowKey="id"
        actionRef={actionRef}
        headerTitle="激活码明细"
        search={false}
        cardProps={{ bordered: false, style: { boxShadow: 'none' } }}
        request={async (params) => {
          if (!numericBatchId) return { data: [], success: true, total: 0 };
          const { current, pageSize } = params;
          try {
            const result = await listActivationBatchCodes(numericBatchId, {
              pageNum: current,
              pageSize: pageSize,
            });
            return {
              data: result?.rows ?? [],
              success: true,
              total: Number(result?.total ?? 0),
            };
          } catch (error) {
            console.error('加载激活码列表失败:', error);
            messageApi.error('加载激活码列表失败');
            return { data: [], success: false, total: 0 };
          }
        }}
        columns={columns}
        pagination={{
          showQuickJumper: true,
          showSizeChanger: true,
          defaultPageSize: 20,
          pageSizeOptions: ['20', '50', '100'],
        }}
      />
    </PageContainer>
  );
};

export default ActivationCodesPage;
