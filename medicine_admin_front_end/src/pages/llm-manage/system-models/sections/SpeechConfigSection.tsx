import { PASSWORD_INPUT_AUTOCOMPLETE, TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { Form, Input, InputNumber, Spin, Typography, message } from 'antd';
import React, {
  useCallback,
  useEffect,
  useRef,
  useState,
  forwardRef,
  useImperativeHandle,
} from 'react';

import { getSpeechConfig, saveSpeechConfig } from '@/api/llm-manage/systemModels';

import speechVoiceOptions from '../data/volcengineSpeechVoices.json';
import styles from '../index.module.less';
import { getSystemModelsErrorMessage, type ConfigSectionRef } from '../shared';
import VoiceSelectInput from '../components/SpeechConfigDrawer/VoiceSelectInput';
import type { SpeechVoiceOption } from '../components/SpeechConfigDrawer/VoiceSelector';

/**
 * 豆包语音表单值。
 */
interface SpeechConfigFormValues {
  /** 火山引擎 App ID。 */
  appId?: string;
  /** 火山引擎 Access Token。 */
  accessToken?: string;
  /** 文本转语音配置。 */
  textToSpeech?: {
    /** 语音类型。 */
    voiceType?: string;
    /** 最大文本长度。 */
    maxTextChars?: number | null;
  };
}

/** 语音音色选项列表。 */
const speechVoiceOptionList: SpeechVoiceOption[] = speechVoiceOptions;

/**
 * 语音合成与识别配置区块。
 *
 * @returns 语音合成语识别配置区块节点。
 */
const SpeechConfigSection = forwardRef<ConfigSectionRef>((props, ref) => {
  const [form] = Form.useForm<SpeechConfigFormValues>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [previewingVoiceType, setPreviewingVoiceType] = useState<string | null>(null);
  const previewAudioRef = useRef<HTMLAudioElement | null>(null);

  /**
   * 销毁当前试听音频实例。
   *
   * @returns 无返回值。
   */
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

  /**
   * 停止当前试听。
   *
   * @returns 无返回值。
   */
  const stopPreview = useCallback(() => {
    disposePreviewAudio();
    setPreviewingVoiceType(null);
  }, [disposePreviewAudio]);

  /**
   * 加载豆包语音配置。
   *
   * @returns 无返回值。
   */
  const loadSection = useCallback(async () => {
    setLoading(true);

    try {
      const nextDetail = await getSpeechConfig();
      const resolvedDetail = nextDetail || {};

      form.setFieldsValue({
        appId: resolvedDetail?.appId,
        accessToken: undefined,
        textToSpeech: {
          voiceType: resolvedDetail?.textToSpeech?.voiceType,
          maxTextChars: resolvedDetail?.textToSpeech?.maxTextChars ?? 300,
        },
      });
    } catch (error) {
      message.error(getSystemModelsErrorMessage(error, '豆包语音配置加载失败'));
    } finally {
      setLoading(false);
    }
  }, [form]);

  useEffect(() => {
    void loadSection();
  }, [loadSection]);

  useEffect(
    () => () => {
      disposePreviewAudio();
    },
    [disposePreviewAudio],
  );

  /**
   * 试听指定音色。
   *
   * @param voice 当前音色选项。
   * @returns 无返回值。
   */
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
        message.error(getSystemModelsErrorMessage(error, '音色试听失败'));
      }
    },
    [previewingVoiceType, stopPreview],
  );

  /**
   * 保存豆包语音配置。
   *
   * @returns 无返回值。
   */
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
    } catch (error) {
      if ((error as { errorFields?: unknown[] } | null)?.errorFields) {
        throw error;
      }
      message.error(getSystemModelsErrorMessage(error, '语音合成配置保存失败'));
      throw error;
    } finally {
      setSaving(false);
    }
  }, [form]);

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
          <div className={styles.sectionCardTitle}>豆包语音</div>
          <Typography.Text type="secondary" className={styles.sectionCardDescription}>
            配置字节跳动火山引擎提供的豆包语音合成系统（TTS）。配置将应用于对话中的文本转语音播报。
          </Typography.Text>
        </div>
      </div>

      <Spin spinning={loading || saving}>
        <Form form={form} layout="vertical">
          <div className={styles.drawerBody}>
            <div className={styles.formCluster}>
              <Typography.Title level={5} style={{ marginTop: 0 }}>
                公共凭证
              </Typography.Title>
              <Form.Item
                label="App ID"
                name="appId"
                rules={[{ required: true, message: '请输入 App ID' }]}
                tooltip="火山引擎语音服务对应的 App ID"
                className={styles.compactFormItem}
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
                className={styles.compactFormItem}
              >
                <Input.Password
                  placeholder="首次保存必须填写；留空则保留原 Token"
                  autoComplete={PASSWORD_INPUT_AUTOCOMPLETE}
                  data-1p-ignore="true"
                  data-lpignore="true"
                  data-form-type="other"
                  role="presentation"
                  readOnly
                  onFocus={(event) => {
                    event.target.removeAttribute('readonly');
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
                className={styles.compactFormItem}
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
                tooltip="限制单次合成请求允许的最大文本长度，过长可能会导致合成失败或超时"
                className={styles.compactFormItem}
              >
                <InputNumber
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  min={10}
                  max={8000}
                  precision={0}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            </div>
          </div>
        </Form>
      </Spin>
    </div>
  );
});

export default SpeechConfigSection;
