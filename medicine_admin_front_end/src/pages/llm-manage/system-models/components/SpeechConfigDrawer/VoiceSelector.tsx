import { PauseCircleOutlined, PlayCircleOutlined, CheckOutlined } from '@ant-design/icons';
import React from 'react';

import styles from './VoiceSelector.module.less';

export interface SpeechVoiceOption {
  voiceType: string;
  name: string;
  avatar: string;
  description: string;
  trialUrl: string;
}

export interface VoiceSelectorProps {
  options: SpeechVoiceOption[];
  value?: string;
  onChange?: (value: string) => void;
  onPreview: (voice: SpeechVoiceOption) => void;
  previewingVoiceType: string | null;
}

const VoiceSelector: React.FC<VoiceSelectorProps> = ({
  options,
  value,
  onChange,
  onPreview,
  previewingVoiceType,
}) => {
  const matchedPreset = options.some((option) => option.voiceType === value);

  return (
    <div className={styles.voiceSelector} data-testid="speech-voice-selector">
      {value && !matchedPreset ? (
        <div className={styles.voiceFallbackHint} data-testid="speech-voice-unknown-hint">
          当前已配置音色不在预置列表中，若需要切换请重新选择一个音色卡片。
        </div>
      ) : null}

      <div className={styles.voiceList} role="radiogroup" aria-label="豆包音色列表">
        {options.map((option) => {
          const selected = value === option.voiceType;
          const previewing = previewingVoiceType === option.voiceType;

          return (
            <div
              key={option.voiceType}
              className={
                selected ? `${styles.voiceCard} ${styles.voiceCardActive}` : styles.voiceCard
              }
              data-testid={`speech-voice-card-${option.voiceType}`}
              onClick={() => onChange?.(option.voiceType)}
            >
              <div
                className={`${styles.avatarWrapper} ${previewing ? styles.avatarWrapperActive : ''}`}
                onClick={(e) => {
                  e.stopPropagation();
                  onPreview(option);
                }}
                title={previewing ? '点击停止试听' : '点击试听'}
              >
                <img
                  className={`${styles.voiceAvatar} ${previewing ? styles.voiceAvatarPlaying : ''}`}
                  src={option.avatar}
                  alt={option.name}
                />
                <div
                  className={`${styles.previewOverlay} ${previewing ? styles.previewOverlayActive : ''}`}
                >
                  {previewing ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                </div>
              </div>

              <div className={styles.voiceContent}>
                <div className={styles.voiceHeader}>
                  <div className={styles.voiceName}>{option.name}</div>
                  {selected && (
                    <div className={styles.selectedIcon}>
                      <CheckOutlined />
                    </div>
                  )}
                </div>
                <div className={styles.voiceDescription}>{option.description}</div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default VoiceSelector;
