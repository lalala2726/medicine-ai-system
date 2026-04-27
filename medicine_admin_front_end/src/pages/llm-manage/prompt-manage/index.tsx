import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { PlusOutlined, ReloadOutlined, SyncOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, Form, Input, Modal, Tag, Typography, message } from 'antd';
import React, { useCallback, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import {
  deletePromptConfig,
  getPromptConfig,
  getPromptKeyOptions,
  savePromptKey,
  syncAllPromptConfigs,
  syncPromptConfig,
  type SystemModelTypes,
} from '@/api/llm-manage/systemModels';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import type { CaptchaVerificationResult } from '@/api/core/captcha';
import { useThemeContext } from '@/contexts/ThemeContext';
import SliderCaptchaModal from '@/pages/login/components/SliderCaptchaModal';
import { buildPromptManageEditPath, buildPromptManageHistoryPath } from '@/router/paths';
import PromptPreviewDrawer from './PromptPreviewDrawer';
import './index.less';

/**
 * 表格默认分页大小。
 */
const DEFAULT_PAGE_SIZE = 20;

/**
 * 表格分页大小选项。
 */
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

/**
 * 新增提示词弹窗宽度。
 */
const CREATE_PROMPT_MODAL_WIDTH = 560;

/**
 * 列表页提示词预览状态。
 */
interface PromptPreviewState {
  /**
   * 预览抽屉是否打开。
   */
  open: boolean;
  /**
   * 预览内容是否加载中。
   */
  loading: boolean;
  /**
   * 当前预览提示词业务键。
   */
  promptKey?: string;
  /**
   * 当前预览提示词版本号。
   */
  promptVersion?: number | null;
  /**
   * 当前预览提示词正文。
   */
  promptContent?: string;
}

/**
 * 新增提示词表单值。
 */
interface CreatePromptFormValues {
  /**
   * 提示词业务键。
   */
  promptKey: string;
  /**
   * 提示词用途说明。
   */
  description?: string;
}

/**
 * 列表页提示词预览初始状态。
 */
const INITIAL_PREVIEW_STATE: PromptPreviewState = {
  open: false,
  loading: false,
  promptKey: undefined,
  promptVersion: null,
  promptContent: '',
};

/**
 * 统一提取错误文案。
 *
 * @param error 原始异常对象。
 * @param fallback 默认错误提示。
 * @returns 可展示给用户的错误文案。
 */
function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

/**
 * 提示词管理列表页。
 */
const PromptManagePage: React.FC = () => {
  const actionRef = useRef<ActionType | null>(null);
  const navigate = useNavigate();
  const { isDark } = useThemeContext();
  const [createForm] = Form.useForm<CreatePromptFormValues>();
  const [previewState, setPreviewState] = useState<PromptPreviewState>(INITIAL_PREVIEW_STATE);
  const [deletingPromptKey, setDeletingPromptKey] = useState<string>();
  const [createPromptModalOpen, setCreatePromptModalOpen] = useState(false);
  const [creatingPromptKey, setCreatingPromptKey] = useState(false);
  const [pendingDeletePromptKey, setPendingDeletePromptKey] = useState<string>();
  const [deleteCaptchaOpen, setDeleteCaptchaOpen] = useState(false);
  const [syncingAllPrompts, setSyncingAllPrompts] = useState(false);
  const [syncingPromptKey, setSyncingPromptKey] = useState<string>();

  /**
   * 刷新提示词表格。
   */
  const reloadTable = useCallback(() => {
    actionRef.current?.reload();
  }, []);

  /**
   * 打开提示词预览抽屉并加载当前内容。
   *
   * @param promptKey 提示词业务键。
   */
  const openPreview = useCallback(async (promptKey: string) => {
    setPreviewState({
      open: true,
      loading: true,
      promptKey,
      promptVersion: null,
      promptContent: '',
    });

    try {
      const detail = await getPromptConfig(promptKey);
      setPreviewState({
        open: true,
        loading: false,
        promptKey,
        promptVersion: detail?.promptVersion ?? null,
        promptContent: detail?.promptContent || '',
      });
    } catch (error) {
      message.error(getErrorMessage(error, '提示词预览加载失败'));
      setPreviewState({
        open: true,
        loading: false,
        promptKey,
        promptVersion: null,
        promptContent: '',
      });
    }
  }, []);

  /**
   * 关闭提示词预览抽屉。
   */
  const closePreview = useCallback(() => {
    setPreviewState({
      ...INITIAL_PREVIEW_STATE,
      open: false,
    });
  }, []);

  /**
   * 打开新增提示词弹窗。
   */
  const openCreatePromptModal = useCallback(() => {
    createForm.resetFields();
    setCreatePromptModalOpen(true);
  }, [createForm]);

  /**
   * 关闭新增提示词弹窗。
   */
  const closeCreatePromptModal = useCallback(() => {
    setCreatePromptModalOpen(false);
    createForm.resetFields();
  }, [createForm]);

  /**
   * 新增提示词业务键。
   */
  const handleCreatePromptKey = useCallback(async () => {
    const values = await createForm.validateFields();
    setCreatingPromptKey(true);
    try {
      const normalizedPromptKey = values.promptKey.trim();
      const normalizedDescription = values.description?.trim() || undefined;
      await savePromptKey({
        promptKey: normalizedPromptKey,
        description: normalizedDescription,
      });
      message.success('提示词新增成功');
      closeCreatePromptModal();
      reloadTable();
    } catch (error) {
      message.error(getErrorMessage(error, '提示词新增失败'));
    } finally {
      setCreatingPromptKey(false);
    }
  }, [closeCreatePromptModal, createForm, reloadTable]);

  /**
   * 打开删除验证码弹层。
   *
   * @param promptKey 提示词业务键。
   */
  const openDeleteCaptcha = useCallback((promptKey: string) => {
    if (!promptKey) {
      return;
    }
    setPendingDeletePromptKey(promptKey);
    setDeleteCaptchaOpen(true);
  }, []);

  /**
   * 打开删除提示词警告弹窗。
   *
   * @param promptKey 提示词业务键。
   */
  const openDeleteWarningModal = useCallback(
    (promptKey: string) => {
      if (!promptKey) {
        return;
      }
      Modal.confirm({
        title: '危险操作确认',
        content: '删除后会同时删除该提示词的全部历史版本，且不可恢复。是否继续？',
        centered: true,
        okText: '继续删除',
        okButtonProps: { danger: true },
        cancelText: '取消',
        onOk: () => {
          openDeleteCaptcha(promptKey);
        },
      });
    },
    [openDeleteCaptcha],
  );

  /**
   * 取消删除验证码校验。
   */
  const handleDeleteCaptchaCancel = useCallback(() => {
    setDeleteCaptchaOpen(false);
    setPendingDeletePromptKey(undefined);
  }, []);

  /**
   * 删除滑块验证码通过后执行真正删除。
   *
   * @param captchaVerificationResult 验证码校验结果。
   */
  const handleDeleteCaptchaVerified = useCallback(
    async (captchaVerificationResult: CaptchaVerificationResult) => {
      const promptKey = pendingDeletePromptKey;
      if (!promptKey) {
        setDeleteCaptchaOpen(false);
        return;
      }
      setDeleteCaptchaOpen(false);
      setPendingDeletePromptKey(undefined);
      setDeletingPromptKey(promptKey);
      try {
        await deletePromptConfig({
          promptKey,
          captchaVerificationId: captchaVerificationResult.id,
        });
        message.success('提示词已删除，历史版本已一并删除');
        if (previewState.open && previewState.promptKey === promptKey) {
          closePreview();
        }
        reloadTable();
      } catch (error) {
        message.error(getErrorMessage(error, '删除提示词失败'));
      } finally {
        setDeletingPromptKey(undefined);
      }
    },
    [closePreview, pendingDeletePromptKey, previewState.open, previewState.promptKey, reloadTable],
  );

  /**
   * 提交全量提示词同步任务。
   */
  const handleSyncAllPrompts = useCallback(async () => {
    setSyncingAllPrompts(true);
    try {
      await syncAllPromptConfigs();
      message.success('全量同步任务已提交，请稍后手动刷新查看状态');
    } catch (error) {
      message.error(getErrorMessage(error, '提交全量同步任务失败'));
    } finally {
      setSyncingAllPrompts(false);
    }
  }, []);

  /**
   * 提交单条提示词同步任务。
   *
   * @param promptKey 提示词业务键。
   */
  const handleSyncPrompt = useCallback(async (promptKey: string) => {
    if (!promptKey) {
      return;
    }
    setSyncingPromptKey(promptKey);
    try {
      await syncPromptConfig({ promptKey });
      message.success('单条同步任务已提交，请稍后手动刷新查看状态');
    } catch (error) {
      message.error(getErrorMessage(error, '提交单条同步任务失败'));
    } finally {
      setSyncingPromptKey(undefined);
    }
  }, []);

  /**
   * 表格列定义。
   */
  const columns: ProColumns<SystemModelTypes.PromptKeyOption>[] = [
    {
      title: '提示词键',
      dataIndex: 'promptKey',
      render: (_, record) => (
        <Typography.Text copyable={{ text: record.promptKey }}>{record.promptKey}</Typography.Text>
      ),
    },
    {
      title: '用途说明',
      dataIndex: 'description',
      ellipsis: true,
      render: (_, record) => record.description || '-',
    },
    {
      title: '状态',
      dataIndex: 'configured',
      align: 'center',
      render: (_, record) =>
        record.configured ? (
          <Tag color="success" bordered={false}>
            已配置
          </Tag>
        ) : (
          <Tag bordered={false}>未配置</Tag>
        ),
    },
    {
      title: '操作',
      dataIndex: 'option',
      valueType: 'option',
      width: 400,
      fixed: 'right',
      align: 'center',
      render: (_, record) => (
        <TableActionGroup>
          <PermissionButton
            type="link"
            access={ADMIN_PERMISSIONS.agentPrompt.query}
            onClick={() => {
              void openPreview(record.promptKey);
            }}
          >
            预览
          </PermissionButton>
          <PermissionButton
            type="link"
            access={ADMIN_PERMISSIONS.agentPrompt.update}
            onClick={() => navigate(buildPromptManageEditPath(record.promptKey))}
          >
            编辑
          </PermissionButton>
          <PermissionButton
            type="link"
            access={ADMIN_PERMISSIONS.agentPrompt.query}
            onClick={() => navigate(buildPromptManageHistoryPath(record.promptKey))}
          >
            查看历史
          </PermissionButton>
          <PermissionButton
            type="link"
            loading={syncingPromptKey === record.promptKey}
            access={ADMIN_PERMISSIONS.agentPrompt.sync}
            onClick={() => {
              void handleSyncPrompt(record.promptKey);
            }}
          >
            同步
          </PermissionButton>
          <PermissionButton
            type="link"
            danger
            loading={deletingPromptKey === record.promptKey}
            access={ADMIN_PERMISSIONS.agentPrompt.delete}
            onClick={() => openDeleteWarningModal(record.promptKey)}
          >
            删除
          </PermissionButton>
        </TableActionGroup>
      ),
    },
  ];

  return (
    <PageContainer>
      <ProTable<SystemModelTypes.PromptKeyOption>
        headerTitle="提示词管理"
        actionRef={actionRef}
        rowKey="promptKey"
        search={false}
        columns={columns}
        request={async () => {
          try {
            const data = (await getPromptKeyOptions()) || [];
            return {
              data,
              success: true,
              total: data.length,
            };
          } catch (error) {
            message.error(getErrorMessage(error, '提示词列表加载失败'));
            return {
              data: [],
              success: false,
              total: 0,
            };
          }
        }}
        toolBarRender={() => [
          <PermissionButton
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            access={ADMIN_PERMISSIONS.agentPrompt.update}
            onClick={openCreatePromptModal}
          >
            新增提示词
          </PermissionButton>,
          <PermissionButton
            key="sync-all"
            icon={<SyncOutlined />}
            loading={syncingAllPrompts}
            access={ADMIN_PERMISSIONS.agentPrompt.sync}
            onClick={() => {
              void handleSyncAllPrompts();
            }}
          >
            全量同步
          </PermissionButton>,
          <Button key="refresh" icon={<ReloadOutlined />} onClick={reloadTable}>
            刷新
          </Button>,
        ]}
        pagination={{
          showSizeChanger: true,
          defaultPageSize: DEFAULT_PAGE_SIZE,
          pageSizeOptions: PAGE_SIZE_OPTIONS,
        }}
      />

      <PromptPreviewDrawer
        open={previewState.open}
        loading={previewState.loading}
        promptKey={previewState.promptKey}
        promptVersion={previewState.promptVersion}
        promptContent={previewState.promptContent}
        onClose={closePreview}
      />

      <Modal
        title="新增提示词"
        open={createPromptModalOpen}
        width={CREATE_PROMPT_MODAL_WIDTH}
        centered
        maskClosable={false}
        destroyOnClose
        onCancel={closeCreatePromptModal}
        onOk={() => void handleCreatePromptKey()}
        okText="确认新增"
        cancelText="取消"
        okButtonProps={{ loading: creatingPromptKey }}
      >
        <Form form={createForm} layout="vertical" preserve={false}>
          <Form.Item
            label="提示词键"
            name="promptKey"
            rules={[
              { required: true, message: '请输入提示词键' },
              { max: 128, message: '提示词键长度不能超过128个字符' },
              {
                pattern: /^[a-z][a-z0-9_]*$/,
                message: '提示词键仅支持小写字母、数字、下划线，且需以字母开头',
              },
            ]}
          >
            <Input autoComplete={TEXT_INPUT_AUTOCOMPLETE} placeholder="示例：client_order_prompt" />
          </Form.Item>
          <Form.Item
            label="用途说明"
            name="description"
            rules={[{ max: 512, message: '用途说明长度不能超过512个字符' }]}
          >
            <Input.TextArea
              autoComplete={TEXT_INPUT_AUTOCOMPLETE}
              placeholder="请输入该提示词的用途说明（可选）"
              autoSize={{ minRows: 3, maxRows: 6 }}
              showCount
              maxLength={512}
            />
          </Form.Item>
        </Form>
      </Modal>

      <SliderCaptchaModal
        open={deleteCaptchaOpen}
        onCancel={handleDeleteCaptchaCancel}
        onVerified={handleDeleteCaptchaVerified}
        isDark={isDark}
      />
    </PageContainer>
  );
};

export default PromptManagePage;
