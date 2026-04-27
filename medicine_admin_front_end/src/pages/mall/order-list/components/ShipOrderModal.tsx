import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { Form, Input, Modal, message } from 'antd';
import React, { useState } from 'react';
import { type MallOrderTypes, shipOrder } from '@/api/mall/order';

interface ShipOrderModalProps {
  open: boolean;
  onClose: () => void;
  orderId: string;
  orderNo?: string;
  onSuccess?: () => void;
}

const ShipOrderModal: React.FC<ShipOrderModalProps> = ({
  open,
  onClose,
  orderId,
  orderNo,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const data: MallOrderTypes.OrderShipRequest = {
        orderId,
        logisticsCompany: values.logisticsCompany,
        trackingNumber: values.trackingNumber,
        shipmentNote: values.shipmentNote,
      };

      await shipOrder(data);
      message.success('发货成功');
      form.resetFields();
      onClose();
      onSuccess?.();
    } catch (error) {
      console.error('发货失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      title="订单发货"
      open={open}
      onOk={handleSubmit}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={600}
      destroyOnHidden
    >
      <div style={{ marginBottom: 16 }}>
        <div style={{ color: '#8c8c8c', fontSize: 14 }}>
          订单编号：<span style={{ color: '#262626' }}>{orderNo || '-'}</span>
        </div>
      </div>

      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          label="物流公司"
          name="logisticsCompany"
          rules={[
            { required: true, message: '请输入物流公司' },
            { max: 50, message: '物流公司名称不能超过50个字符' },
          ]}
        >
          <Input
            autoComplete={TEXT_INPUT_AUTOCOMPLETE}
            placeholder="请输入物流公司名称，如：顺丰速运"
          />
        </Form.Item>

        <Form.Item
          label="物流单号"
          name="trackingNumber"
          rules={[
            { required: true, message: '请输入物流单号' },
            { max: 50, message: '物流单号不能超过50个字符' },
          ]}
        >
          <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="请输入物流单号" />
        </Form.Item>

        <Form.Item
          label="发货备注"
          name="shipmentNote"
          rules={[{ max: 200, message: '发货备注不能超过200个字符' }]}
        >
          <Input.TextArea
            autoComplete={TEXT_INPUT_AUTOCOMPLETE}
            placeholder="请输入发货备注（可选）"
            rows={3}
            maxLength={200}
            showCount
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ShipOrderModal;
