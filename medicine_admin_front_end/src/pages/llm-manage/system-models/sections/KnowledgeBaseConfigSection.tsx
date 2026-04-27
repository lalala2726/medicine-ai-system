import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { CloseCircleFilled } from '@ant-design/icons';
import { Form, Input, InputNumber, Select, Spin, Switch, Typography, message } from 'antd';
import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
  useImperativeHandle,
  forwardRef,
} from 'react';

import { getRerankModelOptions, type SystemModelTypes } from '@/api/llm-manage/systemModels';

import styles from '../index.module.less';
import { getSystemModelsErrorMessage, type ConfigSectionRef } from '../shared';
import { isKnowledgeBaseEnabled } from '../utils';
import { createSlotValue, sanitizeRerankModelSelection } from '../components/modelSelectionUtils';

/**
 * 知识库选择器选项。
 */
interface KnowledgeBaseSelectOption {
  /** 下拉展示名称。 */
  label: string;
  /** 下拉选项值。 */
  value: string;
  /** 向量模型名称。 */
  embeddingModel?: string;
  /** 向量维度。 */
  embeddingDim?: number;
  /** 是否禁用。 */
  disabled?: boolean;
}

/**
 * 知识库配置表单值。
 */
interface KnowledgeBaseFormValues {
  /** 是否启用知识库。 */
  enabled?: boolean;
  /** 可访问知识库名称列表。 */
  knowledgeNames?: string[];
  /** 向量模型表单值。 */
  embeddingModel?: {
    /** 模型名称。 */
    modelName?: string;
    /** 是否启用深度思考。 */
    reasoningEnabled?: boolean;
  };
  /** 向量维度。 */
  embeddingDim?: number | null;
  /** 默认返回条数。 */
  topK?: number | null;
  /** 是否启用重排。 */
  rankingEnabled?: boolean;
  /** 重排模型表单值。 */
  rankingModel?: {
    /** 模型名称。 */
    modelName?: string;
    /** 是否启用深度思考。 */
    reasoningEnabled?: boolean;
  };
}

/**
 * 知识库配置区块属性。
 */
interface KnowledgeBaseConfigSectionProps {
  /** 区块标题。 */
  title: string;
  /** 区块描述。 */
  description: string;
  /** 启用知识库字段说明。 */
  enabledExtra: string;
  /** 加载当前配置方法。 */
  loadConfig: () => Promise<SystemModelTypes.KnowledgeBaseSystemModelConfig>;
  /** 加载知识库选项方法。 */
  loadKnowledgeBaseOptions: () => Promise<SystemModelTypes.KnowledgeBaseOption[]>;
  /** 保存配置方法。 */
  saveConfig: (data: SystemModelTypes.KnowledgeBaseSystemModelConfig) => Promise<unknown> | unknown;
  /** 加载失败提示。 */
  loadFailureMessage: string;
  /** 保存失败提示。 */
  saveFailureMessage: string;
  /** 启用开关测试标识。 */
  enabledSwitchTestId?: string;
  /** 知识库选择器测试标识。 */
  knowledgeNameSelectTestId?: string;
  /** 重排开关测试标识。 */
  rankingSwitchTestId?: string;
  /** 重排模型清空按钮测试标识。 */
  rankingClearTestId?: string;
}

/** 知识库最多可选数量。 */
const KNOWLEDGE_BASE_MAX_COUNT = 5;

/** 建议开启重排的知识库数量阈值。 */
const KNOWLEDGE_BASE_RANKING_SUGGEST_THRESHOLD = 3;

/** TopK 最小值。 */
const KNOWLEDGE_BASE_TOP_K_MIN = 1;

/** TopK 最大值。 */
const KNOWLEDGE_BASE_TOP_K_MAX = 100;

/**
 * 将知识库配置选项转换为下拉可用结构。
 *
 * @param option 原始知识库选项。
 * @returns 下拉选项。
 */
function toKnowledgeBaseOption(
  option: SystemModelTypes.KnowledgeBaseOption,
): KnowledgeBaseSelectOption {
  const label = option.displayName
    ? `${option.displayName} (${option.knowledgeName})`
    : option.knowledgeName;

  return {
    label,
    value: option.knowledgeName,
    embeddingModel: option.embeddingModel,
    embeddingDim: option.embeddingDim,
  };
}

