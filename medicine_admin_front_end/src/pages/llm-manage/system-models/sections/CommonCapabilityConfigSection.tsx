import { Form, Spin, Typography, message } from 'antd';
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
  getCommonCapabilityConfig,
  getVisionModelOptions,
  saveCommonCapabilityConfig,
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
 * 通用能力模型槽位键。
 */
type CommonCapabilitySlotKey =
  | 'imageRecognitionModel'
  | 'chatHistorySummaryModel'
  | 'chatTitleModel';

/**
 * 通用能力槽位元数据。
 */
interface CommonCapabilitySlotMeta {
  /** 槽位键。 */
  key: CommonCapabilitySlotKey;
  /** 槽位标题。 */
  title: string;
  /** 槽位描述。 */
  description: string;
  /** 空状态提示。 */
  emptyHint: string;
  /** 测试标识。 */
  testId: string;
  /** 选项类型。 */
  optionsType: 'vision' | 'chat';
}

/**
 * 通用能力表单值。
 */
type CommonCapabilityFormValues = Partial<Record<CommonCapabilitySlotKey, SlotValue>>;

/**
 * 通用能力无效选择提示。
 */
type InvalidHintState = Partial<Record<CommonCapabilitySlotKey, string>>;

/** 通用能力槽位元数据列表。 */
const COMMON_CAPABILITY_SLOTS: readonly CommonCapabilitySlotMeta[] = [
  {
    key: 'imageRecognitionModel',
    title: '图片识别模型',
    description: '用于图片理解、图片预解析与多模态识别场景。',
    emptyHint: '当前暂无图片理解模型',
    testId: 'common-capability-image-recognition-model',
    optionsType: 'vision',
  },
  {
    key: 'chatHistorySummaryModel',
    title: '聊天历史总结模型',
    description: '用于长会话内容压缩与历史摘要生成。',
    emptyHint: '当前暂无聊天模型',
    testId: 'common-capability-chat-history-summary-model',
    optionsType: 'chat',
  },
  {
    key: 'chatTitleModel',
    title: '聊天标题生成模型',
    description: '用于根据会话首轮上下文生成简洁标题。',
    emptyHint: '当前暂无聊天模型',
    testId: 'common-capability-chat-title-model',
    optionsType: 'chat',
  },
];

/**
 * 合并视觉模型选项，确保当前已保存但暂不可选的模型仍可被展示。
 *
 * @param options 当前可选视觉模型列表。
 * @param currentSelection 当前详情中的视觉模型选择。
 * @returns 合并后的视觉模型选项列表。
 */
function mergeVisionOptions(
  options: SystemModelTypes.ModelOption[],
  currentSelection?: SystemModelTypes.ModelSelection | null,
): SystemModelTypes.ModelOption[] {
  if (
    !currentSelection?.modelName ||
    options.some((item) => item.value === currentSelection.modelName)
  ) {
    return options;
  }

  return [
    {
      label: currentSelection.modelName,
      value: currentSelection.modelName,
      supportReasoning: currentSelection.supportReasoning,
      supportVision: currentSelection.supportVision,
    },
    ...options,
  ];
}

/**
 * 根据表单值构造保存请求体。
 *
 * @param values 当前表单值。
 * @returns 通用能力保存请求体。
 */
function buildSavePayload(
  values: CommonCapabilityFormValues,
): SystemModelTypes.CommonCapabilitySystemModelConfig {
  return {
    imageRecognitionModel: {
      modelName: values.imageRecognitionModel?.modelName,
      reasoningEnabled: Boolean(values.imageRecognitionModel?.reasoningEnabled),
    },
    chatHistorySummaryModel: {
      modelName: values.chatHistorySummaryModel?.modelName,
      reasoningEnabled: Boolean(values.chatHistorySummaryModel?.reasoningEnabled),
    },
    chatTitleModel: {
      modelName: values.chatTitleModel?.modelName,
      reasoningEnabled: Boolean(values.chatTitleModel?.reasoningEnabled),
    },
  };
}

/**
 * 通用能力配置区块。
 *
 * @returns 通用能力配置区块节点。
 */
