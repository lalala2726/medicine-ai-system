import { Form, Spin, Switch, Tag, Typography, message } from 'antd';
import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
  forwardRef,
  useImperativeHandle,
} from 'react';

import {
  getChatModelOptions,
  getClientAssistantConfig,
  saveClientAssistantConfig,
  type SystemModelTypes,
} from '@/api/llm-manage/systemModels';

import styles from '../index.module.less';
import { getSystemModelsErrorMessage, type ConfigSectionRef } from '../shared';
import ModelSlotEditor from '../components/ModelSlotEditor';
import {
  createSlotValue,
  sanitizeChatModelSelection,
  type SlotValue,
} from '../components/modelSelectionUtils';

/**
 * 客户端助手模型槽位键。
 */
type ClientAssistantSlotKey = 'routeModel' | 'serviceNodeModel' | 'diagnosisNodeModel';

/**
 * 客户端助手槽位元数据。
 */
interface ClientAssistantSlotMeta {
  /** 槽位键。 */
  key: ClientAssistantSlotKey;
  /** 槽位标题。 */
  title: string;
  /** 槽位描述。 */
  description: string;
  /** 测试标识。 */
  testId: string;
  /** 是否展示深度思考开关。 */
  showReasoningToggle: boolean;
}

/**
 * 客户端助手表单值。
 */
type ClientAssistantFormValues = Partial<Record<ClientAssistantSlotKey, SlotValue>> & {
  /** 是否允许客户端聊天开启统一深度思考。 */
  reasoningEnabled?: boolean;
};

/**
 * 客户端助手无效选择提示。
 */
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

/** 客户端助手全部槽位键列表。 */
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
 * 客户端助手配置区块。
 *
 * @returns 客户端助手配置区块节点。
 */
const ClientAssistantConfigSection = forwardRef<ConfigSectionRef>((props, ref) => {
  const [form] = Form.useForm<ClientAssistantFormValues>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
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
   * 加载客户端助手配置与聊天模型选项。
   *
   * @returns 无返回值。
   */
  const loadSection = useCallback(async () => {
    setLoading(true);

    try {
      const [nextDetail, nextChatOptions] = await Promise.all([
        getClientAssistantConfig(),
        getChatModelOptions(),
      ]);
      const resolvedDetail = nextDetail || {};
      const resolvedChatOptions = nextChatOptions || [];
      const nextInvalidHints: InvalidHintState = {};
      const nextFormValues = createFormValues(resolvedDetail);

      CLIENT_ASSISTANT_SLOT_KEYS.forEach((key) => {
        const sanitizedSelection = sanitizeChatModelSelection(
          resolvedDetail?.[key],
          resolvedChatOptions,
        );
        nextInvalidHints[key] = sanitizedSelection.invalidSelectionHint;
        nextFormValues[key] = sanitizedSelection.value;
      });

      setChatOptions(resolvedChatOptions);
      setInvalidHints(nextInvalidHints);
      form.setFieldsValue(nextFormValues);
    } catch (error) {
      message.error(getSystemModelsErrorMessage(error, '客户端助手配置加载失败'));
    } finally {
      setLoading(false);
    }
  }, [form]);

  useEffect(() => {
    void loadSection();
  }, [loadSection]);

  useEffect(() => {
    if (!reasoningSwitchAvailable && form.getFieldValue('reasoningEnabled')) {
      form.setFieldValue('reasoningEnabled', false);
    }
  }, [form, reasoningSwitchAvailable]);

  /**
   * 保存客户端助手配置。
   *
   * @returns 无返回值。
   */
  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();

      setSaving(true);
      await saveClientAssistantConfig(buildSavePayload(values));
      await loadSection();
    } catch (error) {
      if ((error as { errorFields?: unknown[] } | null)?.errorFields) {
        throw error;
      }
      message.error(getSystemModelsErrorMessage(error, '客户端助手配置保存失败'));
      throw error;
    } finally {
      setSaving(false);
    }
  }, [form, loadSection]);

  useImperativeHandle(ref, () => ({
    reload: async () => {
      await loadSection();
    },
    save: async () => {
      await handleSave();
    },
  }));

  return (
    <div className={styles.sectionCard}>
      <div className={styles.sectionCardHeader}>
        <div className={styles.sectionCardTitleWrap}>
          <div className={styles.sectionCardTitle}>客户端助手</div>
          <Typography.Text type="secondary" className={styles.sectionCardDescription}>
            配置客户端聊天链路里的路由、服务和问诊节点模型。
          </Typography.Text>
        </div>
      </div>

      <Spin spinning={loading || saving}>
        <Form form={form} layout="vertical">
          <div className={styles.drawerBody}>
            <div className={styles.formCluster}>
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
                    optionsLoading={loading}
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
            </div>
          </div>
        </Form>
      </Spin>
    </div>
  );
});

export default ClientAssistantConfigSection;
