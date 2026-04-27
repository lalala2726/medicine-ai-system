import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { HistoryOutlined, InfoCircleOutlined } from '@ant-design/icons';
import {
  AutoComplete,
  Button,
  Drawer,
  Empty,
  Form,
  Input,
  List,
  Popconfirm,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from 'antd';
import React, { useCallback, useEffect, useMemo, useState } from 'react';

import {
  getPromptConfig,
  getPromptHistory,
  getPromptKeyOptions,
  rollbackPromptConfig,
  savePromptConfig,
  type SystemModelTypes,
} from '@/api/llm-manage/systemModels';
import { useThemeContext } from '@/contexts/ThemeContext';

import styles from '../index.module.less';

interface PromptConfigDrawerProps {
  open: boolean;
  onClose: () => void;
  onSaved: () => Promise<void> | void;
}

interface PromptFormValues {
  promptKey?: string;
  promptContent?: string;
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
 * 格式化时间文案。
 *
 * @param rawValue 原始时间值。
 * @returns 格式化后的时间文案。
 */
function formatDateTime(rawValue?: string) {
  if (!rawValue) {
    return '-';
  }
  const date = new Date(rawValue);
  if (Number.isNaN(date.getTime())) {
    return rawValue;
  }
  return date.toLocaleString();
}

/**
 * 提示词配置抽屉。
 */
const PromptConfigDrawer: React.FC<PromptConfigDrawerProps> = ({ open, onClose, onSaved }) => {
  const { isDark } = useThemeContext();
  const [form] = Form.useForm<PromptFormValues>();
  const [keyOptionsLoading, setKeyOptionsLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [rollbackLoadingVersion, setRollbackLoadingVersion] = useState<number>();
  const [keyOptions, setKeyOptions] = useState<SystemModelTypes.PromptKeyOption[]>([]);
  const [selectedPromptKey, setSelectedPromptKey] = useState<string>();
  const [currentPromptDetail, setCurrentPromptDetail] = useState<SystemModelTypes.PromptConfig>();
  const [promptHistory, setPromptHistory] = useState<SystemModelTypes.PromptHistoryItem[]>([]);

  /**
   * 读取提示词键选项。
   */
  const loadPromptKeyOptions = useCallback(async () => {
    setKeyOptionsLoading(true);
    try {
      const nextOptions = (await getPromptKeyOptions()) || [];
      setKeyOptions(nextOptions);
      return nextOptions;
    } finally {
      setKeyOptionsLoading(false);
    }
  }, []);

  /**
   * 加载指定提示词详情与历史版本。
   *
   * @param promptKey 提示词业务键。
   */
  const loadPromptPayload = useCallback(
    async (promptKey: string) => {
      setDetailLoading(true);
      try {
        let nextDetail: SystemModelTypes.PromptConfig | undefined;
        let nextHistory: SystemModelTypes.PromptHistoryItem[] = [];
        try {
          nextDetail = await getPromptConfig(promptKey);
        } catch {
          nextDetail = {
            promptKey,
            promptContent: '',
            promptVersion: null,
          };
        }
        try {
          nextHistory = (await getPromptHistory(promptKey, 30)) || [];
        } catch {
          nextHistory = [];
        }
        setCurrentPromptDetail(nextDetail || { promptKey, promptContent: '' });
        setPromptHistory(nextHistory);
        form.setFieldsValue({
          promptKey,
          promptContent: nextDetail?.promptContent || '',
        });
      } finally {
        setDetailLoading(false);
      }
    },
    [form],
  );

  /**
   * 初始化抽屉数据。
   */
  const initializeDrawer = useCallback(async () => {
    const options = await loadPromptKeyOptions();
    const initialPromptKey =
      selectedPromptKey && options.some((item) => item.promptKey === selectedPromptKey)
        ? selectedPromptKey
        : options[0]?.promptKey;
    setSelectedPromptKey(initialPromptKey);
    if (!initialPromptKey) {
      setCurrentPromptDetail(undefined);
      setPromptHistory([]);
      form.setFieldsValue({ promptKey: undefined, promptContent: '' });
      return;
    }
    await loadPromptPayload(initialPromptKey);
  }, [form, loadPromptKeyOptions, loadPromptPayload, selectedPromptKey]);

  useEffect(() => {
    if (!open) {
      form.resetFields();
      setSelectedPromptKey(undefined);
      setCurrentPromptDetail(undefined);
      setPromptHistory([]);
      setRollbackLoadingVersion(undefined);
      return;
    }
    void initializeDrawer();
  }, [form, initializeDrawer, open]);

  /**
   * 处理提示词键切换。
   *
   * @param promptKey 当前选择的提示词键。
   */
  const handlePromptKeyChange = useCallback(
    async (promptKey: string) => {
      setSelectedPromptKey(promptKey);
      await loadPromptPayload(promptKey);
    },
    [loadPromptPayload],
  );

  /**
   * 保存提示词配置。
   */
  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();
      if (!values.promptKey) {
        return;
      }
      setSaving(true);
      await savePromptConfig({
        promptKey: values.promptKey,
        promptContent: values.promptContent || '',
      });
      message.success('提示词配置已保存');
      await onSaved();
      const latestOptions = await loadPromptKeyOptions();
      setKeyOptions(latestOptions);
      await loadPromptPayload(values.promptKey);
    } catch (error) {
      if ((error as { errorFields?: unknown[] } | null)?.errorFields) {
        return;
      }
      message.error(getErrorMessage(error, '提示词配置保存失败'));
    } finally {
      setSaving(false);
    }
  }, [form, loadPromptKeyOptions, loadPromptPayload, onSaved]);

  /**
   * 回滚到指定历史版本。
   *
   * @param targetVersion 目标版本号。
   */
  const handleRollback = useCallback(
    async (targetVersion: number) => {
      if (!selectedPromptKey) {
        return;
      }
      setRollbackLoadingVersion(targetVersion);
      try {
        await rollbackPromptConfig({
          promptKey: selectedPromptKey,
          targetVersion,
        });
        message.success(`已回滚到版本 v${targetVersion}（并生成新版本）`);
        await onSaved();
        const latestOptions = await loadPromptKeyOptions();
        setKeyOptions(latestOptions);
        await loadPromptPayload(selectedPromptKey);
      } catch (error) {
        message.error(getErrorMessage(error, '提示词回滚失败'));
      } finally {
        setRollbackLoadingVersion(undefined);
      }
    },
    [loadPromptKeyOptions, loadPromptPayload, onSaved, selectedPromptKey],
  );

  /**
   * 当前选中提示词键的元信息。
   */
  const selectedPromptMeta = useMemo(
    () => keyOptions.find((item) => item.promptKey === selectedPromptKey),
    [keyOptions, selectedPromptKey],
  );

  return (
    <Drawer
      title="配置统一提示词"
      width={980}
      open={open}
      destroyOnClose
      onClose={onClose}
      rootClassName={isDark ? 'app-pro-layout--dark' : ''}
      footer={
        <div className={styles.drawerFooter}>
          <Space>
            <Button onClick={onClose}>关闭</Button>
            <Button type="primary" loading={saving} onClick={handleSave}>
              保存配置
            </Button>
          </Space>
        </div>
      }
    >
      <Spin spinning={keyOptionsLoading || detailLoading}>
        <Form form={form} layout="vertical">
          <div className={styles.promptDrawerBody}>
            <div className={styles.promptEditorSection}>
              <Form.Item
                label="提示词键"
                name="promptKey"
                rules={[{ required: true, message: '请选择提示词键' }]}
                className={styles.compactFormItem}
              >
                <AutoComplete
                  showSearch
                  options={keyOptions.map((item) => ({
                    label: item.promptKey,
                    value: item.promptKey,
                  }))}
                  placeholder="请选择提示词键"
                  filterOption={(inputValue, option) =>
                    String(option?.value || '')
                      .toLowerCase()
                      .includes(inputValue.toLowerCase())
                  }
                  onChange={(value) => {
                    setSelectedPromptKey(value || undefined);
                  }}
                  onSelect={(value) => {
                    void handlePromptKeyChange(value);
                  }}
                  onBlur={() => {
                    const rawPromptKey = form.getFieldValue('promptKey');
                    const normalizedPromptKey = String(rawPromptKey || '').trim();
                    if (!normalizedPromptKey || normalizedPromptKey === selectedPromptKey) {
                      return;
                    }
                    void handlePromptKeyChange(normalizedPromptKey);
                  }}
                />
              </Form.Item>

              <div className={styles.promptMetaWrap}>
                <Tag
                  color={selectedPromptMeta?.configured ? 'success' : 'default'}
                  bordered={false}
                >
                  {selectedPromptMeta?.configured ? '已配置' : '未配置'}
                </Tag>
                <Typography.Text type="secondary">
                  当前版本：
                  {currentPromptDetail?.promptVersion
                    ? `v${currentPromptDetail.promptVersion}`
                    : '-'}
                </Typography.Text>
                <Typography.Text type="secondary">
                  更新时间：{formatDateTime(currentPromptDetail?.updatedAt)}
                </Typography.Text>
                <Typography.Text type="secondary">
                  更新人：{currentPromptDetail?.updatedBy || '-'}
                </Typography.Text>
              </div>

              <Typography.Text type="secondary">
                用途说明：{selectedPromptMeta?.description || '暂无说明'}
              </Typography.Text>

              <Form.Item
                label="提示词正文"
                name="promptContent"
                rules={[{ required: true, message: '请输入提示词正文' }]}
                className={styles.compactFormItem}
              >
                <Input.TextArea
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  autoSize={{ minRows: 16, maxRows: 28 }}
                  placeholder="请输入提示词正文"
                />
              </Form.Item>

              <div className={styles.notesSection}>
                <InfoCircleOutlined />
                <div className={styles.notesContent}>
                  <div>
                    <strong>说明：</strong>
                  </div>
                  保存后会立即写入运行时 Redis，并通过 MQ 通知 Python 端刷新；队列消息超过 5
                  分钟会自动过期丢弃。
                </div>
              </div>
            </div>

            <div className={styles.promptHistorySection}>
              <div className={styles.promptHistoryHeader}>
                <Space>
                  <HistoryOutlined />
                  <Typography.Text strong>历史版本（最多保留30条）</Typography.Text>
                </Space>
              </div>
              {promptHistory.length === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无历史版本" />
              ) : (
                <List
                  className={styles.promptHistoryList}
                  dataSource={promptHistory}
                  renderItem={(item) => {
                    const version = item.promptVersion || 0;
                    const canRollback = Boolean(selectedPromptKey && version > 0);
                    return (
                      <List.Item
                        actions={[
                          <Popconfirm
                            key={`rollback-${version}`}
                            title={`确认回滚到 v${version} 吗？`}
                            description="回滚会基于该版本生成新的当前版本。"
                            okText="确认回滚"
                            cancelText="取消"
                            disabled={!canRollback}
                            onConfirm={() => {
                              if (!canRollback) {
                                return Promise.resolve();
                              }
                              return handleRollback(version);
                            }}
                          >
                            <Button
                              size="small"
                              loading={rollbackLoadingVersion === version}
                              disabled={!canRollback}
                            >
                              回滚
                            </Button>
                          </Popconfirm>,
                        ]}
                      >
                        <List.Item.Meta
                          title={
                            <Space>
                              <Tag bordered={false}>v{version}</Tag>
                              <Typography.Text type="secondary">
                                {formatDateTime(item.createdAt)}
                              </Typography.Text>
                              <Typography.Text type="secondary">
                                {item.createdBy || '-'}
                              </Typography.Text>
                            </Space>
                          }
                          description={
                            <Typography.Paragraph
                              ellipsis={{ rows: 3, expandable: true, symbol: '展开' }}
                              className={styles.promptHistoryContent}
                            >
                              {item.promptContent || '-'}
                            </Typography.Paragraph>
                          }
                        />
                      </List.Item>
                    );
                  }}
                />
              )}
            </div>
          </div>
        </Form>
      </Spin>
    </Drawer>
  );
};

export default PromptConfigDrawer;
