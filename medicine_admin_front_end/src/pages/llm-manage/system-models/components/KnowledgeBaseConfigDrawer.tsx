import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { CloseCircleFilled, InfoCircleOutlined } from '@ant-design/icons';
import {
  Button,
  Card,
  Drawer,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Spin,
  Switch,
  Typography,
  message,
} from 'antd';
import React, { useCallback, useEffect, useMemo, useState } from 'react';

import {
  getKnowledgeBaseOptions,
  getRerankModelOptions,
  saveKnowledgeBaseConfig,
  type SystemModelTypes,
} from '@/api/llm-manage/systemModels';
import { useThemeContext } from '@/contexts/ThemeContext';

import styles from '../index.module.less';
import { isKnowledgeBaseEnabled } from '../utils';
import { createSlotValue, sanitizeRerankModelSelection } from './modelSelectionUtils';

interface KnowledgeBaseConfigDrawerProps {
  open: boolean;
  detail?: SystemModelTypes.KnowledgeBaseSystemModelConfig;
  onClose: () => void;
  onSaved: () => Promise<void> | void;
}

interface KnowledgeBaseSelectOption {
  label: string;
  value: string;
  embeddingModel?: string;
  embeddingDim?: number;
  disabled?: boolean;
}

const KNOWLEDGE_BASE_MAX_COUNT = 5;
const KNOWLEDGE_BASE_RANKING_SUGGEST_THRESHOLD = 3;
const KNOWLEDGE_BASE_TOP_K_MIN = 1;
const KNOWLEDGE_BASE_TOP_K_MAX = 100;

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

