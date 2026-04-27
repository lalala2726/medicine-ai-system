import { PageContainer } from '@ant-design/pro-components';
import React, { useRef, useState } from 'react';
import { InfoCircleOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons';
import { Button, message } from 'antd';

import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import CommonCapabilityConfigSection from '../sections/CommonCapabilityConfigSection';
import SpeechConfigSection from '../sections/SpeechConfigSection';
import {
  SYSTEM_MODELS_PAGE_TITLE,
  useSystemModelsSecondaryMenu,
  type ConfigSectionRef,
} from '../shared';
import styles from '../index.module.less';
import './index.less';

/**
 * 通用能力页面。
 *
 * @returns 通用能力页面节点。
 */
const SystemModelsCommonCapabilityPage: React.FC = () => {
  useSystemModelsSecondaryMenu();
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(false);

  const commonCapabilityRef = useRef<ConfigSectionRef>(null);
  const speechRef = useRef<ConfigSectionRef>(null);

  const handleSaveAll = async () => {
    setSaving(true);
    try {
      await commonCapabilityRef.current?.save();
      await speechRef.current?.save();
      message.success('通用能力配置全部保存成功');
    } catch {
      // 错误已在子组件中提示
    } finally {
      setSaving(false);
    }
  };

  const handleReloadAll = async () => {
    setLoading(true);
    try {
      await commonCapabilityRef.current?.reload();
      await speechRef.current?.reload();
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
            <div className="system-models-page__title">跨场景基础模型与语音能力</div>
            <p className="system-models-page__description">
              集中配置图片识别、历史摘要、标题生成与语音合成能力，为管理端和客户端共用链路提供基础能力。
            </p>
          </div>
          <div className="system-models-page__intro-tags">
            <span>图片理解</span>
            <span>摘要标题</span>
            <span>语音合成</span>
          </div>
        </div>

        <div className="system-models-page__sections system-models-page__sections--split">
          <CommonCapabilityConfigSection ref={commonCapabilityRef} />
          <SpeechConfigSection ref={speechRef} />

          <div className={`system-models-page__full ${styles.notesSection}`}>
            <InfoCircleOutlined />
            <div className={styles.notesContent}>
              <div>
                <strong>通用能力模型提取说明：</strong>
              </div>
              <div>
                1. <strong>图片识别模型</strong>
                ：必须选择支持图片理解的视觉模型，用于图片上传、图片问答和多模态预解析。
                <br />
                2. <strong>聊天历史总结模型</strong>
                ：用于在长会话场景中压缩历史上下文，降低主对话链路的上下文负担。
                <br />
                3. <strong>聊天标题生成模型</strong>
                ：用于根据会话首轮内容生成标题，便于历史会话检索和管理。
              </div>
              <div style={{ marginTop: 8 }}>
                <strong>语音合成配置说明：</strong>
              </div>
              <div>
                1. <strong>服务提供商</strong>：目前系统深度集成了 <strong>豆包语音 2.0</strong>{' '}
                服务。如需获取 App ID 和 Access Token，请访问火山引擎语音技术控制台。
                <br />
                2. <strong>音色选择</strong>
                ：此处选择的音色将作为大模型在聊天界面中通过语音输出时的默认声音。
                <br />
                3. <strong>最大文本长度</strong>
                ：用于限制单次语音合成的最大文本长度，避免生成过长文本时请求失败。
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
            access={ADMIN_PERMISSIONS.agentConfig.commonUpdate}
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

export default SystemModelsCommonCapabilityPage;