/**
 * 确保当前已选知识库即使不在最新选项中也能继续展示。
 *
 * @param options 当前知识库选项列表。
 * @param selectedKnowledgeNames 当前已选知识库名称列表。
 * @returns 合并后的知识库选项列表。
 */
function ensureSelectedKnowledgeBaseOptions(
  options: KnowledgeBaseSelectOption[],
  selectedKnowledgeNames: string[],
): KnowledgeBaseSelectOption[] {
  const existing = new Set(options.map((item) => item.value));
  const nextOptions = [...options];

  selectedKnowledgeNames.forEach((knowledgeName) => {
    if (!existing.has(knowledgeName)) {
      nextOptions.unshift({
        label: knowledgeName,
        value: knowledgeName,
      });
    }
  });

  return nextOptions;
}

/**
 * 知识库配置区块。
 *
 * @param props 组件属性。
 * @returns 知识库配置区块节点。
 */
const KnowledgeBaseConfigSection = forwardRef<ConfigSectionRef, KnowledgeBaseConfigSectionProps>(
  (
    {
      title,
      description,
      enabledExtra,
      loadConfig,
      loadKnowledgeBaseOptions,
      saveConfig,
      loadFailureMessage,
      saveFailureMessage,
      enabledSwitchTestId,
      knowledgeNameSelectTestId,
      rankingSwitchTestId,
      rankingClearTestId,
    },
    ref,
  ) => {
    const [form] = Form.useForm<KnowledgeBaseFormValues>();
    const knowledgeBaseEnabled = Form.useWatch('enabled', form) as boolean | undefined;
    const selectedKnowledgeNames = Form.useWatch('knowledgeNames', form) as string[] | undefined;
    const rankingEnabled = Form.useWatch('rankingEnabled', form) as boolean | undefined;
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [detail, setDetail] = useState<SystemModelTypes.KnowledgeBaseSystemModelConfig>({});
    const [knowledgeBaseOptions, setKnowledgeBaseOptions] = useState<
      SystemModelTypes.KnowledgeBaseOption[]
    >([]);
    const [rankingOptions, setRankingOptions] = useState<SystemModelTypes.ModelOption[]>([]);
    const [invalidRankingHint, setInvalidRankingHint] = useState<string>();
    const selectedKnowledgeCount =
      selectedKnowledgeNames?.length ?? detail?.knowledgeNames?.length ?? 0;
    const rankingSuggested = selectedKnowledgeCount > KNOWLEDGE_BASE_RANKING_SUGGEST_THRESHOLD;

    /**
     * 加载当前知识库配置与模型选项。
     *
     * @returns 无返回值。
     */
    const loadSection = useCallback(async () => {
      setLoading(true);

      try {
        const [nextConfig, nextKnowledgeBaseOptions, nextRankingOptions] = await Promise.all([
          loadConfig(),
          loadKnowledgeBaseOptions(),
          getRerankModelOptions(),
        ]);
        const resolvedConfig = nextConfig || {};
        const resolvedKnowledgeBaseOptions = nextKnowledgeBaseOptions || [];
        const resolvedRankingOptions = nextRankingOptions || [];
        const sanitizedRankingModel = sanitizeRerankModelSelection(
          resolvedConfig?.rankingModel,
          resolvedRankingOptions,
        );

        setDetail(resolvedConfig);
        setKnowledgeBaseOptions(resolvedKnowledgeBaseOptions);
        setRankingOptions(resolvedRankingOptions);
        setInvalidRankingHint(sanitizedRankingModel.invalidSelectionHint);
        form.setFieldsValue({
          enabled: isKnowledgeBaseEnabled(resolvedConfig),
          knowledgeNames: resolvedConfig?.knowledgeNames ?? [],
          embeddingDim: resolvedConfig?.embeddingDim ?? null,
          topK: resolvedConfig?.topK ?? null,
          embeddingModel: createSlotValue(resolvedConfig?.embeddingModel),
          rankingEnabled:
            resolvedConfig?.rankingEnabled ?? Boolean(resolvedConfig?.rankingModel?.modelName),
          rankingModel: sanitizedRankingModel.value,
        });
      } catch (error) {
        message.error(getSystemModelsErrorMessage(error, loadFailureMessage));
      } finally {
        setLoading(false);
      }
    }, [form, loadConfig, loadFailureMessage, loadKnowledgeBaseOptions]);

    useEffect(() => {
      void loadSection();
    }, [loadSection]);

    const baselineKnowledgeName = selectedKnowledgeNames?.[0];

    /**
     * 当前基准知识库配置。
     */
    const baselineKnowledgeBase = useMemo(
      () =>
        knowledgeBaseOptions.find((item) => item.knowledgeName === baselineKnowledgeName) ?? null,
      [baselineKnowledgeName, knowledgeBaseOptions],
    );

    useEffect(() => {
      if (!baselineKnowledgeBase) {
        return;
      }

      form.setFieldsValue({
        embeddingDim: baselineKnowledgeBase.embeddingDim ?? null,
        embeddingModel: createSlotValue({
          modelName: baselineKnowledgeBase.embeddingModel,
          reasoningEnabled: false,
        }),
      });
    }, [baselineKnowledgeBase, form]);

    useEffect(() => {
      if (rankingEnabled !== false) {
        return;
      }

      form.setFieldsValue({
        rankingModel: createSlotValue(undefined),
      });
    }, [form, rankingEnabled]);

    /**
     * 当前页面可展示的知识库选项列表。
     */
    const displayKnowledgeBaseOptions = useMemo(() => {
      const selectedNames = selectedKnowledgeNames ?? detail?.knowledgeNames ?? [];
      const selectedSet = new Set(selectedNames);
      const maxReached = selectedNames.length >= KNOWLEDGE_BASE_MAX_COUNT;
      const rawOptions = knowledgeBaseOptions.map(toKnowledgeBaseOption);

      if (!baselineKnowledgeBase) {
        return ensureSelectedKnowledgeBaseOptions(rawOptions, selectedNames).map((item) => ({
          ...item,
          disabled: maxReached && !selectedSet.has(item.value),
        }));
      }

      const filteredOptions = rawOptions.filter((item) => {
        if (selectedSet.has(item.value)) {
          return true;
        }

        return (
          item.embeddingModel === baselineKnowledgeBase.embeddingModel &&
          item.embeddingDim === baselineKnowledgeBase.embeddingDim
        );
      });

      return ensureSelectedKnowledgeBaseOptions(filteredOptions, selectedNames).map((item) => ({
        ...item,
        disabled: maxReached && !selectedSet.has(item.value),
      }));
    }, [
      baselineKnowledgeBase,
      detail?.knowledgeNames,
      knowledgeBaseOptions,
      selectedKnowledgeNames,
    ]);

    /**
     * 保存当前知识库配置。
     *
     * @returns 无返回值。
     */
    const handleSave = useCallback(async () => {
      try {
        const formValues = await form.validateFields();

        setSaving(true);
        await saveConfig({
          enabled: formValues.enabled,
          knowledgeNames: formValues.knowledgeNames,
          embeddingDim: formValues.embeddingDim ?? undefined,
          topK: formValues.topK ?? null,
          embeddingModel: formValues.embeddingModel?.modelName
            ? {
                modelName: formValues.embeddingModel.modelName,
                reasoningEnabled: Boolean(formValues.embeddingModel.reasoningEnabled),
              }
            : null,
          rankingEnabled: formValues.rankingEnabled,
          rankingModel:
            formValues.rankingEnabled && formValues.rankingModel?.modelName
              ? {
                  modelName: formValues.rankingModel.modelName,
                  reasoningEnabled: Boolean(formValues.rankingModel.reasoningEnabled),
                }
              : null,
        });
      } catch (error) {
        if ((error as { errorFields?: unknown[] } | null)?.errorFields) {
          throw error;
        }
        message.error(getSystemModelsErrorMessage(error, saveFailureMessage));
        throw error;
      } finally {
        setSaving(false);
      }
    }, [form, saveConfig, saveFailureMessage]);

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
            <div className={styles.sectionCardTitle}>{title}</div>
            <Typography.Text type="secondary" className={styles.sectionCardDescription}>
              {description}
            </Typography.Text>
          </div>
        </div>

        <Spin spinning={loading || saving}>
          <Form
            form={form}
            layout="vertical"
            initialValues={{ enabled: false, rankingEnabled: false }}
          >
            <div className={styles.drawerBody}>
              <div className={styles.formCluster}>
                <Form.Item
                  label="启用知识库"
                  name="enabled"
                  valuePropName="checked"
                  extra={enabledExtra}
                  className={styles.compactFormItem}
                >
                  <Switch data-testid={enabledSwitchTestId} />
                </Form.Item>

                <Form.Item
                  label="知识库名称"
                  name="knowledgeNames"
                  rules={[
                    {
                      validator: async (_rule, value?: string[]) => {
                        if (!form.getFieldValue('enabled')) {
                          return;
                        }
                        if (!value || value.length === 0) {
                          throw new Error('请至少选择一个知识库');
                        }
                        if (value.length > KNOWLEDGE_BASE_MAX_COUNT) {
                          throw new Error('知识库最多支持5个');
                        }
                      },
                    },
                  ]}
                  className={styles.compactFormItem}
                >
                  <Select
                    mode="multiple"
                    showSearch
                    optionFilterProp="label"
                    placeholder="请选择可访问的知识库"
                    options={displayKnowledgeBaseOptions}
                    disabled={!knowledgeBaseEnabled}
                    loading={loading}
                    notFoundContent={null}
                    data-testid={knowledgeNameSelectTestId}
                  />
                </Form.Item>

                <Form.Item
                  label="向量模型"
                  name={['embeddingModel', 'modelName']}
                  className={styles.compactFormItem}
                >
                  <Input
                    autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                    readOnly
                    disabled
                    placeholder="选择第一个知识库后自动带出"
                  />
                </Form.Item>

                <Form.Item label="向量维度" name="embeddingDim" className={styles.compactFormItem}>
                  <InputNumber
                    autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                    disabled
                    precision={0}
                    style={{ width: '100%' }}
                    placeholder="选择第一个知识库后自动带出"
                  />
                </Form.Item>

                <Form.Item
                  label="默认返回条数 TopK"
                  name="topK"
                  rules={[
                    {
                      validator: async (_rule, value?: number | null) => {
                        if (!form.getFieldValue('enabled')) {
                          return;
                        }
                        if (value == null) {
                          return;
                        }
                        if (value < KNOWLEDGE_BASE_TOP_K_MIN) {
                          throw new Error('知识库返回条数不能小于1');
                        }
                        if (value > KNOWLEDGE_BASE_TOP_K_MAX) {
                          throw new Error('知识库返回条数不能大于100');
                        }
                      },
                    },
                  ]}
                  extra="为空时使用 AI 端默认值 10"
                  className={styles.compactFormItem}
                >
                  <InputNumber
                    autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                    min={KNOWLEDGE_BASE_TOP_K_MIN}
                    max={KNOWLEDGE_BASE_TOP_K_MAX}
                    precision={0}
                    style={{ width: '100%' }}
                    placeholder="默认 10"
                    disabled={!knowledgeBaseEnabled}
                  />
                </Form.Item>

                <div className={styles.fieldDivider} />

                <Form.Item
                  label="启用结果重排"
                  name="rankingEnabled"
                  valuePropName="checked"
                  extra={
                    knowledgeBaseEnabled && rankingSuggested
                      ? '关联知识库较多时，建议开启重排以提升回答准确度'
                      : undefined
                  }
                  className={styles.compactFormItem}
                  tooltip="开启后，系统将对检索出的原始资料进行二次精准筛选"
                >
                  <Switch data-testid={rankingSwitchTestId} disabled={!knowledgeBaseEnabled} />
                </Form.Item>

                <Form.Item
                  label="重排模型"
                  name={['rankingModel', 'modelName']}
                  rules={[
                    {
                      validator: async (_rule, value?: string) => {
                        if (!form.getFieldValue('enabled')) {
                          return;
                        }
                        if (!form.getFieldValue('rankingEnabled') || value) {
                          return;
                        }
                        throw new Error('请选择用于重排的模型');
                      },
                    },
                  ]}
                  className={styles.compactFormItem}
                  tooltip="选择一个逻辑能力较强的模型来对搜索结果进行优先级排序"
                  extra={invalidRankingHint}
                >
                  <Select
                    allowClear={{
                      clearIcon: <CloseCircleFilled data-testid={rankingClearTestId} />,
                    }}
                    disabled={!knowledgeBaseEnabled || !rankingEnabled}
                    showSearch
                    optionFilterProp="label"
                    placeholder="请选择重排模型"
                    options={rankingOptions}
                    loading={loading}
                    notFoundContent={null}
                  />
                </Form.Item>
              </div>
            </div>
          </Form>
        </Spin>
      </div>
    );
  },
);

export default KnowledgeBaseConfigSection;
