import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { DeleteOutlined, InfoCircleOutlined, PlusOutlined } from '@ant-design/icons';
import {
  Button,
  Card,
  Drawer,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from 'antd';
import React, { useCallback, useEffect, useMemo, useState } from 'react';

import {
  getChatModelOptions,
  saveAdminAssistantConfig,
  type SystemModelTypes,
} from '@/api/llm-manage/systemModels';
import { useThemeContext } from '@/contexts/ThemeContext';

import styles from '../index.module.less';

const { TextArea } = Input;

interface AdminAssistantConfigDrawerProps {
  open: boolean;
  detail?: SystemModelTypes.AdminAssistantSystemModelConfig;
  onClose: () => void;
  onSaved: () => Promise<void> | void;
}

interface DisplayModelEditorRowProps {
  fieldName: number;
  fieldKey: React.Key;
  chatOptions: SystemModelTypes.ModelOption[];
  onRemove: () => void;
}

/**
 * 统一提取错误文案。
 *
 * @param error 原始异常对象
 * @param fallback 默认提示文案
 * @returns 可直接展示给用户的错误文案
 */
function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

/**
 * 单条聊天展示模型编辑行。
 *
 * @param fieldName 当前表单项索引
 * @param fieldKey 当前表单项唯一键
 * @param chatOptions 可选真实聊天模型列表
 * @param onRemove 删除当前行回调
 * @returns 单条编辑行 JSX
 */
const DisplayModelEditorRow: React.FC<DisplayModelEditorRowProps> = ({
  fieldName,
  fieldKey,
  chatOptions,
  onRemove,
}) => {
  const form = Form.useFormInstance();
  const actualModelName = Form.useWatch(
    ['chatDisplayModels', fieldName, 'actualModelName'],
    form,
  ) as string | undefined;
  const selectedOption = useMemo(
    () => chatOptions.find((item) => item.value === actualModelName),
    [actualModelName, chatOptions],
  );

  return (
    <Card key={fieldKey} size="small" className={styles.displayModelEditorCard}>
      <div className={styles.displayModelEditorHeader}>
        <div className={styles.displayModelEditorTitle}>展示模型 #{fieldName + 1}</div>
        <Button danger type="text" icon={<DeleteOutlined />} onClick={onRemove}>
          删除
        </Button>
      </div>

      <Form.Item
        label="前端展示模型名称"
        name={['chatDisplayModels', fieldName, 'customModelName']}
        rules={[{ required: true, message: '请输入前端展示模型名称' }]}
        className={styles.compactFormItem}
      >
        <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="例如：运营分析专用" />
      </Form.Item>

      <Form.Item
        label="真实模型名称"
        name={['chatDisplayModels', fieldName, 'actualModelName']}
        rules={[{ required: true, message: '请选择真实模型名称' }]}
        className={styles.compactFormItem}
      >
        <Select
          showSearch
          optionFilterProp="label"
          placeholder="请选择真实聊天模型"
          options={chatOptions}
          notFoundContent={null}
        />
      </Form.Item>

      {selectedOption?.supportReasoning || selectedOption?.supportVision ? (
        <div className={styles.capabilityRow}>
          {selectedOption?.supportReasoning ? (
            <Tag color="geekblue" bordered={false}>
              支持深度思考
            </Tag>
          ) : null}
          {selectedOption?.supportVision ? (
            <Tag color="cyan" bordered={false}>
              支持图片理解
            </Tag>
          ) : null}
        </div>
      ) : null}

      <Form.Item
        label="模型文案"
        name={['chatDisplayModels', fieldName, 'description']}
        className={styles.compactFormItem}
      >
        <TextArea rows={2} placeholder="前端聊天界面展示文案" />
      </Form.Item>
    </Card>
  );
};

/**
 * 管理端助手系统模型配置抽屉。
 *
 * @param open 抽屉打开状态
 * @param detail 当前配置详情
 * @param onClose 关闭回调
 * @param onSaved 保存成功回调
 * @returns 抽屉 JSX
 */
const AdminAssistantConfigDrawer: React.FC<AdminAssistantConfigDrawerProps> = ({
  open,
  detail,
  onClose,
  onSaved,
}) => {
  const { isDark } = useThemeContext();
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [chatOptions, setChatOptions] = useState<SystemModelTypes.ModelOption[]>([]);
  const [displayModelModalOpen, setDisplayModelModalOpen] = useState(false);

  const displayModels = Form.useWatch('chatDisplayModels', form) as
    | SystemModelTypes.AdminAssistantChatDisplayModel[]
    | undefined;

  /**
   * 加载聊天模型选项与展示模型映射。
   */
  const loadOptions = useCallback(async () => {
    setOptionsLoading(true);
    try {
      const nextChatOptions = (await getChatModelOptions()) || [];

      setChatOptions(nextChatOptions);
      form.setFieldsValue({
        chatDisplayModels: detail?.chatDisplayModels || [],
      });
    } finally {
      setOptionsLoading(false);
    }
  }, [detail?.chatDisplayModels, form]);

  useEffect(() => {
    if (!open) {
      form.resetFields();
      setDisplayModelModalOpen(false);
      return;
    }

    form.setFieldsValue({
      chatDisplayModels: detail?.chatDisplayModels || [],
    });
    void loadOptions();
  }, [detail, form, loadOptions, open]);

  /**
   * 保存管理端助手配置。
   */
  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();
      const nextDisplayModels = (values.chatDisplayModels || []).map(
        (item: SystemModelTypes.AdminAssistantChatDisplayModel) => ({
          customModelName: item.customModelName,
          actualModelName: item.actualModelName,
          description: item.description,
        }),
      );

      setSaving(true);
      await saveAdminAssistantConfig({
        chatDisplayModels: nextDisplayModels,
      });
      message.success('管理端助手配置已保存');
      await onSaved();
      onClose();
    } catch (error) {
      if ((error as { errorFields?: unknown[] } | null)?.errorFields) {
        return;
      }
      message.error(getErrorMessage(error, '管理端助手配置保存失败'));
    } finally {
      setSaving(false);
    }
  }, [form, onClose, onSaved]);

  return (
    <Drawer
      title="配置管理端助手"
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
      <Spin spinning={optionsLoading && chatOptions.length === 0}>
        <Form form={form} layout="vertical">
          <div className={styles.drawerBody}>
            <Card className={styles.slotCard} size="small">
              <div className={styles.slotHeader}>
                <div>
                  <div className={styles.slotTitle}>聊天界面展示模型配置</div>
                  <Typography.Text type="secondary" className={styles.slotDescription}>
                    这里维护前端聊天界面可选模型列表。前端展示和提交使用自定义模型名称，后端会映射到真实模型名称。
                  </Typography.Text>
                </div>
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => setDisplayModelModalOpen(true)}
                >
                  配置展示模型
                </Button>
              </div>

              {displayModels && displayModels.length > 0 ? (
                <div className={styles.displayModelPreviewList}>
                  {displayModels.map((item) => (
                    <div
                      key={`${item.customModelName}-${item.actualModelName}`}
                      className={styles.displayModelPreviewItem}
                    >
                      <div className={styles.displayModelPreviewHead}>
                        <span className={styles.displayModelPreviewName}>
                          {item.customModelName}
                        </span>
                        <span className={styles.displayModelPreviewMeta}>
                          {item.actualModelName}
                        </span>
                      </div>
                      {item.description ? (
                        <Typography.Text type="secondary" className={styles.slotDescription}>
                          {item.description}
                        </Typography.Text>
                      ) : null}
                      {item.supportReasoning || item.supportVision ? (
                        <div className={styles.capabilityRow}>
                          {item.supportReasoning ? (
                            <Tag color="geekblue" bordered={false}>
                              支持深度思考
                            </Tag>
                          ) : null}
                          {item.supportVision ? (
                            <Tag color="cyan" bordered={false}>
                              支持图片理解
                            </Tag>
                          ) : null}
                        </div>
                      ) : null}
                    </div>
                  ))}
                </div>
              ) : (
                <Typography.Text type="secondary" className={styles.slotDescription}>
                  暂未配置前端聊天展示模型。聊天页将不允许发送消息，请先完成这里的展示模型配置。
                </Typography.Text>
              )}
            </Card>

            <div className={styles.notesSection}>
              <InfoCircleOutlined />
              <div className={styles.notesContent}>
                <div>
                  <strong>配置说明：</strong>
                </div>
                这里维护的是管理端智能助手聊天界面的模型映射列表。用户会在聊天页右上角主动选择模型，
                提交时前端发送自定义模型名称，AI 端再映射为真实模型名称。
              </div>
            </div>
          </div>

          <Modal
            title="配置聊天界面展示模型"
            open={displayModelModalOpen}
            width={860}
            destroyOnClose={false}
            onCancel={() => setDisplayModelModalOpen(false)}
            onOk={() => setDisplayModelModalOpen(false)}
            okText="完成"
            cancelText="取消"
          >
            <Form.List name="chatDisplayModels">
              {(fields, { add, remove }) => (
                <div className={styles.displayModelModalBody}>
                  <div className={styles.displayModelModalToolbar}>
                    <Typography.Text type="secondary">
                      支持维护多个前端展示模型，每个条目会映射到一个真实聊天模型。
                    </Typography.Text>
                    <Button
                      type="dashed"
                      icon={<PlusOutlined />}
                      onClick={() =>
                        add({ customModelName: '', actualModelName: '', description: '' })
                      }
                    >
                      新增展示模型
                    </Button>
                  </div>

                  {fields.length > 0 ? (
                    <div className={styles.displayModelEditorList}>
                      {fields.map((field) => (
                        <DisplayModelEditorRow
                          key={field.key}
                          fieldName={field.name}
                          fieldKey={field.key}
                          chatOptions={chatOptions}
                          onRemove={() => remove(field.name)}
                        />
                      ))}
                    </div>
                  ) : (
                    <div className={styles.displayModelEmptyState}>
                      暂无展示模型，点击“新增展示模型”开始配置。
                    </div>
                  )}
                </div>
              )}
            </Form.List>
          </Modal>
        </Form>
      </Spin>
    </Drawer>
  );
};

export default AdminAssistantConfigDrawer;
