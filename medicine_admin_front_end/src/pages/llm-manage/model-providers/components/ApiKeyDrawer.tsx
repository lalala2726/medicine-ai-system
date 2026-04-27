import { PASSWORD_INPUT_AUTOCOMPLETE, TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { KeyOutlined } from '@ant-design/icons';
import { Button, Drawer, Form, Input, message, Space, Typography } from 'antd';
import React, { useEffect, useState } from 'react';

import { updateProviderApiKey } from '@/api/llm-manage/modelProviders';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';

const { Paragraph, Text } = Typography;

interface ApiKeyDrawerProps {
  open: boolean;
  providerId?: string;
  providerName?: string;
  onClose: () => void;
  onSuccess: () => void;
}

const ApiKeyDrawer: React.FC<ApiKeyDrawerProps> = ({
  open,
  providerId,
  providerName,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (open) {
      form.resetFields();
    }
  }, [open, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (!providerId) return;

      setLoading(true);
      await updateProviderApiKey({
        id: Number(providerId),
        apiKey: values.apiKey,
      });

      message.success('API Key 修改成功');
      onSuccess();
    } catch (error) {
      message.error('API Key 修改失败，请重试');
      // 验证失败或接口报错
      setLoading(false);
    }
  };

  return (
    <Drawer
      title={
        <Space>
          <KeyOutlined />
          <span>修改 API Key</span>
        </Space>
      }
      placement="right"
      onClose={onClose}
      open={open}
      width={480}
      destroyOnClose
      footer={
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <Button onClick={onClose} disabled={loading}>
            取消
          </Button>
          <PermissionButton
            type="primary"
            access={ADMIN_PERMISSIONS.llmProvider.update}
            onClick={handleSubmit}
            loading={loading}
          >
            确定保存
          </PermissionButton>
        </div>
      }
    >
      <Paragraph type="secondary" style={{ marginBottom: 24 }}>
        正在为提供商 <Text strong>{providerName}</Text> 修改鉴权密钥。
        出于安全考虑，原始密钥已被隐藏，请直接输入新的 API Key 进行覆盖。
      </Paragraph>

      <Form form={form} layout="vertical" autoComplete={TEXT_INPUT_AUTOCOMPLETE}>
        <Form.Item
          label="新的 API Key"
          name="apiKey"
          rules={[{ required: true, message: '请输入新的 API Key' }]}
        >
          <Input.Password
            placeholder="请输入 API 密钥"
            variant="filled"
            autoComplete={PASSWORD_INPUT_AUTOCOMPLETE}
            data-1p-ignore="true"
            data-lpignore="true"
            data-form-type="other"
            role="presentation"
            readOnly
            onFocus={(e) => {
              e.target.removeAttribute('readonly');
            }}
          />
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default ApiKeyDrawer;
