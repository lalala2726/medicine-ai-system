import { PASSWORD_INPUT_AUTOCOMPLETE, TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { CloudServerOutlined } from '@ant-design/icons';
import { Card, Col, Form, Input, InputNumber, Row, Select, Typography } from 'antd';
import React from 'react';

import { ALIYUN_BAILIAN_BASE_URL, PROVIDER_TYPE_OPTIONS } from '../shared';
import styles from './provider-basic-form.module.less';

const { TextArea } = Input;
const { Text, Paragraph } = Typography;

interface ProviderBasicFormProps {
  isEdit: boolean;
  helperText: string;
}

const ProviderBasicForm: React.FC<ProviderBasicFormProps> = ({ isEdit, helperText }) => {
  return (
    <div className={styles.stepContent}>
      <Card
        bordered={false}
        title={
          <span className={styles.cardTitle}>
            <CloudServerOutlined /> <span>厂商基础配置</span>
          </span>
        }
      >
        <Paragraph className={styles.helper}>{helperText}</Paragraph>
        <Row gutter={24}>
          <Col xs={24} md={12}>
            <Form.Item
              label="提供商名称"
              name="providerName"
              rules={[{ required: true, message: '请输入提供商名称' }]}
              tooltip="自定义此提供商在系统中的显示名称，例如 '百联生产实例'"
            >
              <Input
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                placeholder="例如: 百联生产实例"
                maxLength={50}
                variant="filled"
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item
              label="提供商类型"
              name="providerType"
              rules={[{ required: true, message: '请选择提供商类型' }]}
              tooltip="当前仅支持阿里云百联类型，运行时将按该类型加载对应适配"
            >
              <Select
                placeholder="请选择提供商类型"
                options={PROVIDER_TYPE_OPTIONS}
                variant="filled"
                disabled
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item
              label="Base URL"
              name="baseUrl"
              rules={[{ required: true, message: '请输入 Base URL' }]}
              tooltip={`阿里云百联 OpenAI 兼容模式请求地址，例如 '${ALIYUN_BAILIAN_BASE_URL}'`}
            >
              <Input
                placeholder={`API 服务端点，例如: ${ALIYUN_BAILIAN_BASE_URL}`}
                variant="filled"
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
              />
            </Form.Item>
          </Col>
          {!isEdit && (
            <Col xs={24} md={12}>
              <Form.Item
                label="API Key"
                name="apiKey"
                rules={[{ required: true, message: '请输入 API Key' }]}
                tooltip="用于鉴权的 API 密钥。如果保持留空，则在保存时不会覆盖原有的配置"
              >
                <Input.Password
                  placeholder="请输入 API 密钥"
                  visibilityToggle
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
            </Col>
          )}
          <Col xs={24} md={12}>
            <Form.Item label="排序" name="sort" tooltip="提供商在列表中的展示顺序，数字越小越靠前">
              <InputNumber
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                min={0}
                style={{ width: '100%' }}
                variant="filled"
              />
            </Form.Item>
          </Col>
          <Col xs={24}>
            <Form.Item
              label="备注说明"
              name="description"
              tooltip="针对当前提供商的额外说明或者记录用途"
            >
              <TextArea
                rows={4}
                maxLength={200}
                showCount
                placeholder="输入该提供商的相关备注..."
                variant="filled"
              />
            </Form.Item>
          </Col>
        </Row>
        <Text type="secondary" style={{ fontSize: '12px', marginTop: '16px', display: 'block' }}>
          * 注：当前仅支持阿里云百联接入（OpenAI 兼容模式），不再提供 OpenAI 或火山引擎新增入口。
        </Text>
      </Card>
    </div>
  );
};

export default ProviderBasicForm;
