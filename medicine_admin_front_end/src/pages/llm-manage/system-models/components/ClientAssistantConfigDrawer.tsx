import { InfoCircleOutlined } from '@ant-design/icons';
import { Button, Drawer, Form, Space, Spin, Switch, Tag, Typography, message } from 'antd';
import React, { useCallback, useEffect, useMemo, useState } from 'react';

import {
  getChatModelOptions,
  saveClientAssistantConfig,
  type SystemModelTypes,
} from '@/api/llm-manage/systemModels';
import { useThemeContext } from '@/contexts/ThemeContext';

import styles from '../index.module.less';
import ModelSlotEditor from './ModelSlotEditor';
import { createSlotValue, sanitizeChatModelSelection, type SlotValue } from './modelSelectionUtils';

interface ClientAssistantConfigDrawerProps {
  open: boolean;
  detail?: SystemModelTypes.ClientAssistantSystemModelConfig;
  onClose: () => void;
  onSaved: () => Promise<void> | void;
}

type ClientAssistantSlotKey = 'routeModel' | 'serviceNodeModel' | 'diagnosisNodeModel';

interface ClientAssistantSlotMeta {
  key: ClientAssistantSlotKey;
  title: string;
  description: string;
  testId: string;
  showReasoningToggle: boolean;
}

type ClientAssistantFormValues = Partial<Record<ClientAssistantSlotKey, SlotValue>> & {
  /** 是否允许客户端聊天开启统一深度思考。 */
  reasoningEnabled?: boolean;
};
type InvalidHintState = Partial<Record<ClientAssistantSlotKey, string>>;

/**
 * 客户端统一深度思考未满足开启条件时的提示文案。
 */
const CLIENT_ASSISTANT_REASONING_DISABLED_HINT =
  '仅当服务节点模型和诊断节点模型都支持深度思考时，才可开启客户端统一深度思考。';

/**
 * 客户端统一深度思考开启后的效果提示文案。
 */
const CLIENT_ASSISTANT_REASONING_ENABLED_HINT =
  '开启后，客户端聊天输入框会显示“深度思考”开关，当前轮开启后会同时作用于服务节点和诊断节点。';

/** 客户端助手全部模型槽位键列表。 */
const CLIENT_ASSISTANT_SLOT_KEYS: readonly ClientAssistantSlotKey[] = [
  'routeModel',
  'serviceNodeModel',
  'diagnosisNodeModel',
];

/** 客户端助手槽位定义列表。 */
const CLIENT_ASSISTANT_SLOTS: readonly ClientAssistantSlotMeta[] = [
  {
    key: 'routeModel',
    title: '路由模型',
    description: '用于客户端入口意图判断和节点路由。',
    testId: 'client-assistant-route-model',
    showReasoningToggle: false,
  },
  {
    key: 'serviceNodeModel',
    title: '服务节点模型',
    description: '用于订单、商品、售后、通用对话与非问诊咨询处理。',
    testId: 'client-assistant-service-model',
    showReasoningToggle: false,
  },
  {
    key: 'diagnosisNodeModel',
    title: '诊断节点模型',
    description: '用于问诊链路的症状分析与建议输出。',
    testId: 'client-assistant-diagnosis-model',
    showReasoningToggle: false,
  },
];

/**
 * 根据当前详情生成表单初始值。
 *
 * @param detail 当前详情数据。
 * @returns 表单可直接回填的模型槽位值。
 */
function createFormValues(
  detail?: SystemModelTypes.ClientAssistantSystemModelConfig,
): ClientAssistantFormValues {
  const nextFormValues = CLIENT_ASSISTANT_SLOT_KEYS.reduce<ClientAssistantFormValues>(
    (prev, key) => {
      prev[key] = createSlotValue(detail?.[key]);
      return prev;
    },
    {},
  );
  nextFormValues.reasoningEnabled = detail?.reasoningEnabled ?? false;
  return nextFormValues;
}

/**
 * 根据表单值构造保存请求体。
 *
 * @param values 表单值。
 * @returns 客户端助手保存请求。
 */
