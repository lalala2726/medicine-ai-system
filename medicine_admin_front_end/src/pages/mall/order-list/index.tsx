import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Image, Modal, message, Tabs, Tag } from 'antd';
import React, { useCallback, useMemo, useRef, useState } from 'react';
import { deleteOrders, getOrderList, type MallOrderTypes } from '@/api/mall/order';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import OrderDetail from '@/pages/mall/order-list/components/OrderDetail';
import ShipOrderModal from '@/pages/mall/order-list/components/ShipOrderModal';
import { canShipOrder } from '@/types/orderStatus';

type OrderRecord = MallOrderTypes.MallOrderListVo;
type OrderQuery = MallOrderTypes.MallOrderListRequest & {
  pageNum?: number;
  pageSize?: number;
};

const OrderList: React.FC = () => {
  const actionRef = useRef<ActionType | null>(null);
  const [selectedRows, setSelectedRows] = useState<OrderRecord[]>([]);
  const [messageApi, contextHolder] = message.useMessage();
  const [activeStatus, setActiveStatus] = useState<string | undefined>(undefined);
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentOrderId, setCurrentOrderId] = useState<string>('');
  const [shipModalVisible, setShipModalVisible] = useState(false);
  const [currentShipOrder, setCurrentShipOrder] = useState<{
    orderId: string;
    orderNo?: string;
  } | null>(null);

  // 订单状态映射
  const statusMap = {
    PENDING_PAYMENT: { text: '待支付', color: 'warning' },
    PENDING_SHIPMENT: { text: '待发货', color: 'processing' },
    PENDING_RECEIPT: { text: '待收货', color: 'processing' },
    COMPLETED: { text: '已完成', color: 'success' },
    REFUNDED: { text: '已退款', color: 'error' },
    AFTER_SALE: { text: '售后中', color: 'warning' },
    EXPIRED: { text: '已过期', color: 'error' },
  };

  // 支付方式映射
  const payTypeMap = {
    WALLET: { text: '钱包', color: 'orange' },
    WAIT_PAY: { text: '待支付', color: 'default' },
  };

  const handleDetail = useCallback(
    (record: OrderRecord) => {
      if (!record.id) {
        messageApi.warning('缺少订单ID，无法查看详情');
        return;
      }
      setCurrentOrderId(record.id);
      setDetailVisible(true);
    },
    [messageApi],
  );

  const handleDetailClose = useCallback(() => {
    setDetailVisible(false);
    setCurrentOrderId('');
  }, []);

  const handleShip = useCallback(
    (record: OrderRecord) => {
      if (!record.id) {
        messageApi.warning('缺少订单ID，无法发货');
        return;
      }
      setCurrentShipOrder({
        orderId: record.id,
        orderNo: record.orderNo,
      });
      setShipModalVisible(true);
    },
    [messageApi],
  );

  const handleShipModalClose = useCallback(() => {
    setShipModalVisible(false);
    setCurrentShipOrder(null);
  }, []);

  const handleDetailSuccess = useCallback(() => {
    actionRef.current?.reload();
  }, []);

  const handleShipSuccess = useCallback(() => {
    actionRef.current?.reload();
  }, []);

  // 检查订单是否可以删除
  const canDeleteOrder = useCallback((status?: string) => {
    return status === 'COMPLETED' || status === 'EXPIRED' || status === 'CANCELLED';
  }, []);

  // 删除单个订单
  const handleDelete = useCallback(
    (record: OrderRecord) => {
      if (!record.id) {
        messageApi.warning('缺少订单ID，无法删除');
        return;
      }

      if (!canDeleteOrder(record.orderStatus)) {
        messageApi.warning('只有已完成、已过期或已取消的订单才能删除');
        return;
      }

      Modal.confirm({
        title: '删除订单',
        content: `确定要删除订单 ${record.orderNo} 吗？此操作不可恢复。`,
        okText: '确定删除',
        cancelText: '取消',
        okType: 'danger',
        onOk: async () => {
          try {
            await deleteOrders({ ids: [record.id!] });
            messageApi.success('订单删除成功');
            actionRef.current?.reload();
          } catch (error) {
            messageApi.error('订单删除失败');
          }
        },
      });
    },
    [messageApi, canDeleteOrder],
  );

  // 批量删除订单
  const handleBatchDelete = useCallback(() => {
    if (selectedRows.length === 0) {
      messageApi.warning('请选择要删除的订单');
      return;
    }

    // 检查选中的订单是否都可以删除
    const invalidOrders = selectedRows.filter((order) => !canDeleteOrder(order.orderStatus));

    if (invalidOrders.length > 0) {
      messageApi.warning(
        `只有已完成、已过期或已取消的订单才能删除。选中的订单中有 ${invalidOrders.length} 个不符合条件。`,
      );
      return;
    }

    Modal.confirm({
      title: '批量删除订单',
      content: `确定要删除选中的 ${selectedRows.length} 个订单吗？此操作不可恢复。`,
      okText: '确定删除',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        try {
          const ids = selectedRows.map((order) => order.id).filter(Boolean) as string[];
          await deleteOrders({ ids });
          messageApi.success(`成功删除 ${ids.length} 个订单`);
          setSelectedRows([]);
          actionRef.current?.reload();
        } catch (error) {
          messageApi.error('批量删除订单失败');
        }
      },
    });
  }, [selectedRows, messageApi, canDeleteOrder]);

  const columns: ProColumns<OrderRecord>[] = useMemo(
    () => [
      {
        title: '订单编号',
        dataIndex: 'orderNo',
        width: 160,
        ellipsis: true,
        copyable: true,
      },
      {
        title: '商品信息',
        dataIndex: 'productInfo',
        width: 120,
        hideInSearch: true,
        render: (_, record) => {
          const productInfo = record.productInfo;
          if (!productInfo) {
            return '-';
          }

          return (
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <Image
                src={productInfo.productImage}
                alt={productInfo.productName}
                width={50}
                height={50}
                style={{ objectFit: 'cover', borderRadius: 4 }}
                preview={{
                  mask: '预览',
                }}
              />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div
                  style={{
                    fontWeight: 500,
                    color: '#262626',
                    marginBottom: 2,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {productInfo.productName || '未知商品'}
                </div>
                <div
                  style={{
                    fontSize: 12,
                    color: '#8c8c8c',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 4,
                  }}
                >
                  <span>数量: {productInfo.quantity || 0}</span>
                </div>
              </div>
            </div>
          );
        },
      },
      {
        title: '支付方式',
        dataIndex: 'payType',
        width: 100,
        valueType: 'select',
        valueEnum: {
          WALLET: { text: '钱包' },
          WAIT_PAY: { text: '待支付' },
        },
        render: (_, record) => {
          const payType = payTypeMap[record.payType as keyof typeof payTypeMap];
          return payType ? <Tag color={payType.color}>{payType.text}</Tag> : '-';
        },
      },
      {
        title: '订单状态',
        dataIndex: 'orderStatus',
        width: 100,
        hideInSearch: true,
        render: (_, record) => {
          const status = statusMap[record.orderStatus as keyof typeof statusMap];
          return status ? <Tag color={status.color}>{status.text}</Tag> : '-';
        },
      },
      {
        title: '订单金额',
        dataIndex: 'totalAmount',
        width: 100,
        hideInSearch: true,
        render: (text) => (text ? `¥${Number(text).toFixed(2)}` : '-'),
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        valueType: 'dateTime',
        width: 160,
        hideInSearch: true,
      },
      {
        title: '操作',
        dataIndex: 'option',
        valueType: 'option',
        width: 200,
        fixed: 'right',
        align: 'center',
        render: (_, record) => (
          <TableActionGroup>
            <PermissionButton
              type="link"
              access={ADMIN_PERMISSIONS.mallOrder.query}
              onClick={() => handleDetail(record)}
              style={{ padding: '4px 8px', height: 'auto', lineHeight: 1 }}
            >
              详情
            </PermissionButton>
            {canShipOrder(record.orderStatus) && (
              <PermissionButton
                type="link"
                access={ADMIN_PERMISSIONS.mallOrder.ship}
                onClick={() => handleShip(record)}
              >
                发货
              </PermissionButton>
            )}
            {canDeleteOrder(record.orderStatus) && (
              <PermissionButton
                type="link"
                danger
                access={ADMIN_PERMISSIONS.mallOrder.delete}
                onClick={() => handleDelete(record)}
                style={{ padding: '4px 8px', height: 'auto', lineHeight: 1 }}
              >
                删除
              </PermissionButton>
            )}
          </TableActionGroup>
        ),
      },
    ],
    [handleDetail, handleShip, handleDelete, canDeleteOrder, payTypeMap, statusMap],
  );

  // 状态标签配置
  const statusTabs = [
    { label: '全部订单', key: 'all', value: undefined },
    { label: '待支付', key: 'PENDING_PAYMENT', value: 'PENDING_PAYMENT' },
    { label: '待发货', key: 'PENDING_SHIPMENT', value: 'PENDING_SHIPMENT' },
    { label: '待收货', key: 'PENDING_RECEIPT', value: 'PENDING_RECEIPT' },
    { label: '已完成', key: 'COMPLETED', value: 'COMPLETED' },
    { label: '已退款', key: 'REFUNDED', value: 'REFUNDED' },
    { label: '售后中', key: 'AFTER_SALE', value: 'AFTER_SALE' },
    { label: '已取消', key: 'CANCELLED', value: 'CANCELLED' },
    { label: '已过期', key: 'EXPIRED', value: 'EXPIRED' },
  ];

  return (
    <PageContainer>
      {contextHolder}

      <Tabs
        activeKey={activeStatus ?? 'all'}
        onChange={(key) => {
          const tab = statusTabs.find((t) => t.key === key);
          setActiveStatus(tab?.value);
          actionRef.current?.reload();
        }}
        items={statusTabs}
        style={{ marginBottom: 16 }}
      />
      <ProTable<OrderRecord, OrderQuery>
        headerTitle="订单列表"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 120 }}
        toolBarRender={() => [
          <PermissionButton
            key="batchDelete"
            danger
            access={ADMIN_PERMISSIONS.mallOrder.delete}
            disabled={selectedRows.length === 0}
            onClick={handleBatchDelete}
          >
            批量删除 ({selectedRows.length})
          </PermissionButton>,
        ]}
        rowSelection={{
          selectedRowKeys: selectedRows
            .map((row) => row.id)
            .filter((id): id is string => Boolean(id)),
          onChange: (_, rows) => setSelectedRows(rows),
          getCheckboxProps: (record) => ({
            disabled: !canDeleteOrder(record.orderStatus),
          }),
        }}
        request={async (params) => {
          const { current, pageSize, ...rest } = params;
          const currentPage = Number(current ?? 1);
          const pageSizeValue = Number(pageSize ?? 10);
          const query: OrderQuery = {
            ...(rest as OrderQuery),
            orderStatus: activeStatus,
            pageNum: currentPage,
            pageSize: pageSizeValue,
          };

          const result = await getOrderList(query);

          // 后端返回正确的分页对象结构：{total, pageNum, pageSize, rows}
          const orderData = result?.rows || [];
          const total = Number(result?.total) || 0;

          return {
            data: orderData,
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
      <OrderDetail
        open={detailVisible}
        onClose={handleDetailClose}
        orderId={currentOrderId}
        onSuccess={handleDetailSuccess}
      />
      <ShipOrderModal
        open={shipModalVisible}
        onClose={handleShipModalClose}
        orderId={currentShipOrder?.orderId || ''}
        orderNo={currentShipOrder?.orderNo}
        onSuccess={handleShipSuccess}
      />
    </PageContainer>
  );
};

export default OrderList;
