import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { MoreOutlined } from '@ant-design/icons';
import {
  Col,
  Descriptions,
  Drawer,
  Dropdown,
  Empty,
  Form,
  Input,
  type MenuProps,
  Modal,
  message,
  Row,
  Select,
  Space,
  Spin,
  Tabs,
  Typography,
} from 'antd';
import React, { useEffect, useState } from 'react';
import {
  cancelOrder,
  getOrderAddress,
  getOrderDetail,
  getOrderPrice,
  getOrderRemark,
  type MallOrderTypes,
  manualConfirmReceipt,
  orderRefund,
  updateOrderAddress,
  updateOrderPrice,
  updateOrderRemark,
} from '@/api/mall/order';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { usePermission } from '@/hooks/usePermission';
import {
  canCancelOrder,
  canChangePrice,
  canConfirmReceipt,
  canRefund,
  DELIVERY_TYPE_OPTIONS,
  isOrderPaid,
  ORDER_STATUS_MAP,
} from '@/types/orderStatus';
import DeliveryRecords from './DeliveryRecords';
import OrderInfo from './OrderInfo';
import OrderTimeline from './OrderTimeline';
import ProductInfo from './ProductInfo';

const { Text } = Typography;

interface OrderDetailProps {
  open: boolean;
  onClose: () => void;
  orderId: string;
  onSuccess?: () => void;
}