function buildSavePayload(
  values: ClientAssistantFormValues,
): SystemModelTypes.ClientAssistantSystemModelConfig {
  return {
    routeModel: {
      modelName: values.routeModel?.modelName,
    },
    serviceNodeModel: {
      modelName: values.serviceNodeModel?.modelName,
    },
    diagnosisNodeModel: {
      modelName: values.diagnosisNodeModel?.modelName,
    },
    reasoningEnabled: Boolean(values.reasoningEnabled),
  };
}

/**
 * 统一提取错误文案。
 *
 * @param error 原始异常对象。
 * @param fallback 默认错误提示。
 * @returns 可展示给用户的错误文案。
 */
function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

/**
 * 客户端助手系统模型配置抽屉。
 */
const ClientAssistantConfigDrawer: React.FC<ClientAssistantConfigDrawerProps> = ({
  open,
  detail,
  onClose,
  onSaved,
}) => {
  const { isDark } = useThemeContext();
  const [form] = Form.useForm<ClientAssistantFormValues>();
  const [saving, setSaving] = useState(false);
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [chatOptions, setChatOptions] = useState<SystemModelTypes.ModelOption[]>([]);
  const [invalidHints, setInvalidHints] = useState<InvalidHintState>({});
  const serviceNodeModelName = Form.useWatch(['serviceNodeModel', 'modelName'], form) as
    | string
    | undefined;
  const diagnosisNodeModelName = Form.useWatch(['diagnosisNodeModel', 'modelName'], form) as
    | string
    | undefined;

  /** 当前选中的服务节点模型选项。 */
  const selectedServiceNodeOption = useMemo(
    () => chatOptions.find((item) => item.value === serviceNodeModelName),
    [chatOptions, serviceNodeModelName],
  );

  /** 当前选中的诊断节点模型选项。 */
  const selectedDiagnosisNodeOption = useMemo(
    () => chatOptions.find((item) => item.value === diagnosisNodeModelName),
    [chatOptions, diagnosisNodeModelName],
  );

  /** 当前是否满足统一深度思考开关的开启条件。 */
  const reasoningSwitchAvailable = useMemo(
    () =>
      Boolean(selectedServiceNodeOption?.supportReasoning) &&
      Boolean(selectedDiagnosisNodeOption?.supportReasoning),
    [selectedDiagnosisNodeOption?.supportReasoning, selectedServiceNodeOption?.supportReasoning],
  );

  /** 当前统一深度思考说明文案。 */
  const reasoningHintText = reasoningSwitchAvailable
    ? CLIENT_ASSISTANT_REASONING_ENABLED_HINT
    : CLIENT_ASSISTANT_REASONING_DISABLED_HINT;

  /**
   * 加载聊天模型选项，并对当前不可用的旧选择做提示。
   */
  const loadOptions = useCallback(async () => {
    setOptionsLoading(true);
    try {
      const nextChatOptions = (await getChatModelOptions()) || [];
      const nextInvalidHints: InvalidHintState = {};
      const nextFormValues = createFormValues(detail);

      CLIENT_ASSISTANT_SLOT_KEYS.forEach((key) => {
        const sanitizedSelection = sanitizeChatModelSelection(detail?.[key], nextChatOptions);
        nextInvalidHints[key] = sanitizedSelection.invalidSelectionHint;
        nextFormValues[key] = sanitizedSelection.value;
      });

      setChatOptions(nextChatOptions);
      setInvalidHints(nextInvalidHints);
      form.setFieldsValue(nextFormValues);
    } finally {
      setOptionsLoading(false);
    }
  }, [detail, form]);

  useEffect(() => {
    if (!open) {
      form.resetFields();
      setInvalidHints({});
      return;
    }

    form.setFieldsValue(createFormValues(detail));
    void loadOptions();
  }, [detail, form, loadOptions, open]);

  useEffect(() => {
    if (!reasoningSwitchAvailable && form.getFieldValue('reasoningEnabled')) {
      form.setFieldValue('reasoningEnabled', false);
    }
  }, [form, reasoningSwitchAvailable]);

  /**
   * 保存客户端助手配置。
   */
  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();

      setSaving(true);
      await saveClientAssistantConfig(buildSavePayload(values));
      await loadOptions();
      message.success('客户端助手配置已保存');
      await onSaved();
      onClose();
    } catch (error) {
      if ((error as { errorFields?: unknown[] } | null)?.errorFields) {
        return;
      }
      message.error(getErrorMessage(error, '客户端助手配置保存失败'));
    } finally {
      setSaving(false);
    }
  }, [form, loadOptions, onClose, onSaved]);

  return (
    <Drawer
      title="配置客户端助手"
      width={920}
      open={open}
      destroyOnClose
      onClose={onClose}
      rootClassName={isDark ? 'app-pro-layout--dark' : ''}
      footer={
        <div className={styles.drawerFooter}>
          <Space>
            <Button onClick={onClose}>取消</Button>
            <Button type="primary" loading={saving} onClick={handleSave}>
              保存配置
            </Button>
          </Space>
        </div>
      }
    >
      <Spin spinning={optionsLoading && chatOptions.length === 0}>
        <Form form={form} layout="vertical">
          <div className={styles.slotHeader}>
            <div>
              <div className={styles.slotTitle}>客户端助手节点模型编排</div>
              <Typography.Text type="secondary" className={styles.slotDescription}>
                为客户端的不同执行节点分配最适合的模型。路由和服务节点建议选择逻辑能力强的模型。
              </Typography.Text>
            </div>
          </div>

          <div className={styles.slotGrid}>
            {CLIENT_ASSISTANT_SLOTS.map((slot) => (
              <ModelSlotEditor
                key={slot.key}
                title={slot.title}
                description={slot.description}
                name={slot.key}
                options={chatOptions}
                invalidSelectionHint={invalidHints[slot.key]}
                optionsLoading={optionsLoading}
                emptyHint="当前暂无聊天模型"
                showReasoningToggle={slot.showReasoningToggle}
                testId={slot.testId}
              />
            ))}
          </div>

          <div className={styles.slotCard}>
            <div className={styles.slotHeader}>
              <div>
                <div className={styles.slotTitle}>统一深度思考支持</div>
                <Typography.Text type="secondary" className={styles.slotDescription}>
                  统一控制客户端聊天输入框是否允许开启深度思考。
                </Typography.Text>
              </div>
            </div>

            <div className={styles.capabilityRow}>
              {reasoningSwitchAvailable ? (
                <Tag color="geekblue" bordered={false}>
                  已满足开启条件
                </Tag>
              ) : (
                <Tag bordered={false}>待满足开启条件</Tag>
              )}
            </div>

            <Form.Item
              label="允许客户端聊天开启深度思考"
              name="reasoningEnabled"
              valuePropName="checked"
              className={styles.compactFormItem}
            >
              <Switch
                checkedChildren="开启"
                unCheckedChildren="关闭"
                disabled={!reasoningSwitchAvailable}
              />
            </Form.Item>

            <Typography.Text type="secondary" className={styles.inlineInfoHint}>
              {reasoningHintText}
            </Typography.Text>
          </div>

          <div className={styles.notesSection}>
            <InfoCircleOutlined />
            <div className={styles.notesContent}>
              <div>
                <strong>配置说明：</strong>
              </div>
              <div>
                1. <strong>路由模型</strong>：负责识别用户问题属于闲聊、业务处理还是问诊链路。
              </div>
              <div>
                2. <strong>服务节点模型</strong>：处理订单、商品、售后、通用对话与所有非问诊咨询。
              </div>
              <div>
                3. <strong>诊断节点模型</strong>
                ：专门用于问诊链路中的症状分析、问答追问与诊断建议生成。
              </div>
              <div>
                4. <strong>统一深度思考支持</strong>
                ：仅当服务节点模型和诊断节点模型都支持深度思考时才可开启；开启后，客户端聊天输入框会展示“深度思考”开关。
              </div>
            </div>
          </div>
        </Form>
      </Spin>
    </Drawer>
  );
};

export default ClientAssistantConfigDrawer;
