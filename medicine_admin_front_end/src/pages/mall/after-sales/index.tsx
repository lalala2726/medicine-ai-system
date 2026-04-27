import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { useRequest } from 'ahooks';
import { Button, Drawer, Image, Modal, message, Space, Tabs, Tag } from 'antd';
import React, { useCallback, useMemo, useRef, useState } from 'react';
import {
  auditAfterSale,
  getAfterSaleDetail,
  getAfterSaleList,
  type MallAfterSaleTypes,
  processExchange,
  processRefund,
} from '@/api/mall/mallAfterSale';
import {
  AfterSaleStatus,
  AFTER_SALE_STATUS_MAP,
  AfterSaleType,
  AFTER_SALE_TYPE_MAP,
  getAfterSaleReasonText,
} from '@/types/afterSale';
import AfterSaleDetailTab from './components/AfterSaleDetailTab';
import AfterSaleTimeline from './components/AfterSaleTimeline';
import ProductInfoTab from './components/ProductInfoTab';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';

type AfterSaleRecord = MallAfterSaleTypes.AfterSaleListVo;
type AfterSaleQuery = MallAfterSaleTypes.AfterSaleListRequest & {
  pageNum?: number;
  pageSize?: number;
};

const AfterSaleManagePage: React.FC = () => {
  const actionRef = useRef<ActionType | null>(null);
  const [messageApi, contextHolder] = message.useMessage();
  const [activeStatus, setActiveStatus] = useState<string | undefined>(undefined);
  const [detailDrawerVisible, setDetailDrawerVisible] = useState(false);
  const [currentDetail, setCurrentDetail] = useState<MallAfterSaleTypes.AfterSaleDetailVo | null>(
    null,
  );
  const [activeDetailTab, setActiveDetailTab] = useState('detail');
  const [rejectReason, setRejectReason] = useState('');

  // 获取售后详情
  const [detailLoading, setDetailLoading] = useState(false);
  const fetchDetail = async (afterSaleId: string) => {
    try {
      setDetailLoading(true);
      const data = await getAfterSaleDetail(afterSaleId);
      setCurrentDetail(data);
      setDetailDrawerVisible(true);
      setActiveDetailTab('detail');
      setRejectReason('');
    } catch (_error) {
      messageApi.error('获取售后详情失败');
    } finally {
      setDetailLoading(false);
    }
  };

  // 审核售后
  const { run: auditRun, loading: auditLoading } = useRequest(auditAfterSale, {
    manual: true,
    onSuccess: () => {
      messageApi.success('审核成功');
      setDetailDrawerVisible(false);
      actionRef.current?.reload();
    },
    onError: () => {
      messageApi.error('审核失败');
    },
  });

  // 处理退款
  const { run: refundRun } = useRequest(processRefund, {
    manual: true,
    onSuccess: () => {
      messageApi.success('退款处理成功');
      actionRef.current?.reload();
    },
    onError: () => {
      messageApi.error('退款处理失败');
    },
  });

  // 处理换货
  const { run: exchangeRun } = useRequest(processExchange, {
    manual: true,
    onSuccess: () => {
      messageApi.success('换货处理成功');
      actionRef.current?.reload();
    },
    onError: () => {
      messageApi.error('换货处理失败');
    },
  });

  // 查看详情
  const handleViewDetail = useCallback(
    (record: AfterSaleRecord) => {
      if (!record.id) {
        messageApi.warning('缺少售后ID');
        return;
      }
      fetchDetail(record.id);
    },
    [fetchDetail, messageApi],
  );

  // 审核通过（在抽屉内）
  const handleApproveInDrawer = useCallback(() => {
    if (!currentDetail?.id) {
      messageApi.warning('缺少售后ID');
      return;
    }
    Modal.confirm({
      title: '审核通过',
      content: '确定要通过该售后申请吗？',
      onOk: () => {
        return auditRun({
          afterSaleId: currentDetail.id || '',
          approved: true,
        });
      },
    });
  }, [currentDetail, auditRun, messageApi]);

  // 审核拒绝（在抽屉内）
  const handleRejectInDrawer = useCallback(() => {
    if (!currentDetail?.id) {
      messageApi.warning('缺少售后ID');
      return;
    }
    if (!rejectReason?.trim()) {
      messageApi.warning('请输入拒绝原因');
      return;
    }
    Modal.confirm({
      title: '审核拒绝',
      content: `确定要拒绝该售后申请吗？拒绝原因：${rejectReason}`,
      onOk: () => {
        return auditRun({
          afterSaleId: currentDetail.id || '',
          approved: false,
          rejectReason,
        });
      },
    });
  }, [currentDetail, rejectReason, auditRun, messageApi]);

  // 处理退款
  const handleProcessRefund = useCallback(
    (afterSaleId: string) => {
      Modal.confirm({
        title: '处理退款',
        content: '确定要处理该退款申请吗？退款将原路返回到用户账户。',
        onOk: () => {
          return refundRun({
            afterSaleId,
          });
        },
      });
    },
    [refundRun],
  );

  // 处理换货
  const handleProcessExchange = useCallback(
    (afterSaleId: string) => {
      let logisticsCompanyValue = '';
      let trackingNumberValue = '';

      Modal.confirm({
        title: '处理换货',
        content: (
          <div>
            <p>物流公司：</p>
            <input
              style={{ width: '100%', marginBottom: 10 }}
              placeholder="请输入物流公司"
              onChange={(e) => {
                logisticsCompanyValue = e.target.value;
              }}
            />
            <p>物流单号：</p>
            <input
              style={{ width: '100%' }}
              placeholder="请输入物流单号"
              onChange={(e) => {
                trackingNumberValue = e.target.value;
              }}
            />
          </div>
        ),
        onOk: () => {
          if (!logisticsCompanyValue?.trim() || !trackingNumberValue?.trim()) {
            messageApi.warning('请输入物流信息');
            return Promise.reject();
          }
          return exchangeRun({
            afterSaleId,
            logisticsCompany: logisticsCompanyValue,
            trackingNumber: trackingNumberValue,
          });
        },
      });
    },
    [exchangeRun, messageApi],
  );

  // 售后类型标签
  const getAfterSaleTypeTag = (type?: string, typeName?: string) => {
    const config = AFTER_SALE_TYPE_MAP[type as AfterSaleType];
    const text = typeName || config?.text || type || '-';
    const color = config?.color || 'default';
    return <Tag color={color}>{text}</Tag>;
  };

  // 售后状态标签
  const getAfterSaleStatusTag = (status?: string, statusName?: string) => {
    const config = AFTER_SALE_STATUS_MAP[status as AfterSaleStatus];
    const text = statusName || config?.text || status || '-';
    const color = config?.color || 'default';
    return <Tag color={color}>{text}</Tag>;
  };

  const columns: ProColumns<AfterSaleRecord>[] = useMemo(
    () => [
      {
        title: '售后单号',
        dataIndex: 'afterSaleNo',
        width: 160,
        copyable: true,
      },
      {
        title: '订单编号',
        dataIndex: 'orderNo',
        width: 160,
        copyable: true,
      },
      {
        title: '商品信息',
        dataIndex: 'productName',
        width: 200,
        hideInSearch: true,
        render: (_, record) => (
          <Space>
            {record.productImage ? (
              <Image
                width={40}
                height={40}
                src={record.productImage}
                alt="商品图片"
                style={{ objectFit: 'cover' }}
                preview={{
                  mask: '预览',
                }}
              />
            ) : null}
            <span>{record.productName || '-'}</span>
          </Space>
        ),
      },
      {
        title: '用户信息',
        dataIndex: 'userNickname',
        width: 120,
        hideInSearch: true,
        render: (_, record) => (
          <div>
            <div>{record.userNickname || '-'}</div>
            <div style={{ fontSize: 12, color: '#999' }}>ID: {record.userId || '-'}</div>
          </div>
        ),
      },
      {
        title: '用户ID',
        dataIndex: 'userId',
        hideInTable: true,
      },
      {
        title: '售后类型',
        dataIndex: 'afterSaleType',
        width: 100,
        valueType: 'select',
        valueEnum: {
          [AfterSaleType.REFUND_ONLY]: { text: '仅退款' },
          [AfterSaleType.RETURN_REFUND]: { text: '退货退款' },
          [AfterSaleType.EXCHANGE]: { text: '换货' },
        },
        render: (_, record) => getAfterSaleTypeTag(record.afterSaleType, record.afterSaleTypeName),
      },
      {
        title: '售后状态',
        dataIndex: 'afterSaleStatus',
        width: 100,
        hideInSearch: true,
        render: (_, record) =>
          getAfterSaleStatusTag(record.afterSaleStatus, record.afterSaleStatusName),
      },
      {
        title: '退款金额',
        dataIndex: 'refundAmount',
        width: 100,
        hideInSearch: true,
        render: (text) => (text ? `¥${Number(text).toFixed(2)}` : '-'),
      },
      {
        title: '申请原因',
        dataIndex: 'applyReasonName',
        width: 120,
        ellipsis: true,
        hideInSearch: true,
        render: (text, record: any) => text || getAfterSaleReasonText(record.applyReason) || '-',
      },
      {
        title: '申请时间',
        dataIndex: 'applyTime',
        valueType: 'dateTime',
        width: 160,
        hideInSearch: true,
      },
      {
        title: '审核时间',
        dataIndex: 'auditTime',
        valueType: 'dateTime',
        width: 160,
        hideInSearch: true,
      },
      {
        title: '操作',
        dataIndex: 'option',
        valueType: 'option',
        width: 150,
        fixed: 'right',
        align: 'center',
        render: (_, record) => (
          <TableActionGroup>
            <PermissionButton
              type="link"
              size="small"
              access={ADMIN_PERMISSIONS.mallAfterSale.query}
              onClick={() => handleViewDetail(record)}
            >
              详情
            </PermissionButton>
            {record.afterSaleStatus === 'APPROVED' && record.afterSaleType === 'REFUND_ONLY' && (
              <PermissionButton
                type="link"
                size="small"
                access={ADMIN_PERMISSIONS.mallAfterSale.refund}
                onClick={() => handleProcessRefund(record.id || '')}
              >
                处理退款
              </PermissionButton>
            )}
            {record.afterSaleStatus === 'APPROVED' && record.afterSaleType === 'EXCHANGE' && (
              <PermissionButton
                type="link"
                size="small"
                access={ADMIN_PERMISSIONS.mallAfterSale.exchange}
                onClick={() => handleProcessExchange(record.id || '')}
              >
                处理换货
              </PermissionButton>
            )}
          </TableActionGroup>
        ),
      },
    ],
    [handleViewDetail, handleProcessRefund, handleProcessExchange],
  );

  // 状态标签配置
  const statusTabs = [
    { label: '全部', key: 'all', value: undefined },
    { label: '待审核', key: 'pending', value: 'PENDING' },
    { label: '已通过', key: 'approved', value: 'APPROVED' },
    { label: '已拒绝', key: 'rejected', value: 'REJECTED' },
    { label: '处理中', key: 'processing', value: 'PROCESSING' },
    { label: '已完成', key: 'completed', value: 'COMPLETED' },
    { label: '已取消', key: 'cancelled', value: 'CANCELLED' },
  ];

  return (
    <PageContainer>
      {contextHolder}

      <Tabs
        activeKey={statusTabs.find((t) => t.value === activeStatus)?.key || 'all'}
        onChange={(key) => {
          const tab = statusTabs.find((t) => t.key === key);
          setActiveStatus(tab?.value);
          actionRef.current?.reload();
        }}
        items={statusTabs}
        style={{ marginBottom: 16 }}
      />

      <ProTable<AfterSaleRecord, AfterSaleQuery>
        headerTitle="售后管理"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 120 }}
        request={async (params) => {
          const { current, pageSize, ...rest } = params;
          const currentPage = Number(current ?? 1);
          const pageSizeValue = Number(pageSize ?? 10);
          const query: AfterSaleQuery = {
            ...(rest as AfterSaleQuery),
            afterSaleStatus: activeStatus,
            pageNum: currentPage,
            pageSize: pageSizeValue,
          };

          const result = await getAfterSaleList(query);
          const total = Number(result?.total ?? 0);

          return {
            data: result?.rows ?? [],
            success: true,
            total,
          };
        }}
        columns={columns}
        pagination={{
          showQuickJumper: true,
          showSizeChanger: true,
          defaultPageSize: 10,
        }}
      />

      {/* 售后详情抽屉 */}
      <Drawer
        title="售后详情"
        placement="right"
        open={detailDrawerVisible}
        onClose={() => setDetailDrawerVisible(false)}
        width={720}
        loading={detailLoading}
        footer={
          currentDetail?.afterSaleStatus === 'PENDING' ? (
            <Space style={{ float: 'right' }}>
              <Button onClick={() => setDetailDrawerVisible(false)}>取消</Button>
              <PermissionButton
                danger
                access={ADMIN_PERMISSIONS.mallAfterSale.audit}
                onClick={handleRejectInDrawer}
                loading={auditLoading}
              >
                拒绝
              </PermissionButton>
              <PermissionButton
                type="primary"
                access={ADMIN_PERMISSIONS.mallAfterSale.audit}
                onClick={handleApproveInDrawer}
                loading={auditLoading}
              >
                通过
              </PermissionButton>
            </Space>
          ) : null
        }
      >
        {currentDetail && (
          <Tabs
            activeKey={activeDetailTab}
            onChange={setActiveDetailTab}
            items={[
              {
                key: 'detail',
                label: '售后详情',
                children: (
                  <AfterSaleDetailTab
                    detail={currentDetail}
                    rejectReason={rejectReason}
                    onRejectReasonChange={setRejectReason}
                    getAfterSaleTypeTag={getAfterSaleTypeTag}
                    getAfterSaleStatusTag={getAfterSaleStatusTag}
                  />
                ),
              },
              {
                key: 'product',
                label: '商品信息',
                children: <ProductInfoTab productInfo={currentDetail.productInfo} />,
              },
              {
                key: 'timeline',
                label: '时间线',
                children: <AfterSaleTimeline timeline={currentDetail.timeline} />,
              },
            ]}
          />
        )}
      </Drawer>
    </PageContainer>
  );
};

export default AfterSaleManagePage;
