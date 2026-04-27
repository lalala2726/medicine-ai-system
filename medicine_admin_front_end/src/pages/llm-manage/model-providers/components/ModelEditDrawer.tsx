import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import {
  Button,
  Card,
  Col,
  Drawer,
  Form,
  Input,
  Row,
  Select,
  Switch,
  type FormInstance,
} from 'antd';
import React from 'react';

import { createEmptyModel, MODEL_TYPE_OPTIONS } from '../shared';
import styles from './model-edit-drawer.module.less';

const { TextArea } = Input;

interface ModelEditDrawerProps {
  open: boolean;
  editingIndex: number | null;
  form: FormInstance;
  onClose: () => void;
  onOk: () => void;
}

const ModelEditDrawer: React.FC<ModelEditDrawerProps> = ({
  open,
  editingIndex,
  form,
  onClose,
  onOk,
}) => {
  return (
    <Drawer
      title={editingIndex !== null ? '编辑模型' : '添加模型'}
      open={open}
      onClose={onClose}
      width={600}
      destroyOnClose
      rootClassName={styles.drawerForm}
      footer={
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" onClick={onOk}>
            确定
          </Button>
        </div>
      }
    >
      <Form form={form} layout="vertical" initialValues={createEmptyModel()}>
        <Row gutter={16}>
          <Col span={24}>
            <Form.Item
              label="模型实际名称"
              name="modelName"
              rules={[{ required: true, message: '请输入模型实际名称' }]}
              tooltip="模型调用时传递给厂商的真实 model 参数值，如 'gpt-4o' 或 'qwen-max'"
            >
              <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="例如: gpt-4" />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item
              label="模型类型"
              name="modelType"
              rules={[{ required: true }]}
              tooltip="该模型的主要用途类别，用于在不同场景下过滤可用的模型"
            >
              <Select options={MODEL_TYPE_OPTIONS} />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Row gutter={16}>
              <Col span={8}>
                <Card size="small" bordered className={styles.featureCard}>
                  <div className={styles.featureCardHeader}>
                    <span className={styles.featureTitle}>启用状态</span>
                    <Form.Item name="enabled" valuePropName="checked" initialValue={true} noStyle>
                      <Switch size="small" />
                    </Form.Item>
                  </div>
                  <div className={styles.featureDesc}>
                    关闭后，该模型将不会在系统的模型选择列表中出现。
                  </div>
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small" bordered className={styles.featureCard}>
                  <div className={styles.featureCardHeader}>
                    <span className={styles.featureTitle}>深度思考</span>
                    <Form.Item
                      name="supportReasoning"
                      valuePropName="checked"
                      getValueProps={(v) => ({ checked: v === 1 })}
                      normalize={(v) => (v ? 1 : 0)}
                      noStyle
                    >
                      <Switch size="small" />
                    </Form.Item>
                  </div>
                  <div className={styles.featureDesc}>
                    是否支持类似 OpenAI o1 或 DeepSeek r1 的能力。
                  </div>
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small" bordered className={styles.featureCard}>
                  <div className={styles.featureCardHeader}>
                    <span className={styles.featureTitle}>图片理解</span>
                    <Form.Item
                      name="supportVision"
                      valuePropName="checked"
                      getValueProps={(v) => ({ checked: v === 1 })}
                      normalize={(v) => (v ? 1 : 0)}
                      noStyle
                    >
                      <Switch size="small" />
                    </Form.Item>
                  </div>
                  <div className={styles.featureDesc}>是否具备视觉多模态能力，支持传入图片。</div>
                </Card>
              </Col>
            </Row>
          </Col>
          <Col span={24} style={{ marginTop: 24 }}>
            <Form.Item label="描述" name="description">
              <TextArea rows={2} placeholder="模型简单描述..." />
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Drawer>
  );
};

export default ModelEditDrawer;
