import { PageContainer } from '@ant-design/pro-components';
import React, { useRef, useState } from 'react';
import { InfoCircleOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons';
import { Button, message } from 'antd';

import {
  getClientKnowledgeBaseOptions,
  getClientKnowledgeBaseConfig,
  saveClientKnowledgeBaseConfig,
} from '@/api/llm-manage/systemModels';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';

import KnowledgeBaseConfigSection from '../sections/KnowledgeBaseConfigSection';
import ClientAssistantConfigSection from '../sections/ClientAssistantConfigSection';
import {
  SYSTEM_MODELS_PAGE_TITLE,
  useSystemModelsSecondaryMenu,
  type ConfigSectionRef,
} from '../shared';
import styles from '../index.module.less';
import './index.less';

/**
 * 客户端配置页面。
 *
 * @returns 客户端配置页面节点。
 */
const SystemModelsClientConfigPage: React.FC = () => {
  useSystemModelsSecondaryMenu();
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(false);

  const knowledgeBaseRef = useRef<ConfigSectionRef>(null);
  const clientAssistantRef = useRef<ConfigSectionRef>(null);

  const handleSaveAll = async () => {
    setSaving(true);
    try {
      await knowledgeBaseRef.current?.save();
      await clientAssistantRef.current?.save();
      message.success('客户端配置全部保存成功');
    } catch {
      message.error('客户端配置保存失败，请检查页面提示后重试');
    } finally {
      setSaving(false);
    }
  };

  const handleReloadAll = async () => {
    setLoading(true);
    try {
      await knowledgeBaseRef.current?.reload();
      await clientAssistantRef.current?.reload();
      message.success('已刷新');
    } catch {
      message.error('客户端配置刷新失败，请稍后重试');
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
            <div className="system-models-page__title">客户端聊天链路模型编排</div>
            <p className="system-models-page__description">
              管理客户端聊天的知识库访问、节点模型分工与统一深度思考开关，保存后客户端会按新配置执行。
            </p>
          </div>
          <div className="system-models-page__intro-tags">
            <span>知识库范围</span>
            <span>节点模型编排</span>
            <span>深度思考</span>
          </div>
        </div>

        <div className="system-models-page__sections system-models-page__sections--split">
          <KnowledgeBaseConfigSection
            ref={knowledgeBaseRef}
            title="客户端知识库"
            description="配置客户端聊天节点可访问的知识库范围、向量信息与结果重排策略。"
            enabledExtra="关闭后，客户端聊天节点将不再访问知识库进行检索"
            loadConfig={getClientKnowledgeBaseConfig}
            loadKnowledgeBaseOptions={getClientKnowledgeBaseOptions}
            saveConfig={saveClientKnowledgeBaseConfig}
            loadFailureMessage="客户端知识库配置加载失败"
            saveFailureMessage="客户端知识库配置保存失败"
            enabledSwitchTestId="client-knowledge-base-enabled-switch"
            knowledgeNameSelectTestId="client-knowledge-base-name-select"
          />

          <ClientAssistantConfigSection ref={clientAssistantRef} />

          <div className={`system-models-page__full ${styles.notesSection}`}>
            <InfoCircleOutlined />
            <div className={styles.notesContent}>
              <div>
                <strong>配置说明：</strong>
              </div>
              <div>
                为客户端聊天场景配置独立的知识库范围。最多支持选择 5
                个。第一个知识库决定了当前配置的向量模型和维度；当关联知识库超过 3
                个时建议开启“结果重排”。开启后将使用选定的模型对原始检索结果进行二次精选，这能显著提升复杂知识场景下的回答质量。如果关闭，系统将直接使用原始检索结果（TopK
                默认值为 10）。
              </div>
              <div style={{ marginTop: 8 }}>
                <strong>关于客户端助手模型：</strong>
                <br />
                1. <strong>路由模型</strong>：负责识别用户问题属于闲聊、业务处理还是问诊链路。
                <br />
                2. <strong>服务节点模型</strong>：处理订单、商品、售后、通用对话与所有非问诊咨询。
                <br />
                3. <strong>诊断节点模型</strong>
                ：专门用于问诊链路中的症状分析、问答追问与诊断建议生成。
                <br />
                4. <strong>统一深度思考支持</strong>
                ：仅当服务节点模型和诊断节点模型都支持深度思考时才可开启；开启后，客户端聊天输入框会展示“深度思考”开关。
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
            access={ADMIN_PERMISSIONS.agentConfig.clientUpdate}
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

export default SystemModelsClientConfigPage;