function ensureSelectedKnowledgeBaseOptions(
  options: KnowledgeBaseSelectOption[],
  selectedKnowledgeNames: string[],
) {
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

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

/**
 * 知识库系统模型配置抽屉。
 */
const KnowledgeBaseConfigDrawer: React.FC<KnowledgeBaseConfigDrawerProps> = ({
  open,
  detail,
  onClose,
  onSaved,
}) => {
  const { isDark } = useThemeContext();
  const [form] = Form.useForm();
  const knowledgeBaseEnabled = Form.useWatch('enabled', form) as boolean | undefined;
  const selectedKnowledgeNames = Form.useWatch('knowledgeNames', form) as string[] | undefined;
  const rankingEnabled = Form.useWatch('rankingEnabled', form) as boolean | undefined;
  const [saving, setSaving] = useState(false);
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [knowledgeBaseOptions, setKnowledgeBaseOptions] = useState<
    SystemModelTypes.KnowledgeBaseOption[]
  >([]);
  const [rankingOptions, setRankingOptions] = useState<SystemModelTypes.ModelOption[]>([]);
  const [invalidRankingHint, setInvalidRankingHint] = useState<string>();
  const selectedKnowledgeCount =
    selectedKnowledgeNames?.length ?? detail?.knowledgeNames?.length ?? 0;
  const rankingSuggested = selectedKnowledgeCount > KNOWLEDGE_BASE_RANKING_SUGGEST_THRESHOLD;

  const loadOptions = useCallback(async () => {
    setOptionsLoading(true);
    try {
      const [nextKnowledgeBaseOptions, nextRankingOptions] = await Promise.all([
        getKnowledgeBaseOptions(),
        getRerankModelOptions(),
      ]);
      const nextKnowledgeOptions = nextKnowledgeBaseOptions || [];
      const nextOptions = nextRankingOptions || [];
      const sanitizedRankingModel = sanitizeRerankModelSelection(detail?.rankingModel, nextOptions);
      setKnowledgeBaseOptions(nextKnowledgeOptions);
      setRankingOptions(nextOptions);
      setInvalidRankingHint(sanitizedRankingModel.invalidSelectionHint);
      form.setFieldsValue({
        rankingModel: sanitizedRankingModel.value,
      });
    } finally {
      setOptionsLoading(false);
    }
  }, [detail?.rankingModel, form]);

  useEffect(() => {
    if (!open) {
      form.resetFields();
      setInvalidRankingHint(undefined);
      return;
    }

    form.setFieldsValue({
      enabled: isKnowledgeBaseEnabled(detail),
      knowledgeNames: detail?.knowledgeNames ?? [],
      embeddingDim: detail?.embeddingDim ?? null,
      topK: detail?.topK ?? null,
      embeddingModel: createSlotValue(detail?.embeddingModel),
      rankingEnabled: detail?.rankingEnabled ?? Boolean(detail?.rankingModel?.modelName),
      rankingModel: createSlotValue(detail?.rankingModel),
    });
    void loadOptions();
  }, [detail, form, loadOptions, open]);

  const baselineKnowledgeName = selectedKnowledgeNames?.[0];

  const baselineKnowledgeBase = useMemo(
    () => knowledgeBaseOptions.find((item) => item.knowledgeName === baselineKnowledgeName) ?? null,
    [baselineKnowledgeName, knowledgeBaseOptions],
  );

  useEffect(() => {
    if (!open || !baselineKnowledgeBase) {
      return;
    }
    form.setFieldsValue({
      embeddingDim: baselineKnowledgeBase.embeddingDim ?? null,
      embeddingModel: createSlotValue({
        modelName: baselineKnowledgeBase.embeddingModel,
        reasoningEnabled: false,
      }),
    });
  }, [baselineKnowledgeBase, form, open]);

  useEffect(() => {
    if (!open || rankingEnabled !== false) {
      return;
    }
    form.setFieldsValue({
      rankingModel: createSlotValue(undefined),
    });
  }, [form, open, rankingEnabled]);

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

    const filtered = rawOptions.filter((item) => {
      if (selectedSet.has(item.value)) {
        return true;
      }
      return (
        item.embeddingModel === baselineKnowledgeBase.embeddingModel &&
        item.embeddingDim === baselineKnowledgeBase.embeddingDim
      );
    });
    return ensureSelectedKnowledgeBaseOptions(filtered, selectedNames).map((item) => ({
      ...item,
      disabled: maxReached && !selectedSet.has(item.value),
    }));
  }, [baselineKnowledgeBase, detail?.knowledgeNames, knowledgeBaseOptions, selectedKnowledgeNames]);

  /**
   * 保存知识库配置，仅提交当前编辑态需要的字段。
   */
  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();

      setSaving(true);
      await saveKnowledgeBaseConfig({
        enabled: Boolean(values.enabled),
        knowledgeNames: values.knowledgeNames,
        embeddingDim: values.embeddingDim,
        topK: values.topK ?? null,
        embeddingModel: values.embeddingModel?.modelName
          ? {
              modelName: values.embeddingModel?.modelName,
              reasoningEnabled: Boolean(values.embeddingModel?.reasoningEnabled),
            }
          : null,
        rankingEnabled: Boolean(values.rankingEnabled),
        rankingModel:
          values.rankingEnabled && values.rankingModel?.modelName
            ? {
                modelName: values.rankingModel?.modelName,
                reasoningEnabled: Boolean(values.rankingModel?.reasoningEnabled),
              }
            : null,
      });
      message.success('管理端知识库配置已保存');
      await onSaved();
      onClose();
    } catch (error) {
      if ((error as { errorFields?: unknown[] } | null)?.errorFields) {
        return;
      }
      message.error(getErrorMessage(error, '管理端知识库配置保存失败'));
    } finally {
      setSaving(false);
    }
  }, [form, onClose, onSaved]);

  return (
    <Drawer
      title="配置管理端知识库"
      width={640}
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
      <Spin
        spinning={
          optionsLoading && knowledgeBaseOptions.length === 0 && rankingOptions.length === 0
        }
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ enabled: false, rankingEnabled: false }}
        >
          <div className={styles.drawerBody}>
            <Card className={styles.slotCard} size="small">
              <div className={styles.slotHeader}>
                <div>
                  <div className={styles.slotTitle}>知识库访问与检索配置</div>
                  <Typography.Text type="secondary" className={styles.slotDescription}>
                    第一个知识库将作为基准，后续新增的知识库必须和它使用相同的向量模型与向量维度。
                  </Typography.Text>
                </div>
              </div>

              <Form.Item
                label="启用知识库"
                name="enabled"
                valuePropName="checked"
                extra="关闭后 AI 将不再访问知识库，但会保留当前配置，重新开启后可继续使用"
                className={styles.compactFormItem}
              >
                <Switch data-testid="knowledge-base-enabled-switch" />
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
                  loading={optionsLoading}
                  notFoundContent={null}
                  data-testid="knowledge-base-name-select"
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
                  min={1}
                  max={100}
                  precision={0}
                  style={{ width: '100%' }}
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
                <Switch data-testid="knowledge-ranking-switch" disabled={!knowledgeBaseEnabled} />
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
                    clearIcon: <CloseCircleFilled data-testid="knowledge-ranking-clear" />,
                  }}
                  disabled={!knowledgeBaseEnabled || !rankingEnabled}
                  showSearch
                  optionFilterProp="label"
                  placeholder="请选择重排模型"
                  options={rankingOptions}
                  loading={optionsLoading}
                  notFoundContent={null}
                />
              </Form.Item>
            </Card>

            <div className={styles.notesSection}>
              <InfoCircleOutlined />
              <div className={styles.notesContent}>
                <div>
                  <strong>配置说明：</strong>
                </div>
                这里配置的是 AI 查询知识库时可访问的知识库范围，最多支持选择 5
                个。第一个知识库决定了当前配置的向量模型和维度；当关联知识库超过 3
                个时建议开启“结果重排”。开启后将使用选定的模型对原始检索结果进行二次精选，这能显著提升复杂知识场景下的回答质量。如果关闭，系统将直接使用原始检索结果（TopK
                默认值为 10）。
              </div>
            </div>
          </div>
        </Form>
      </Spin>
    </Drawer>
  );
};

export default KnowledgeBaseConfigDrawer;
