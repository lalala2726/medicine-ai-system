import { InfoCircleOutlined } from '@ant-design/icons';
import { Button, Drawer, Form, Space, Spin, Typography, message } from 'antd';
import React, { useCallback, useEffect, useMemo, useState } from 'react';

import {
  getChatModelOptions,
  getVisionModelOptions,
  saveCommonCapabilityConfig,
  type SystemModelTypes,
} from '@/api/llm-manage/systemModels';
import { useThemeContext } from '@/contexts/ThemeContext';

import styles from '../index.module.less';
import ModelSlotEditor from './ModelSlotEditor';
import { createSlotValue, sanitizeChatModelSelection, type SlotValue } from './modelSelectionUtils';

interface CommonCapabilityConfigDrawerProps {
  open: boolean;
  detail?: SystemModelTypes.CommonCapabilitySystemModelConfig;
  onClose: () => void;
  onSaved: () => Promise<void> | void;
}

type CommonCapabilitySlotKey =
  | 'imageRecognitionModel'
  | 'chatHistorySummaryModel'
  | 'chatTitleModel';

interface CommonCapabilitySlotMeta {
  key: CommonCapabilitySlotKey;
  title: string;
  description: string;
  emptyHint: string;
  testId: string;
  optionsType: 'vision' | 'chat';
}

type CommonCapabilityFormValues = Partial<Record<CommonCapabilitySlotKey, SlotValue>>;
type InvalidHintState = Partial<Record<CommonCapabilitySlotKey, string>>;

/** 通用能力全部槽位键列表。 */
const COMMON_CAPABILITY_SLOT_KEYS: readonly CommonCapabilitySlotKey[] = [
  'imageRecognitionModel',
  'chatHistorySummaryModel',
  'chatTitleModel',
];

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
 * 根据当前详情生成表单初始值。
 *
 * @param detail 当前通用能力详情。
 * @returns 可直接回填到表单的槽位值。
 */
function createFormValues(
  detail?: SystemModelTypes.CommonCapabilitySystemModelConfig,
): CommonCapabilityFormValues {
  return COMMON_CAPABILITY_SLOT_KEYS.reduce<CommonCapabilityFormValues>((prev, key) => {
    prev[key] = createSlotValue(detail?.[key]);
    return prev;
  }, {});
}

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
) {
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
 * 通用能力系统模型配置抽屉。
 */
const CommonCapabilityConfigDrawer: React.FC<CommonCapabilityConfigDrawerProps> = ({
  open,
  detail,
  onClose,
  onSaved,
}) => {
  const { isDark } = useThemeContext();
  const [form] = Form.useForm<CommonCapabilityFormValues>();
  const [saving, setSaving] = useState(false);
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [visionOptions, setVisionOptions] = useState<SystemModelTypes.ModelOption[]>([]);
  const [chatOptions, setChatOptions] = useState<SystemModelTypes.ModelOption[]>([]);
  const [invalidHints, setInvalidHints] = useState<InvalidHintState>({});

  /**
   * 加载视觉模型与聊天模型选项，并处理当前已失效的聊天模型选择。
   */
  const loadOptions = useCallback(async () => {
    setOptionsLoading(true);
    try {
      const [nextVisionOptions, nextChatOptions] = await Promise.all([
        getVisionModelOptions(),
        getChatModelOptions(),
      ]);
      const resolvedVisionOptions = nextVisionOptions || [];
      const resolvedChatOptions = nextChatOptions || [];
      const nextInvalidHints: InvalidHintState = {};
      const nextFormValues: CommonCapabilityFormValues = {
        imageRecognitionModel: createSlotValue(detail?.imageRecognitionModel),
      };

      (['chatHistorySummaryModel', 'chatTitleModel'] as const).forEach((key) => {
        const sanitizedSelection = sanitizeChatModelSelection(detail?.[key], resolvedChatOptions);
        nextInvalidHints[key] = sanitizedSelection.invalidSelectionHint;
        nextFormValues[key] = sanitizedSelection.value;
      });

      setVisionOptions(resolvedVisionOptions);
      setChatOptions(resolvedChatOptions);
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

  /**
   * 保存通用能力配置。
   */
  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();

      setSaving(true);
      await saveCommonCapabilityConfig(buildSavePayload(values));
      message.success('通用能力配置已保存');
      await onSaved();
      onClose();
    } catch (error) {
      if ((error as { errorFields?: unknown[] } | null)?.errorFields) {
        return;
      }
      message.error(getErrorMessage(error, '通用能力配置保存失败'));
    } finally {
      setSaving(false);
    }
  }, [form, onClose, onSaved]);

  /**
   * 生成当前图片识别槽位可展示的视觉模型选项。
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
    (slot: CommonCapabilitySlotMeta) =>
      slot.optionsType === 'vision' ? displayVisionOptions : chatOptions,
    [chatOptions, displayVisionOptions],
  );

  return (
    <Drawer
      title="配置通用能力"
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
      <Spin spinning={optionsLoading && visionOptions.length === 0 && chatOptions.length === 0}>
        <Form form={form} layout="vertical">
          <div className={styles.slotHeader}>
            <div>
              <div className={styles.slotTitle}>通用能力模型配置组</div>
              <Typography.Text type="secondary" className={styles.slotDescription}>
                这里统一管理图片识别、聊天历史总结和聊天标题生成三项能力。它们共用一个配置组入口，但仍然是三个独立槽位，不会共用同一个模型。
              </Typography.Text>
            </div>
          </div>

          <div className={styles.slotStack}>
            {COMMON_CAPABILITY_SLOTS.map((slot) => (
              <ModelSlotEditor
                key={slot.key}
                title={slot.title}
                description={slot.description}
                name={slot.key}
                options={getSlotOptions(slot)}
                invalidSelectionHint={invalidHints[slot.key]}
                optionsLoading={optionsLoading}
                emptyHint={slot.emptyHint}
                testId={slot.testId}
              />
            ))}
          </div>

          <div className={styles.notesSection}>
            <InfoCircleOutlined />
            <div className={styles.notesContent}>
              <div>
                <strong>配置说明：</strong>
              </div>
              <div>
                1. <strong>图片识别模型</strong>
                ：必须选择支持图片理解的视觉模型，用于图片上传、图片问答和多模态预解析。
              </div>
              <div>
                2. <strong>聊天历史总结模型</strong>
                ：用于在长会话场景中压缩历史上下文，降低主对话链路的上下文负担。
              </div>
              <div>
                3. <strong>聊天标题生成模型</strong>
                ：用于根据会话首轮内容生成标题，便于历史会话检索和管理。
              </div>
            </div>
          </div>
        </Form>
      </Spin>
    </Drawer>
  );
};

export default CommonCapabilityConfigDrawer;
