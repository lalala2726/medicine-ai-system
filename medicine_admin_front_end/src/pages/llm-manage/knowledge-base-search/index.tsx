import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { SearchOutlined } from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import {
  Alert,
  Button,
  Card,
  Empty,
  Form,
  Input,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import {
  getKnowledgeBaseById,
  searchKnowledgeBase,
  type KnowledgeBaseTypes,
} from '@/api/llm-manage/knowledgeBase';
import {
  getKnowledgeBaseOptions,
  getRerankModelOptions,
  type SystemModelTypes,
} from '@/api/llm-manage/systemModels';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { buildKnowledgeBaseDocumentPath, routePaths } from '@/router/paths';

import styles from './index.module.less';

/**
 * 检索页知识库最多可选数量。
 */
const KNOWLEDGE_BASE_SEARCH_MAX_COUNT = 5;

/**
 * 检索页默认返回结果文案。
 */
const DEFAULT_EMPTY_DESCRIPTION = '输入问题并执行检索后，这里会展示命中的知识片段';

/**
 * 检索表单值。
 */
interface KnowledgeBaseSearchFormValues {
  /** 检索问题。 */
  question?: string;
  /** 参与检索的知识库名称列表。 */
  knowledgeNames?: string[];
  /** 本次检索使用的重排模型名称。 */
  rankingModel?: string;
}

/**
 * 知识库下拉选项。
 */
interface KnowledgeBaseSelectOption {
  /** 选项展示标签。 */
  label: string;
  /** 选项值。 */
  value: string;
  /** 向量模型名称。 */
  embeddingModel?: string;
  /** 向量维度。 */
  embeddingDim?: number;
  /** 是否禁用。 */
  disabled?: boolean;
}

/**
 * 将知识库选项转换为前端可用结构。
 * @param option 原始知识库选项。
 * @returns 前端可用下拉选项。
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
 * 补齐当前已选知识库选项，避免筛选后丢失展示。
 * @param options 当前可选项列表。
 * @param selectedKnowledgeNames 当前已选知识库名称列表。
 * @returns 补齐后的可选项列表。
 */
function ensureSelectedKnowledgeBaseOptions(
  options: KnowledgeBaseSelectOption[],
  selectedKnowledgeNames: string[],
): KnowledgeBaseSelectOption[] {
  const existingValues = new Set(options.map((item) => item.value));
  const nextOptions = [...options];
  selectedKnowledgeNames.forEach((knowledgeName) => {
    if (!existingValues.has(knowledgeName)) {
      nextOptions.unshift({
        label: knowledgeName,
        value: knowledgeName,
      });
    }
  });
  return nextOptions;
}

/**
 * 构建当前页面可展示的知识库选项列表。
 * @param options 原始知识库选项列表。
 * @param selectedKnowledgeNames 当前已选知识库名称列表。
 * @returns 当前页面可展示的知识库选项列表。
 */
function buildDisplayKnowledgeBaseOptions(
  options: SystemModelTypes.KnowledgeBaseOption[],
  selectedKnowledgeNames: string[],
): KnowledgeBaseSelectOption[] {
  const rawOptions = options.map(toKnowledgeBaseOption);
  const selectedSet = new Set(selectedKnowledgeNames);
  const maxReached = selectedKnowledgeNames.length >= KNOWLEDGE_BASE_SEARCH_MAX_COUNT;
  const baselineKnowledgeName = selectedKnowledgeNames[0];
  const baselineKnowledgeBase = options.find(
    (item) => item.knowledgeName === baselineKnowledgeName,
  );

  if (!baselineKnowledgeBase) {
    return ensureSelectedKnowledgeBaseOptions(rawOptions, selectedKnowledgeNames).map((item) => ({
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

  return ensureSelectedKnowledgeBaseOptions(filteredOptions, selectedKnowledgeNames).map(
    (item) => ({
      ...item,
      disabled: maxReached && !selectedSet.has(item.value),
    }),
  );
}

/**
 * 提取请求失败提示文案。
 * @param error 请求异常对象。
 * @param fallbackMessage 默认错误提示。
 * @returns 最终展示给用户的错误文案。
 */
function getErrorMessage(error: unknown, fallbackMessage: string): string {
  if (
    typeof error === 'object' &&
    error &&
    'message' in error &&
    typeof error.message === 'string'
  ) {
    return error.message;
  }
  return fallbackMessage;
}

/**
 * 构建返回知识库文档页的路径。
 * @param currentKnowledgeBaseId 当前知识库 ID。
 * @param currentKnowledgeBaseName 当前知识库名称。
 * @returns 返回路径。
 */
function buildBackPath(currentKnowledgeBaseId?: number, currentKnowledgeBaseName?: string): string {
  if (currentKnowledgeBaseId) {
    return buildKnowledgeBaseDocumentPath({
      knowledgeBaseId: currentKnowledgeBaseId,
      knowledgeBaseName: currentKnowledgeBaseName || '知识库',
    });
  }
  return routePaths.llmKnowledgeBase;
}

/**
 * 后台知识库结构化检索页面。
 * @returns 页面节点。
 */
const KnowledgeBaseSearch: React.FC = () => {
  const [form] = Form.useForm<KnowledgeBaseSearchFormValues>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);
  const [searched, setSearched] = useState(false);
  const [knowledgeBaseOptions, setKnowledgeBaseOptions] = useState<
    SystemModelTypes.KnowledgeBaseOption[]
  >([]);
  const [rerankModelOptions, setRerankModelOptions] = useState<SystemModelTypes.ModelOption[]>([]);
  const [hits, setHits] = useState<KnowledgeBaseTypes.KnowledgeBaseSearchHit[]>([]);

  const selectedKnowledgeNames =
    (Form.useWatch('knowledgeNames', form) as string[] | undefined) || [];
  const selectedRankingModelName = Form.useWatch('rankingModel', form) as string | undefined;
  const currentKnowledgeBaseId = Number(searchParams.get('id')) || undefined;
  const currentKnowledgeBaseName = searchParams.get('name') || '知识库';
  const rerankModelReady = rerankModelOptions.length > 0;
  const displayKnowledgeBaseOptions = buildDisplayKnowledgeBaseOptions(
    knowledgeBaseOptions,
    selectedKnowledgeNames,
  );

  /**
   * 加载页面初始化数据。
   * @returns 无返回值。
   */
  const loadPageData = useCallback(async () => {
    setLoading(true);
    try {
      const [options, rankingOptions, currentKnowledgeBase] = await Promise.all([
        getKnowledgeBaseOptions(),
        getRerankModelOptions(),
        currentKnowledgeBaseId
          ? getKnowledgeBaseById(currentKnowledgeBaseId)
          : Promise.resolve(undefined),
      ]);
      const resolvedOptions = options || [];
      const resolvedRankingOptions = rankingOptions || [];
      const currentKnowledgeName = currentKnowledgeBase?.knowledgeName;
      const canUseCurrentKnowledgeBase = Boolean(
        currentKnowledgeName &&
        resolvedOptions.some((item) => item.knowledgeName === currentKnowledgeName),
      );

      setKnowledgeBaseOptions(resolvedOptions);
      setRerankModelOptions(resolvedRankingOptions);
      form.setFieldsValue({
        question: form.getFieldValue('question'),
        knowledgeNames:
          canUseCurrentKnowledgeBase && currentKnowledgeName ? [currentKnowledgeName] : [],
        rankingModel: undefined,
      });
    } catch (error) {
      message.error(getErrorMessage(error, '知识检索页面加载失败'));
    } finally {
      setLoading(false);
    }
  }, [currentKnowledgeBaseId, form]);

  useEffect(() => {
    void loadPageData();
  }, [loadPageData]);

  /**
   * 提交知识检索。
   * @returns 无返回值。
   */
  const handleSearch = async () => {
    try {
      const values = await form.validateFields();
      setSearching(true);
      const result = await searchKnowledgeBase({
        question: values.question?.trim() || '',
        knowledgeNames: values.knowledgeNames || [],
        rankingModel: values.rankingModel || undefined,
      });
      setHits(result?.hits || []);
      setSearched(true);
    } catch (error) {
      if (typeof error === 'object' && error && 'errorFields' in error) {
        return;
      }
      message.error(getErrorMessage(error, '知识检索失败'));
    } finally {
      setSearching(false);
    }
  };

  return (
    <PageContainer
      title="知识检索"
      onBack={() => navigate(buildBackPath(currentKnowledgeBaseId, currentKnowledgeBaseName))}
      breadcrumb={{
        items: [
          { title: '大模型管理' },
          { title: currentKnowledgeBaseName },
          { title: '知识检索' },
        ],
      }}
    >
      <Spin spinning={loading}>
        <div className={styles.pageCard}>
          <Card className={styles.searchCard} bordered={false}>
            <Form form={form} layout="vertical" initialValues={{ knowledgeNames: [] }}>
              <Form.Item
                label="检索问题"
                name="question"
                rules={[
                  {
                    validator: async (_rule, value?: string) => {
                      if (!value || !value.trim()) {
                        throw new Error('请输入检索问题');
                      }
                    },
                  },
                ]}
              >
                <Input.TextArea
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  placeholder="请输入需要检索的问题，例如：感冒药与退烧药能否同时服用？"
                  autoSize={{ minRows: 3, maxRows: 6 }}
                  maxLength={500}
                  showCount
                />
              </Form.Item>

              <Form.Item
                label="知识库"
                name="knowledgeNames"
                extra="最多选择 5 个，且多选时仅支持向量模型和维度一致的知识库组合"
                rules={[
                  {
                    validator: async (_rule, value?: string[]) => {
                      if (!value || value.length === 0) {
                        throw new Error('请至少选择一个知识库');
                      }
                      if (value.length > KNOWLEDGE_BASE_SEARCH_MAX_COUNT) {
                        throw new Error('知识库最多支持5个');
                      }
                    },
                  },
                ]}
              >
                <Select
                  mode="multiple"
                  showSearch
                  optionFilterProp="label"
                  placeholder="请选择参与检索的知识库"
                  options={displayKnowledgeBaseOptions}
                  maxTagCount="responsive"
                />
              </Form.Item>

              <Alert
                showIcon
                type={rerankModelReady ? 'info' : 'warning'}
                message={
                  rerankModelReady
                    ? selectedRankingModelName
                      ? `当前检索已选择重排模型：${selectedRankingModelName}`
                      : '可按需选择系统中的重排模型；选择后本次检索启用重排，不选择则不启用。'
                    : '当前没有可用的系统重排模型，如需启用重排，请先到系统模型配置中配置可用模型。'
                }
                action={
                  <PermissionButton
                    type="link"
                    size="small"
                    access={ADMIN_PERMISSIONS.agentConfig.adminQuery}
                    onClick={() => navigate(routePaths.llmSystemModelsAdminConfig)}
                  >
                    去配置
                  </PermissionButton>
                }
                style={{ marginBottom: 20 }}
              />

              <Form.Item
                label="重排模型"
                name="rankingModel"
                extra="选择系统中的重排模型后，本次检索启用重排；不选择则关闭重排"
              >
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  placeholder={
                    rerankModelReady ? '请选择本次检索使用的重排模型' : '当前暂无可用重排模型'
                  }
                  options={rerankModelOptions}
                  disabled={!rerankModelReady}
                />
              </Form.Item>

              <Space>
                <PermissionButton
                  type="primary"
                  icon={<SearchOutlined />}
                  loading={searching}
                  access={ADMIN_PERMISSIONS.knowledgeBase.query}
                  onClick={() => void handleSearch()}
                >
                  开始检索
                </PermissionButton>
                <Button
                  onClick={() => {
                    setSearched(false);
                    setHits([]);
                    form.resetFields();
                    form.setFieldsValue({ knowledgeNames: [], rankingModel: undefined });
                    void loadPageData();
                  }}
                >
                  重置
                </Button>
              </Space>
            </Form>
          </Card>

          <Card
            className={styles.resultCard}
            bordered={false}
            title={
              <div className={styles.resultHeader}>
                <span>命中结果</span>
                {searched ? (
                  <Typography.Text type="secondary">
                    共命中 {hits.length} 条知识片段
                  </Typography.Text>
                ) : null}
              </div>
            }
          >
            {!searched ? (
              <div className={styles.emptyWrap}>
                <Empty description={DEFAULT_EMPTY_DESCRIPTION} />
              </div>
            ) : hits.length === 0 ? (
              <div className={styles.emptyWrap}>
                <Empty description="未检索到相关知识片段" />
              </div>
            ) : (
              <div className={styles.resultList}>
                {hits.map((item, index) => (
                  <div
                    key={`${item.knowledgeName}-${item.documentId || 'doc'}-${item.chunkIndex || index}`}
                    className={styles.resultItem}
                  >
                    <div className={styles.resultItemHeader}>
                      <Space wrap size={8}>
                        <Tag color="blue">{item.knowledgeDisplayName || item.knowledgeName}</Tag>
                        <Typography.Text type="secondary">{item.knowledgeName}</Typography.Text>
                      </Space>
                      <Tag color="geekblue">相似度 {item.score.toFixed(4)}</Tag>
                    </div>
                    <div className={styles.metaList}>
                      <Tag>documentId: {item.documentId ?? '-'}</Tag>
                      <Tag>chunkIndex: {item.chunkIndex ?? '-'}</Tag>
                      <Tag>charCount: {item.charCount ?? '-'}</Tag>
                    </div>
                    <Typography.Paragraph className={styles.content}>
                      {item.content}
                    </Typography.Paragraph>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      </Spin>
    </PageContainer>
  );
};

export default KnowledgeBaseSearch;
