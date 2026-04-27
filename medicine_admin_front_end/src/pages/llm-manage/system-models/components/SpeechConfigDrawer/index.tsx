import { PASSWORD_INPUT_AUTOCOMPLETE, TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { Button, Drawer, Form, Input, InputNumber, Space, Typography, message } from 'antd';
import React, { useCallback, useEffect, useRef, useState } from 'react';

import { saveSpeechConfig, type SystemModelTypes } from '@/api/llm-manage/systemModels';
import { useThemeContext } from '@/contexts/ThemeContext';

import speechVoiceOptions from '../../data/volcengineSpeechVoices.json';
import styles from '../../index.module.less';
import VoiceSelectInput from './VoiceSelectInput';
import type { SpeechVoiceOption } from './VoiceSelector';

interface SpeechConfigDrawerProps {
  open: boolean;
  detail?: SystemModelTypes.SpeechSystemModelConfig;
  onClose: () => void;
  onSaved: () => Promise<void> | void;
}

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

const speechVoiceOptionList: SpeechVoiceOption[] = speechVoiceOptions;

/**
 * 豆包语音配置抽屉。
 */
const SpeechConfigDrawer: React.FC<SpeechConfigDrawerProps> = ({
  open,
  detail,
  onClose,
  onSaved,
}) => {
  const { isDark } = useThemeContext();
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [previewingVoiceType, setPreviewingVoiceType] = useState<string | null>(null);
  const previewAudioRef = useRef<HTMLAudioElement | null>(null);

  const disposePreviewAudio = useCallback(() => {
    const currentAudio = previewAudioRef.current;
    if (!currentAudio) {
      return;
    }

    currentAudio.pause();
    currentAudio.currentTime = 0;
    currentAudio.onended = null;
    currentAudio.onerror = null;
    currentAudio.src = '';
    previewAudioRef.current = null;
  }, []);

  const stopPreview = useCallback(() => {
    disposePreviewAudio();
    setPreviewingVoiceType(null);
  }, [disposePreviewAudio]);

  useEffect(() => {
    if (!open) {
      stopPreview();
      form.resetFields();
      return;
    }

    form.setFieldsValue({
      appId: detail?.appId,
      accessToken: undefined,
      textToSpeech: {
        voiceType: detail?.textToSpeech?.voiceType,
        maxTextChars: detail?.textToSpeech?.maxTextChars ?? 300,
      },
    });
  }, [detail, form, open, stopPreview]);

  useEffect(
    () => () => {
      disposePreviewAudio();
    },
    [disposePreviewAudio],
  );

  const handlePreview = useCallback(
    async (voice: SpeechVoiceOption) => {
      if (previewingVoiceType === voice.voiceType) {
        stopPreview();
        return;
      }

      stopPreview();

      const audio = new Audio(voice.trialUrl);
      previewAudioRef.current = audio;
      setPreviewingVoiceType(voice.voiceType);

      audio.onended = () => {
        if (previewAudioRef.current !== audio) {
          return;
        }
        previewAudioRef.current = null;
        setPreviewingVoiceType(null);
      };

      audio.onerror = () => {
        if (previewAudioRef.current !== audio) {
          return;
        }
        previewAudioRef.current = null;
        setPreviewingVoiceType(null);
        message.error('音色试听失败');
      };

      try {
        await audio.play();
      } catch (error) {
        if (previewAudioRef.current === audio) {
          previewAudioRef.current = null;
          setPreviewingVoiceType(null);
        }
        message.error(getErrorMessage(error, '音色试听失败'));
      }
    },
    [previewingVoiceType, stopPreview],
  );

  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();

      setSaving(true);
      await saveSpeechConfig({
        appId: values.appId ? String(values.appId).trim() : undefined,
        accessToken: values.accessToken?.trim() || null,
        textToSpeech: {
          voiceType: values.textToSpeech?.voiceType?.trim(),
          maxTextChars: values.textToSpeech?.maxTextChars,
        },
      });
      message.success('豆包语音配置已保存');
      await onSaved();
      onClose();
    } catch (error) {
      if ((error as { errorFields?: unknown[] } | null)?.errorFields) {
        return;
      }
      message.error(getErrorMessage(error, '豆包语音配置保存失败'));
    } finally {
      setSaving(false);
    }
  }, [form, onClose, onSaved]);

  return (
    <Drawer
      title="配置 豆包语音"
      width={760}
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
      <Form form={form} layout="vertical">
        <div className={styles.slotStack}>
          <Typography.Title level={5} style={{ marginTop: 0 }}>
            公共凭证
          </Typography.Title>
          <Form.Item
            label="App ID"
            name="appId"
            rules={[{ required: true, message: '请输入 App ID' }]}
            tooltip="火山引擎语音服务对应的 App ID"
          >
            <InputNumber
              autoComplete={TEXT_INPUT_AUTOCOMPLETE}
              controls={false}
              style={{ width: '100%' }}
              placeholder="请输入火山引擎语音 App ID"
            />
          </Form.Item>
          <Form.Item
            label="Access Token"
            name="accessToken"
            tooltip="访问火山引擎语音服务的 Access Token"
          >
            <Input.Password
              placeholder="首次保存必须填写；留空则保留原 Token"
              autoComplete={PASSWORD_INPUT_AUTOCOMPLETE}
              data-1p-ignore="true"
              data-lpignore="true"
              data-form-type="other"
              role="presentation"
              readOnly
              onFocus={(e) => {
                e.target.removeAttribute('readonly');
              }}
              data-testid="speech-access-token"
            />
          </Form.Item>

          <div className={styles.fieldDivider} />

          <Typography.Title level={5} style={{ marginTop: 0 }}>
            语音合成配置
          </Typography.Title>
          <Form.Item
            label="音色选择"
            name={['textToSpeech', 'voiceType']}
            rules={[{ required: true, message: '请选择语音合成音色' }]}
            tooltip="选择前端展示的豆包音色，实际保存给后端的仍然是对应的 Voice Type"
          >
            <VoiceSelectInput
              options={speechVoiceOptionList}
              onPreview={handlePreview}
              previewingVoiceType={previewingVoiceType}
              onModalClose={stopPreview}
            />
          </Form.Item>
          <Form.Item
            label="最大文本长度"
            name={['textToSpeech', 'maxTextChars']}
            rules={[
              { required: true, message: '请输入最大文本长度' },
              { type: 'number', min: 10, message: '最大文本长度不能小于10' },
              { type: 'number', max: 8000, message: '最大文本长度不能大于8000' },
            ]}
            tooltip="限制单词合成请求允许的最大文本长度，过长可能会导致合成失败或超时"
          >
            <InputNumber
              autoComplete={TEXT_INPUT_AUTOCOMPLETE}
              min={10}
              max={8000}
              precision={0}
              style={{ width: '100%' }}
            />
          </Form.Item>

          <div style={{ marginTop: 24, color: '#8c8c8c', fontSize: 13, lineHeight: '1.6' }}>
            <div>
              <strong>配置说明：</strong>
            </div>
            <div>
              1. <strong>服务提供商</strong>：目前系统深度集成了 <strong>豆包语音 2.0</strong>{' '}
              服务。如需获取 App ID 和 Access Token，请访问：
              <a
                href="https://console.volcengine.com/speech/new/"
                target="_blank"
                rel="noreferrer"
                style={{ color: '#0056ff', marginLeft: 4 }}
              >
                火山引擎语音技术控制台
              </a>
            </div>
            <div>
              2. <strong>音色选择</strong>
              ：此处选择的音色将作为大模型在聊天界面中通过语音输出时的默认声音。
            </div>
            <div>
              3. <strong>最大文本长度</strong>
              ：限制单次合成请求允许的最大字符长度（10-8000）。大模型输出的文本若超过此长度，超出的部分将不会被播放
            </div>
            <div style={{ marginTop: 8 }}>
              * 注：Access Token
              保存之后为了安全不会再次展示，如果需要修改直接填写并保存即可，如果没有修改的需求请不要填写任何字符。
            </div>
          </div>
        </div>
      </Form>
    </Drawer>
  );
};

export default SpeechConfigDrawer;
