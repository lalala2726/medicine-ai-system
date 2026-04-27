import { PageContainer } from '@ant-design/pro-components';
import React, { useRef, useState } from 'react';
import { InfoCircleOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons';
import { Button, message } from 'antd';

import {
  getKnowledgeBaseConfig,
  getKnowledgeBaseOptions,
  saveKnowledgeBaseConfig,
} from '@/api/llm-manage/systemModels';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';

import KnowledgeBaseConfigSection from '../sections/KnowledgeBaseConfigSection';
import AdminAssistantConfigSection from '../sections/AdminAssistantConfigSection';
import {
  SYSTEM_MODELS_PAGE_TITLE,
  useSystemModelsSecondaryMenu,
  type ConfigSectionRef,
} from '../shared';
import styles from '../index.module.less';
import './index.less';

/**
 * 管理端配置页面。
 *
 * @returns 管理端配置页面节点。
 */
const SystemModelsAdminConfigPage: React.FC = () => {
  useSystemModelsSecondaryMenu();
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(false);

  const knowledgeBaseRef = useRef<ConfigSectionRef>(null);
  const adminAssistantRef = useRef<ConfigSectionRef>(null);

  const handleSaveAll = async () => {
    setSaving(true);
    try {
      await knowledgeBaseRef.current?.save();
      await adminAssistantRef.current?.save();
      message.success('管理端配置全部保存成功');
    } catch {
      // 错误已在子组件中提示
    } finally {
      setSaving(false);
    }
  };

  const handleReloadAll = async () => {
    setLoading(true);
    try {
      await knowledgeBaseRef.current?.reload();
      await adminAssistantRef.current?.reload();
      message.success('已刷新');
    } catch {
      // 错误已在子组件中提示
    } finally {
      setLoading(false);
    }
  };

  return (
    <PageContainer
      header={{
        title: SYSTEM_MODELS_PAGE_TITLE,
      }}
    >
      <div className="system-models-page">
        <div className="system-models-page__intro">
          <div className="system-models-page__intro-main">
            <div className="system-models-page__title">后台 AI 工作台模型策略</div>
            <p className="system-models-page__description">
              统一维护管理端知识库检索范围与智能助手展示模型映射，配置保存后直接作用于后台会话。
            </p>
          </div>
          <div className="system-models-page__intro-tags">
            <span>知识库检索</span>
            <span>展示模型映射</span>
          </div>
        </div>

        <div className="system-models-page__sections system-models-page__sections--split">
          <KnowledgeBaseConfigSection
            ref={knowledgeBaseRef}
            title="管理端知识库"
            description="配置后台 AI 可访问的知识库范围、向量信息与结果重排策略。"
            enabledExtra="关闭后 AI 将不再访问知识库，但会保留当前配置，重新开启后可继续使用"
            loadConfig={getKnowledgeBaseConfig}
            loadKnowledgeBaseOptions={getKnowledgeBaseOptions}
            saveConfig={saveKnowledgeBaseConfig}
            loadFailureMessage="管理端知识库配置加载失败"
            saveFailureMessage="管理端知识库配置保存失败"
            enabledSwitchTestId="knowledge-base-enabled-switch"
            knowledgeNameSelectTestId="knowledge-base-name-select"
            rankingSwitchTestId="knowledge-ranking-switch"
            rankingClearTestId="knowledge-ranking-clear"
          />

          <AdminAssistantConfigSection ref={adminAssistantRef} />

          <div className={`system-models-page__full ${styles.notesSection}`}>
            <InfoCircleOutlined />
            <div className={styles.notesContent}>
              <div>
                <strong>配置说明：</strong>
              </div>
              <div>
                这里配置的是 AI 查询知识库时可访问的知识库范围，最多支持选择 5
                个。第一个知识库决定了当前配置的向量模型和维度；当关联知识库超过 3
                个时建议开启“结果重排”。开启后将使用选定的模型对原始检索结果进行二次精选，这能显著提升复杂知识场景下的回答质量。如果关闭，系统将直接使用原始检索结果（TopK
                默认值为 10）。
              </div>
              <div style={{ marginTop: 8 }}>
                这里维护的是管理端智能助手聊天界面的展示模型映射。用户会在聊天页右上角主动选择模型，前端提交自定义模型名称，AI
                端再映射为真实模型名称执行。
              </div>
            </div>
          </div>
        </div>
      </div>
      <div className="system-models-page__footer">
        <div className="system-models-page__footer-content">
          <Button icon={<ReloadOutlined />} onClick={handleReloadAll} loading={loading}>
            刷新
          </Button>
          <PermissionButton
            type="primary"
            icon={<SaveOutlined />}
            access={ADMIN_PERMISSIONS.agentConfig.adminUpdate}
            onClick={handleSaveAll}
            loading={saving}
          >
            保存配置
          </PermissionButton>
        </div>
      </div>
    </PageContainer>
  );
};

export default SystemModelsAdminConfigPage;
