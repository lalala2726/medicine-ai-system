import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { PlusOutlined } from '@ant-design/icons';
import {
  Button,
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
import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
  useImperativeHandle,
  forwardRef,
} from 'react';

import {
  getAdminAssistantConfig,
  getChatModelOptions,
  saveAdminAssistantConfig,
  type SystemModelTypes,
} from '@/api/llm-manage/systemModels';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';

import styles from '../index.module.less';
import { getSystemModelsErrorMessage, type ConfigSectionRef } from '../shared';

/**
 * 管理端助手配置表单值。
 */
interface AdminAssistantFormValues {
  /** 管理端聊天展示模型列表。 */
  chatDisplayModels?: SystemModelTypes.AdminAssistantChatDisplayModel[];
}

/**
 * 展示模型编辑行属性。
 */
interface DisplayModelEditorRowProps {
  /** 字段对象。 */
  field: { name: number; key: React.Key; isListField?: boolean };
  /** 聊天模型选项列表。 */
  chatOptions: SystemModelTypes.ModelOption[];
  /** 删除当前行回调。 */
  onRemove: () => void;
}

/**
 * 展示模型预览项。
 */
interface DisplayModelPreviewItem extends SystemModelTypes.AdminAssistantChatDisplayModel {
  /** 是否支持深度思考。 */
  supportReasoning?: boolean;
  /** 是否支持图片理解。 */
  supportVision?: boolean;
}

/**
 * 判断异常是否为表单校验异常。
 *
 * @param error 捕获到的异常对象。
 * @returns 是否为表单校验异常。
 */
function isFormValidationError(error: unknown): error is { errorFields?: unknown[] } {
  return Boolean((error as { errorFields?: unknown[] } | null)?.errorFields);
}

/**
 * 单条展示模型编辑行。
 *
 * @param props 组件属性。
 * @returns 展示模型编辑行节点。
 */
const DisplayModelEditorRow: React.FC<DisplayModelEditorRowProps> = ({
  field,
  chatOptions,
  onRemove,
}) => {
  const form = Form.useFormInstance<AdminAssistantFormValues>();
  const actualModelName = Form.useWatch(
    ['chatDisplayModels', field.name, 'actualModelName'],
    form,
  ) as string | undefined;

  const customModelName = Form.useWatch(
    ['chatDisplayModels', field.name, 'customModelName'],
    form,
  ) as string | undefined;

  const selectedOption = useMemo(
    () => chatOptions.find((item) => item.value === actualModelName),
    [actualModelName, chatOptions],
  );

  return (
    <div key={field.key} className={styles.displayModelEditorItem}>
      <div className={styles.displayModelEditorItemHeader}>
        <div className={styles.displayModelEditorItemTitle}>
          {customModelName ? `模型 - ${customModelName}` : `显示模型 #${field.name + 1}`}
        </div>
        <PermissionButton
          danger
          type="text"
          access={ADMIN_PERMISSIONS.agentConfig.adminUpdate}
          onClick={onRemove}
        >
          移除
        </PermissionButton>
      </div>

      <div className={styles.displayModelEditorFieldRow}>
        <div className={styles.displayModelEditorFieldLabel}>自定义展示名称</div>
        <div className={styles.displayModelEditorFieldControl}>
          <Form.Item
            {...field}
            name={[field.name, 'customModelName']}
            rules={[{ required: true, message: '必填' }]}
            style={{ marginBottom: 0 }}
          >
            <Input
              autoComplete={TEXT_INPUT_AUTOCOMPLETE}
              placeholder="输入用户界面的展示名称，例：智能助手"
            />
          </Form.Item>
        </div>
      </div>

      <div className={styles.displayModelEditorFieldRow}>
        <div className={styles.displayModelEditorFieldLabel}>底座大模型</div>
        <div className={styles.displayModelEditorFieldControl}>
          <Form.Item
            {...field}
            name={[field.name, 'actualModelName']}
            rules={[{ required: true, message: '必选' }]}
            style={{ marginBottom: 0 }}
          >
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="请选择"
              options={chatOptions}
              notFoundContent={null}
            />
          </Form.Item>
        </div>
      </div>

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

      <div className={styles.displayModelEditorFieldRow}>
        <div className={styles.displayModelEditorFieldLabel}>简介副标题（可选）</div>
        <div className={styles.displayModelEditorFieldControl}>
          <Form.Item {...field} name={[field.name, 'description']} style={{ marginBottom: 0 }}>
            <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="简单一句介绍" />
          </Form.Item>
        </div>
      </div>
    </div>
  );
};

/**
 * 管理端助手配置区块。
 *
 * @returns 管理端助手配置区块节点。
 */