const CommonCapabilityConfigSection = forwardRef<ConfigSectionRef>((props, ref) => {
  const [form] = Form.useForm<CommonCapabilityFormValues>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [detail, setDetail] = useState<SystemModelTypes.CommonCapabilitySystemModelConfig>({});
  const [visionOptions, setVisionOptions] = useState<SystemModelTypes.ModelOption[]>([]);
  const [chatOptions, setChatOptions] = useState<SystemModelTypes.ModelOption[]>([]);
  const [invalidHints, setInvalidHints] = useState<InvalidHintState>({});

  /**
   * 加载通用能力配置与模型选项。
   *
   * @returns 无返回值。
   */
  const loadSection = useCallback(async () => {
    setLoading(true);

    try {
      const [nextDetail, nextVisionOptions, nextChatOptions] = await Promise.all([
        getCommonCapabilityConfig(),
        getVisionModelOptions(),
        getChatModelOptions(),
      ]);
      const resolvedDetail = nextDetail || {};
      const resolvedVisionOptions = nextVisionOptions || [];
      const resolvedChatOptions = nextChatOptions || [];
      const nextInvalidHints: InvalidHintState = {};
      const nextFormValues: CommonCapabilityFormValues = {
        imageRecognitionModel: createSlotValue(resolvedDetail?.imageRecognitionModel),
      };

      (['chatHistorySummaryModel', 'chatTitleModel'] as const).forEach((key) => {
        const sanitizedSelection = sanitizeChatModelSelection(
          resolvedDetail?.[key],
          resolvedChatOptions,
        );
        nextInvalidHints[key] = sanitizedSelection.invalidSelectionHint;
        nextFormValues[key] = sanitizedSelection.value;
      });

      setDetail(resolvedDetail);
      setVisionOptions(resolvedVisionOptions);
      setChatOptions(resolvedChatOptions);
      setInvalidHints(nextInvalidHints);
      form.setFieldsValue(nextFormValues);
    } catch (error) {
      message.error(getSystemModelsErrorMessage(error, '通用能力配置加载失败'));
    } finally {
      setLoading(false);
    }
  }, [form]);

  useEffect(() => {
    void loadSection();
  }, [loadSection]);

  /**
   * 当前图片识别槽位可展示的视觉模型选项。
   */
  const displayVisionOptions = useMemo(
    () => mergeVisionOptions(visionOptions, detail?.imageRecognitionModel),
    [detail?.imageRecognitionModel, visionOptions],
  );

  /**
   * 获取指定槽位当前应展示的模型选项集合。
   *
   * @param slot 当前槽位元数据。
   * @returns 槽位对应的模型选项列表。
   */
  const getSlotOptions = useCallback(
    (slot: CommonCapabilitySlotMeta): SystemModelTypes.ModelOption[] =>
      slot.optionsType === 'vision' ? displayVisionOptions : chatOptions,
    [chatOptions, displayVisionOptions],
  );

  /**
   * 保存通用能力配置。
   *
   * @returns 无返回值。
   */
  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();

      setSaving(true);
      await saveCommonCapabilityConfig(buildSavePayload(values));
    } catch (error) {
      if ((error as { errorFields?: unknown[] } | null)?.errorFields) {
        throw error;
      }
      message.error(getSystemModelsErrorMessage(error, '通用能力配置保存失败'));
      throw error;
    } finally {
      setSaving(false);
    }
  }, [form]);

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
          <div className={styles.sectionCardTitle}>基础模型</div>
          <Typography.Text type="secondary" className={styles.sectionCardDescription}>
            配置图片理解、历史摘要和会话标题生成使用的模型。
          </Typography.Text>
        </div>
      </div>

      <Spin spinning={loading || saving}>
        <Form form={form} layout="vertical">
          <div className={styles.drawerBody}>
            <div className={styles.formCluster}>
              <div className={styles.slotStack}>
                {COMMON_CAPABILITY_SLOTS.map((slot) => (
                  <ModelSlotEditor
                    key={slot.key}
                    title={slot.title}
                    description={slot.description}
                    name={slot.key}
                    options={getSlotOptions(slot)}
                    invalidSelectionHint={invalidHints[slot.key]}
                    optionsLoading={loading}
                    emptyHint={slot.emptyHint}
                    testId={slot.testId}
                  />
                ))}
              </div>
            </div>
          </div>
        </Form>
      </Spin>
    </div>
  );
});

export default CommonCapabilityConfigSection;
