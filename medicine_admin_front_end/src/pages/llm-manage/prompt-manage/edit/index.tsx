import { HistoryOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import { Button, Empty, Form, Spin, message } from 'antd';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { getPromptConfig, savePromptConfig } from '@/api/llm-manage/systemModels';
import { PermissionButton, RichTextEditor } from '@/components';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { buildPromptManageHistoryPath, routePaths } from '@/router/paths';
import './index.less';

/**
 * 提示词编辑表单值。
 */
interface PromptEditFormValues {
  /**
   * 提示词正文（Markdown）。
   */
  promptContent?: string;
}

/**
 * 富文本编辑器高度（贴近页面底部）。
 */
const PROMPT_RICH_EDITOR_HEIGHT = 'calc(100vh - 290px)';

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
 * 提示词编辑页。
 */
const PromptEditPage: React.FC = () => {
  const [form] = Form.useForm<PromptEditFormValues>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const promptKey = useMemo(
    () => String(searchParams.get('promptKey') || '').trim(),
    [searchParams],
  );

  /**
   * 返回提示词管理列表页。
   */
  const goBackToManagePage = useCallback(() => {
    navigate(routePaths.llmPromptManage);
  }, [navigate]);

  /**
   * 加载当前提示词详情。
   */
  const loadPromptDetail = useCallback(async () => {
    if (!promptKey) {
      form.setFieldsValue({
        promptContent: '',
      });
      return;
    }

    setLoading(true);
    try {
      let promptContent = '';
      try {
        const detail = await getPromptConfig(promptKey);
        promptContent = detail?.promptContent || '';
      } catch {
        promptContent = '';
      }

      form.setFieldsValue({
        promptContent,
      });
    } catch (error) {
      message.error(getErrorMessage(error, '提示词详情加载失败'));
    } finally {
      setLoading(false);
    }
  }, [form, promptKey]);

  useEffect(() => {
    void loadPromptDetail();
  }, [loadPromptDetail]);

  /**
   * 保存当前提示词。
   */
  const handleSave = useCallback(async () => {
    if (!promptKey) {
      message.warning('缺少 promptKey，无法保存');
      return;
    }

    try {
      const values = await form.validateFields(['promptContent']);
      setSaving(true);
      await savePromptConfig({
        promptKey,
        promptContent: values.promptContent || '',
      });
      message.success('提示词保存成功');
      await loadPromptDetail();
    } catch (error) {
      if ((error as { errorFields?: unknown[] } | null)?.errorFields) {
        return;
      }
      message.error(getErrorMessage(error, '提示词保存失败'));
    } finally {
      setSaving(false);
    }
  }, [form, loadPromptDetail, promptKey]);

  if (!promptKey) {
    return (
      <PageContainer
        className="prompt-manage-edit-page"
        title="提示词编辑"
        onBack={goBackToManagePage}
      >
        <Empty
          description="未传入 promptKey，请从提示词管理列表点击“编辑”进入。"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        >
          <Button type="primary" onClick={goBackToManagePage}>
            返回提示词管理
          </Button>
        </Empty>
      </PageContainer>
    );
  }

  return (
    <PageContainer
      className="prompt-manage-edit-page"
      title="提示词编辑"
      onBack={goBackToManagePage}
      header={{
        extra: [
          <PermissionButton
            key="history"
            icon={<HistoryOutlined />}
            access={ADMIN_PERMISSIONS.agentPrompt.query}
            onClick={() => navigate(buildPromptManageHistoryPath(promptKey))}
          >
            查看历史
          </PermissionButton>,
          <Button key="reload" icon={<ReloadOutlined />} onClick={() => void loadPromptDetail()}>
            重新加载
          </Button>,
          <PermissionButton
            key="save"
            type="primary"
            icon={<SaveOutlined />}
            loading={saving}
            access={ADMIN_PERMISSIONS.agentPrompt.update}
            onClick={() => void handleSave()}
          >
            保存
          </PermissionButton>,
        ],
      }}
    >
      <Spin spinning={loading}>
        <Form form={form} layout="vertical">
          <Form.Item label="提示词正文" name="promptContent" style={{ marginBottom: 12 }}>
            <RichTextEditor
              contentFormat="markdown"
              height={PROMPT_RICH_EDITOR_HEIGHT}
              placeholder="请输入正文"
            />
          </Form.Item>
        </Form>
      </Spin>
    </PageContainer>
  );
};

export default PromptEditPage;