const OrderDetail: React.FC<OrderDetailProps> = ({ open, onClose, orderId, onSuccess }) => {
  const [loading, setLoading] = useState(false);
  const [orderDetail, setOrderDetail] = useState<MallOrderTypes.OrderDetailVo | null>(null);
  const [activeTab, setActiveTab] = useState<string>('order');
  const [refundLoading, setRefundLoading] = useState(false);
  const [cancelLoading, setCancelLoading] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();
  const [priceModalOpen, setPriceModalOpen] = useState(false);
  const [addressModalOpen, setAddressModalOpen] = useState(false);
  const [remarkModalOpen, setRemarkModalOpen] = useState(false);
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [priceForm] = Form.useForm();
  const [addressForm] = Form.useForm();
  const [remarkForm] = Form.useForm();
  const [cancelForm] = Form.useForm();
  const [priceData, setPriceData] = useState<MallOrderTypes.OrderPriceVo | null>(null);
  const [addressData, setAddressData] = useState<MallOrderTypes.OrderAddressVo | null>(null);
  const [remarkData, setRemarkData] = useState<MallOrderTypes.OrderRemarkVo | null>(null);
  const [confirmReceiptLoading, setConfirmReceiptLoading] = useState(false);
  const [confirmReceiptModalOpen, setConfirmReceiptModalOpen] = useState(false);
  const [confirmReceiptForm] = Form.useForm();
  const { canAccess } = usePermission();

  useEffect(() => {
    if (open && orderId) {
      fetchOrderDetail();
    }
  }, [open, orderId]);

  const fetchOrderDetail = async () => {
    if (!orderId) return;

    try {
      setLoading(true);
      const result = await getOrderDetail(orderId);
      setOrderDetail(result);
    } catch (error) {
      console.error('获取订单详情失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 操作处理函数
  const handleRefund = () => {
    if (!orderDetail?.orderInfo?.orderNo) {
      messageApi.error('订单信息不完整，无法退款');
      return;
    }

    Modal.confirm({
      title: '确认退款',
      content: `确定要退款订单 ${orderDetail.orderInfo.orderNo} 吗？`,
      okText: '确认退款',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        try {
          setRefundLoading(true);

          // 调用退款接口
          const refundData: MallOrderTypes.OrderRefundRequest = {
            orderNo: orderDetail.orderInfo?.orderNo || '',
            refundAmount: orderDetail.orderInfo?.payAmount,
            refundReason: '管理员操作退款',
          };

          await orderRefund(refundData);

          messageApi.success('退款成功');
          onClose();
          onSuccess?.(); // 刷新列表
        } catch (error) {
          console.error('退款失败:', error);
        } finally {
          setRefundLoading(false);
        }
      },
    });
  };

  // 改价功能
  const handlePriceChange = async () => {
    try {
      const data = await getOrderPrice(orderId);
      setPriceData(data);
      priceForm.setFieldsValue({
        totalAmount: data.totalAmount || '',
      });
      setPriceModalOpen(true);
    } catch (error) {
      console.error('获取订单价格信息失败:', error);
      messageApi.error('获取订单价格信息失败');
    }
  };

  const handlePriceSubmit = async () => {
    try {
      const values = await priceForm.validateFields();
      await updateOrderPrice({
        orderId: orderId,
        price: values.totalAmount,
      });
      messageApi.success('改价成功');
      setPriceModalOpen(false);
      await fetchOrderDetail(); // 刷新订单详情
      onSuccess?.(); // 刷新列表
    } catch (error) {
      console.error('改价失败:', error);
      messageApi.error('改价失败');
    }
  };

  // 修改地址功能
  const handleAddressChange = async () => {
    try {
      const data = await getOrderAddress(orderId);
      setAddressData(data);
      addressForm.setFieldsValue({
        receiverName: data.receiverName || '',
        receiverPhone: data.receiverPhone || '',
        receiverAddress: data.receiverDetail || '',
        deliveryType: data.deliveryType || '',
      });
      setAddressModalOpen(true);
    } catch (error) {
      console.error('获取订单地址信息失败:', error);
      messageApi.error('获取订单地址信息失败');
    }
  };

  const handleAddressSubmit = async () => {
    try {
      const values = await addressForm.validateFields();
      await updateOrderAddress({
        orderId: orderId,
        receiverName: values.receiverName,
        receiverPhone: values.receiverPhone,
        receiverAddress: values.receiverAddress,
        deliveryType: values.deliveryType,
      });
      messageApi.success('修改地址成功');
      setAddressModalOpen(false);
      await fetchOrderDetail(); // 刷新订单详情
      onSuccess?.(); // 刷新列表
    } catch (error) {
      console.error('修改地址失败:', error);
      messageApi.error('修改地址失败');
    }
  };

  // 修改备注功能
  const handleRemarkChange = async () => {
    try {
      const data = await getOrderRemark(orderId);
      setRemarkData(data);
      remarkForm.setFieldsValue({
        remark: data.remark || '',
      });
      setRemarkModalOpen(true);
    } catch (error) {
      console.error('获取订单备注信息失败:', error);
      messageApi.error('获取订单备注信息失败');
    }
  };

  const handleRemarkSubmit = async () => {
    try {
      const values = await remarkForm.validateFields();
      await updateOrderRemark({
        orderId: orderId,
        remark: values.remark,
      });
      messageApi.success('修改备注成功');
      setRemarkModalOpen(false);
      await fetchOrderDetail(); // 刷新订单详情
      onSuccess?.(); // 刷新列表
    } catch (error) {
      console.error('修改备注失败:', error);
      messageApi.error('修改备注失败');
    }
  };

  // 取消订单功能
  const handleCancelOrder = () => {
    if (!orderDetail?.orderInfo?.orderNo) {
      messageApi.error('订单信息不完整，无法取消');
      return;
    }

    cancelForm.resetFields();
    setCancelModalOpen(true);
  };

  // 手动确认收货功能
  const handleConfirmReceipt = () => {
    if (!orderDetail?.orderInfo?.orderNo) {
      messageApi.error('订单信息不完整，无法确认收货');
      return;
    }

    confirmReceiptForm.resetFields();
    setConfirmReceiptModalOpen(true);
  };

  const handleConfirmReceiptSubmit = async () => {
    try {
      const values = await confirmReceiptForm.validateFields();
      setConfirmReceiptLoading(true);
      await manualConfirmReceipt({
        orderId: orderId,
        remark: values.remark || '管理员手动确认收货',
      });
      messageApi.success('确认收货成功');
      setConfirmReceiptModalOpen(false);
      await fetchOrderDetail(); // 刷新订单详情
      onSuccess?.(); // 刷新列表
    } catch (error) {
      console.error('确认收货失败:', error);
      messageApi.error('确认收货失败');
    } finally {
      setConfirmReceiptLoading(false);
    }
  };

  const handleCancelSubmit = async () => {
    try {
      const values = await cancelForm.validateFields();
      setCancelLoading(true);
      await cancelOrder({
        orderId: orderId,
        cancelReason: values.cancelReason,
      });

      const isPaid = isOrderPaid(orderDetail?.orderInfo?.orderStatus);

      messageApi.success(isPaid ? '取消成功，已自动退款' : '取消成功');
      setCancelModalOpen(false);
      onClose();
      onSuccess?.(); // 刷新列表
    } catch (error) {
      console.error('取消订单失败:', error);
      if (error && typeof error === 'object' && 'errorFields' in error) {
        // 表单验证错误，不显示消息
        return;
      }
      messageApi.error('取消订单失败');
    } finally {
      setCancelLoading(false);
    }
  };

  const handleDelete = () => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除订单 ${orderDetail?.orderInfo?.orderNo} 吗？删除后无法恢复！`,
      okText: '确认删除',
      okType: 'danger',
      onOk: () => {
        messageApi.success('删除成功');
        onClose();
      },
    });
  };

  // 根据订单状态判断是否显示各个操作按钮
  const orderStatus = orderDetail?.orderInfo?.orderStatus;
  const showChangePriceBtn = canChangePrice(orderStatus);
  const showRefundBtn = canRefund(orderStatus);
  const showConfirmReceiptBtn = canConfirmReceipt(orderStatus);
  const showCancelBtn = canCancelOrder(orderStatus);

  const menuItems: MenuProps['items'] = [
    {
      key: 'addressChange',
      label: '修改地址',
      disabled: !canAccess(ADMIN_PERMISSIONS.mallOrder.edit),
      onClick: handleAddressChange,
    },
    {
      key: 'remarkChange',
      label: '修改备注',
      disabled: !canAccess(ADMIN_PERMISSIONS.mallOrder.edit),
      onClick: handleRemarkChange,
    },
    ...(showConfirmReceiptBtn
      ? [
          {
            type: 'divider' as const,
          },
          {
            key: 'confirmReceipt',
            label: '手动确认收货',
            disabled: !canAccess(ADMIN_PERMISSIONS.mallOrder.edit),
            onClick: handleConfirmReceipt,
          },
        ]
      : []),
    {
      type: 'divider',
    },
    {
      key: 'delete',
      label: '删除',
      danger: true,
      disabled: !canAccess(ADMIN_PERMISSIONS.mallOrder.delete),
      onClick: handleDelete,
    },
  ];

  return (
    <Drawer
      title={
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <span>订单详情</span>
          <Space>
            {showChangePriceBtn && (
              <PermissionButton
                size="small"
                access={ADMIN_PERMISSIONS.mallOrder.edit}
                onClick={handlePriceChange}
              >
                改价
              </PermissionButton>
            )}
            {showCancelBtn && (
              <PermissionButton
                size="small"
                danger
                access={ADMIN_PERMISSIONS.mallOrder.cancel}
                onClick={handleCancelOrder}
                loading={cancelLoading}
                disabled={cancelLoading || refundLoading}
              >
                取消订单
              </PermissionButton>
            )}
            {showRefundBtn && (
              <PermissionButton
                size="small"
                type="primary"
                danger
                access={ADMIN_PERMISSIONS.mallOrder.refund}
                onClick={handleRefund}
                loading={refundLoading}
                disabled={refundLoading || cancelLoading}
              >
                退款
              </PermissionButton>
            )}
            <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
              <PermissionButton
                type="text"
                access={[ADMIN_PERMISSIONS.mallOrder.edit, ADMIN_PERMISSIONS.mallOrder.delete]}
                icon={<MoreOutlined />}
                style={{ border: 'none' }}
                onClick={(e) => e.preventDefault()}
                disabled={refundLoading || cancelLoading || confirmReceiptLoading}
              />
            </Dropdown>
          </Space>
        </div>
      }
      placement="right"
      width={900}
      open={open}
      onClose={onClose}
    >
      {contextHolder}
      <Spin spinning={loading}>
        {orderDetail ? (
          <>
            {/* 订单基本信息 */}
            <div
              style={{
                marginBottom: 24,
                padding: '20px 0',
                borderBottom: '1px solid #f0f0f0',
              }}
            >
              <Row gutter={[24, 16]}>
                <Col span={8}>
                  <div
                    style={{
                      marginBottom: 12,
                      fontSize: 14,
                      display: 'flex',
                      flexDirection: 'column',
                    }}
                  >
                    <Text style={{ color: '#8c8c8c', marginBottom: 8 }}>订单编号</Text>
                    <Text
                      style={{
                        color: '#262626',
                        fontSize: 16,
                        fontWeight: 500,
                      }}
                    >
                      {orderDetail?.orderInfo?.orderNo || '-'}
                    </Text>
                  </div>
                </Col>
                <Col span={8}>
                  <div
                    style={{
                      marginBottom: 12,
                      fontSize: 14,
                      display: 'flex',
                      flexDirection: 'column',
                    }}
                  >
                    <Text style={{ color: '#8c8c8c', marginBottom: 8 }}>订单状态</Text>
                    <Text style={{ color: '#262626', fontSize: 14 }}>
                      {ORDER_STATUS_MAP[
                        orderDetail?.orderInfo?.orderStatus as keyof typeof ORDER_STATUS_MAP
                      ]?.text || '-'}
                    </Text>
                  </div>
                </Col>
                <Col span={8}>
                  <div
                    style={{
                      marginBottom: 12,
                      fontSize: 14,
                      display: 'flex',
                      flexDirection: 'column',
                    }}
                  >
                    <Text style={{ color: '#8c8c8c', marginBottom: 8 }}>订单金额</Text>
                    <Text
                      style={{
                        color: '#ff4d4f',
                        fontSize: 20,
                        fontWeight: 600,
                      }}
                    >
                      ¥
                      {orderDetail?.orderInfo?.totalAmount
                        ? Number(orderDetail.orderInfo.totalAmount).toFixed(2)
                        : '0.00'}
                    </Text>
                  </div>
                </Col>
              </Row>
            </div>

            <Tabs
              activeKey={activeTab}
              onChange={(key) => setActiveTab(key)}
              items={[
                {
                  key: 'order',
                  label: '订单信息',
                  children: <OrderInfo orderDetail={orderDetail} />,
                },
                {
                  key: 'products',
                  label: '商品信息',
                  children: <ProductInfo orderDetail={orderDetail} />,
                },
                {
                  key: 'records',
                  label: '订单流程',
                  children: <OrderTimeline orderId={orderId} visible={activeTab === 'records'} />,
                },
                {
                  key: 'delivery',
                  label: '发货记录',
                  children: (
                    <DeliveryRecords orderId={orderId} visible={activeTab === 'delivery'} />
                  ),
                },
              ]}
            />
          </>
        ) : (
          <Empty description="暂无订单详情信息" />
        )}
      </Spin>

      {/* 改价弹窗 */}
      <Modal
        title="订单改价"
        open={priceModalOpen}
        onOk={handlePriceSubmit}
        onCancel={() => setPriceModalOpen(false)}
        okText="确认"
        cancelText="取消"
        width={600}
      >
        {priceData && (
          <Descriptions column={1} bordered size="small" style={{ marginBottom: 16 }}>
            <Descriptions.Item label="订单编号">{priceData.orderNo}</Descriptions.Item>
            <Descriptions.Item label="当前订单总金额">
              <Text style={{ color: '#ff4d4f', fontWeight: 500 }}>
                ¥{Number(priceData.totalAmount || 0).toFixed(2)}
              </Text>
            </Descriptions.Item>
          </Descriptions>
        )}
        <Form form={priceForm} layout="vertical">
          <Form.Item
            label="修改订单总金额"
            name="totalAmount"
            rules={[
              { required: true, message: '请输入订单总金额' },
              {
                pattern: /^\d+(\.\d{1,2})?$/,
                message: '请输入有效的金额（最多两位小数）',
              },
            ]}
          >
            <Input
              autoComplete={TEXT_INPUT_AUTOCOMPLETE}
              prefix="¥"
              placeholder="请输入订单总金额"
              type="number"
              step="0.01"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 修改地址弹窗 */}
      <Modal
        title="修改收货地址"
        open={addressModalOpen}
        onOk={handleAddressSubmit}
        onCancel={() => setAddressModalOpen(false)}
        okText="确认"
        cancelText="取消"
        width={700}
      >
        {addressData && (
          <Descriptions column={2} bordered size="small" style={{ marginBottom: 16 }}>
            <Descriptions.Item label="订单编号" span={2}>
              {addressData.orderNo}
            </Descriptions.Item>
            <Descriptions.Item label="订单状态" span={2}>
              {ORDER_STATUS_MAP[addressData.orderStatus as keyof typeof ORDER_STATUS_MAP]?.text ||
                addressData.orderStatus}
            </Descriptions.Item>
          </Descriptions>
        )}
        <Form form={addressForm} layout="vertical">
          <Form.Item
            label="收货人"
            name="receiverName"
            rules={[{ required: true, message: '请输入收货人姓名' }]}
          >
            <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入收货人姓名" />
          </Form.Item>
          <Form.Item
            label="收货电话"
            name="receiverPhone"
            rules={[
              { required: true, message: '请输入收货电话' },
              {
                pattern: /^1[3-9]\d{9}$/,
                message: '请输入有效的手机号码',
              },
            ]}
          >
            <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入收货电话" />
          </Form.Item>
          <Form.Item
            label="收货地址"
            name="receiverAddress"
            rules={[{ required: true, message: '请输入收货地址' }]}
          >
            <Input.TextArea
              autoComplete={TEXT_INPUT_AUTOCOMPLETE}
              placeholder="请输入收货地址"
              rows={3}
              maxLength={200}
              showCount
            />
          </Form.Item>
          <Form.Item
            label="配送方式"
            name="deliveryType"
            rules={[{ required: true, message: '请选择配送方式' }]}
          >
            <Select placeholder="请选择配送方式" options={DELIVERY_TYPE_OPTIONS} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 修改备注弹窗 */}
      <Modal
        title="修改订单备注"
        open={remarkModalOpen}
        onOk={handleRemarkSubmit}
        onCancel={() => setRemarkModalOpen(false)}
        okText="确认"
        cancelText="取消"
        width={700}
      >
        {remarkData && (
          <Descriptions column={1} bordered size="small" style={{ marginBottom: 16 }}>
            <Descriptions.Item label="订单编号">{remarkData.orderNo}</Descriptions.Item>
            {remarkData.note && (
              <Descriptions.Item label="用户留言">
                <Text type="secondary">{remarkData.note}</Text>
              </Descriptions.Item>
            )}
          </Descriptions>
        )}
        <Form form={remarkForm} layout="vertical">
          <Form.Item
            label="订单备注（管理员备注）"
            name="remark"
            rules={[{ required: true, message: '请输入订单备注' }]}
          >
            <Input.TextArea
              autoComplete={TEXT_INPUT_AUTOCOMPLETE}
              placeholder="请输入订单备注"
              rows={4}
              maxLength={500}
              showCount
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 取消订单弹窗 */}
      <Modal
        title="取消订单"
        open={cancelModalOpen}
        onOk={handleCancelSubmit}
        onCancel={() => setCancelModalOpen(false)}
        okText="确认取消"
        cancelText="取消"
        okType="danger"
        confirmLoading={cancelLoading}
        width={600}
      >
        {orderDetail && (
          <>
            <Descriptions column={1} bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="订单编号">
                {orderDetail.orderInfo?.orderNo}
              </Descriptions.Item>
              <Descriptions.Item label="订单状态">
                {ORDER_STATUS_MAP[
                  orderDetail.orderInfo?.orderStatus as keyof typeof ORDER_STATUS_MAP
                ]?.text || orderDetail.orderInfo?.orderStatus}
              </Descriptions.Item>
            </Descriptions>
            {isOrderPaid(orderDetail.orderInfo?.orderStatus) && (
              <div
                style={{
                  padding: '12px',
                  background: '#fff7e6',
                  border: '1px solid #ffd591',
                  borderRadius: 4,
                  marginBottom: 16,
                }}
              >
                <Text style={{ color: '#fa8c16' }}>⚠️ 该订单用户已支付，取消订单将自动退款</Text>
              </div>
            )}
          </>
        )}
        <Form form={cancelForm} layout="vertical">
          <Form.Item
            label="取消原因"
            name="cancelReason"
            rules={[
              { required: true, message: '请输入取消原因' },
              { min: 5, message: '取消原因至少5个字符' },
            ]}
          >
            <Input.TextArea
              autoComplete={TEXT_INPUT_AUTOCOMPLETE}
              placeholder="请输入取消订单的原因"
              rows={4}
              maxLength={200}
              showCount
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 手动确认收货弹窗 */}
      <Modal
        title="手动确认收货"
        open={confirmReceiptModalOpen}
        onOk={handleConfirmReceiptSubmit}
        onCancel={() => setConfirmReceiptModalOpen(false)}
        okText="确认收货"
        cancelText="取消"
        confirmLoading={confirmReceiptLoading}
        width={600}
      >
        {orderDetail && (
          <Descriptions column={1} bordered size="small" style={{ marginBottom: 16 }}>
            <Descriptions.Item label="订单编号">{orderDetail.orderInfo?.orderNo}</Descriptions.Item>
            <Descriptions.Item label="订单状态">
              {ORDER_STATUS_MAP[orderDetail.orderInfo?.orderStatus as keyof typeof ORDER_STATUS_MAP]
                ?.text || orderDetail.orderInfo?.orderStatus}
            </Descriptions.Item>
          </Descriptions>
        )}
        <Form form={confirmReceiptForm} layout="vertical">
          <Form.Item
            label="确认收货备注（可选）"
            name="remark"
            rules={[{ max: 200, message: '备注不能超过200个字符' }]}
          >
            <Input.TextArea
              autoComplete={TEXT_INPUT_AUTOCOMPLETE}
              placeholder="请输入确认收货备注（可选）"
              rows={3}
              maxLength={200}
              showCount
            />
          </Form.Item>
        </Form>
      </Modal>
    </Drawer>
  );
};

export default OrderDetail;