const AdminAssistantConfigSection = forwardRef<ConfigSectionRef>((props, ref) => {
  const [form] = Form.useForm<AdminAssistantFormValues>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [chatOptions, setChatOptions] = useState<SystemModelTypes.ModelOption[]>([]);
  const [displayModelModalOpen, setDisplayModelModalOpen] = useState(false);
  const [previewDisplayModels, setPreviewDisplayModels] = useState<
    SystemModelTypes.AdminAssistantChatDisplayModel[]
  >([]);

  /**
   * 加载管理端助手配置与聊天模型选项。
   *
   * @returns 无返回值。
   */
  const loadSection = useCallback(async () => {
    setLoading(true);

    try {
      const [nextDetail, nextChatOptions] = await Promise.all([
        getAdminAssistantConfig(),
        getChatModelOptions(),
      ]);
      const resolvedDetail = nextDetail || {};
      const resolvedChatOptions = nextChatOptions || [];
      const nextDisplayModels = resolvedDetail?.chatDisplayModels || [];

      setChatOptions(resolvedChatOptions);
      setPreviewDisplayModels(nextDisplayModels);
      form.setFieldsValue({
        chatDisplayModels: nextDisplayModels,
      });
    } catch (error) {
      message.error(getSystemModelsErrorMessage(error, '管理端助手配置加载失败'));
    } finally {
      setLoading(false);
    }
  }, [form]);

  useEffect(() => {
    void loadSection();
  }, [loadSection]);

  /**
   * 当前展示模型预览列表。
   */
  const displayModelPreviewList = useMemo<DisplayModelPreviewItem[]>(
    () =>
      previewDisplayModels.map((item) => {
        const matchedOption = chatOptions.find((option) => option.value === item.actualModelName);

        return {
          ...item,
          supportReasoning: item.supportReasoning ?? matchedOption?.supportReasoning,
          supportVision: item.supportVision ?? matchedOption?.supportVision,
        };
      }),
    [chatOptions, previewDisplayModels],
  );

  /**
   * 保存管理端助手配置。
   *
   * @returns 无返回值。
   */
  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();
      const nextDisplayModels = (values.chatDisplayModels || []).map((item) => ({
        customModelName: item.customModelName,
        actualModelName: item.actualModelName,
        description: item.description,
      }));

      setSaving(true);
      await saveAdminAssistantConfig({
        chatDisplayModels: nextDisplayModels,
      });
      setPreviewDisplayModels(nextDisplayModels);
      setDisplayModelModalOpen(false);
    } catch (error) {
      if (isFormValidationError(error)) {
        throw error;
      }
      message.error(getSystemModelsErrorMessage(error, '管理端助手配置保存失败'));
      throw error;
    } finally {
      setSaving(false);
    }
  }, [form]);

  /**
   * 仅关闭展示模型抽屉，不触发接口保存。
   *
   * @returns 无返回值。
   */
  const handleDisplayModelModalClose = useCallback(() => {
    Modal.confirm({
      title: '确定要退出吗？',
      content: '当前抽屉内修改尚未保存，退出后本次编辑内容将不会生效。',
      onOk: () => setDisplayModelModalOpen(false),
      okText: '确定',
      cancelText: '取消',
    });
  }, []);

  /**
   * 在展示模型抽屉内直接保存配置并关闭，避免刷新后配置丢失。
   *
   * @returns 无返回值。
   */
  const handleSaveDisplayModels = useCallback(async () => {
    try {
      await handleSave();
      message.success('聊天界面展示模型配置已保存');
    } catch (error) {
      if (!isFormValidationError(error)) {
        // 其余错误文案已在 handleSave 内提示。
      }
    }
  }, [handleSave]);

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
          <div className={styles.sectionCardTitle}>管理端助手</div>
          <Typography.Text type="secondary" className={styles.sectionCardDescription}>
            配置后台智能助手在聊天界面展示的模型名称与真实模型映射。
          </Typography.Text>
        </div>
      </div>

      <Spin spinning={loading}>
        <Form autoComplete={TEXT_INPUT_AUTOCOMPLETE} form={form} layout="vertical">
          <div className={styles.drawerBody}>
            <div className={styles.slotStack}>
              <div className={styles.slotCard}>
                <div className={styles.slotHeader}>
                  <div>
                    <div className={styles.slotTitle}>聊天界面展示模型配置</div>
                    <Typography.Text type="secondary" className={styles.slotDescription}>
                      管理端聊天界面由用户在右上角选择模型。这里维护展示模型名称与真实模型名称的映射关系。
                    </Typography.Text>
                  </div>
                  <PermissionButton
                    type="primary"
                    icon={<PlusOutlined />}
                    access={ADMIN_PERMISSIONS.agentConfig.adminUpdate}
                    onClick={() => setDisplayModelModalOpen(true)}
                  >
                    配置展示模型
                  </PermissionButton>
                </div>

                {displayModelPreviewList.length > 0 ? (
                  <div className={styles.displayModelPreviewList}>
                    {displayModelPreviewList.map((item) => (
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
              </div>
            </div>
          </div>

          <Drawer
            title="聊天界面展示模型配置"
            open={displayModelModalOpen}
            width={860}
            destroyOnClose={false}
            forceRender
            onClose={handleDisplayModelModalClose}
            footer={
              <div className={styles.drawerFooter}>
                <Space>
                  <Button onClick={handleDisplayModelModalClose}>取消</Button>
                  <PermissionButton
                    type="primary"
                    loading={saving}
                    access={ADMIN_PERMISSIONS.agentConfig.adminUpdate}
                    onClick={() => void handleSaveDisplayModels()}
                  >
                    保存
                  </PermissionButton>
                </Space>
              </div>
            }
          >
            <Form.List name="chatDisplayModels">
              {(fields, { add, remove }) => (
                <div className={styles.displayModelModalBody}>
                  <div className={styles.displayModelModalToolbar}>
                    <Typography.Text type="secondary">
                      支持维护多个前端展示模型，每个条目会映射到一个真实聊天模型。
                    </Typography.Text>
                    <PermissionButton
                      type="dashed"
                      icon={<PlusOutlined />}
                      access={ADMIN_PERMISSIONS.agentConfig.adminUpdate}
                      onClick={() =>
                        add({
                          customModelName: '',
                          actualModelName: '',
                          description: '',
                        })
                      }
                    >
                      新增展示模型
                    </PermissionButton>
                  </div>

                  {fields.length > 0 ? (
                    <div className={styles.displayModelEditorList}>
                      {fields.map((field) => (
                        <DisplayModelEditorRow
                          key={field.key}
                          field={field}
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
          </Drawer>
        </Form>
      </Spin>
    </div>
  );
});

export default AdminAssistantConfigSection;
