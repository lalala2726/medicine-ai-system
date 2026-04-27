import { DownOutlined } from '@ant-design/icons';
import { Button, Modal, Space } from 'antd';
import React, { useState } from 'react';

import styles from './VoiceSelectInput.module.less';
import VoiceSelector, { SpeechVoiceOption } from './VoiceSelector';

export interface VoiceSelectInputProps {
  value?: string;
  onChange?: (value: string) => void;
  options: SpeechVoiceOption[];
  onPreview: (voice: SpeechVoiceOption) => void;
  previewingVoiceType: string | null;
  onModalClose: () => void;
}

const VoiceSelectInput: React.FC<VoiceSelectInputProps> = ({
  value,
  onChange,
  options,
  onPreview,
  previewingVoiceType,
  onModalClose,
}) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  // 使用内部状态记录弹窗中选中的值，只有点击确定才同步出去
  const [tempValue, setTempValue] = useState<string | undefined>(value);
  const selectedOption = options.find((opt) => opt.voiceType === value);

  const handleOpenModal = () => {
    setTempValue(value);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    onModalClose();
  };

  const handleConfirm = () => {
    if (tempValue) {
      onChange?.(tempValue);
    }
    handleCloseModal();
  };

  return (
    <>
      <div
        className={styles.inputTrigger}
        onClick={handleOpenModal}
        data-testid="voice-select-trigger"
      >
        {selectedOption ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <img
              src={selectedOption.avatar}
              alt={selectedOption.name}
              className={styles.triggerAvatar}
            />
            <span className={styles.triggerName}>{selectedOption.name}</span>
          </div>
        ) : (
          <span className={styles.placeholder}>请选择音色</span>
        )}
        <DownOutlined className={styles.triggerIcon} />
      </div>

      <Modal
        title={<div style={{ fontSize: 18, fontWeight: 600, paddingBottom: 4 }}>选择音色</div>}
        open={isModalOpen}
        onCancel={handleCloseModal}
        width={760}
        destroyOnClose
        styles={{
          body: { padding: '8px 0 16px 0' },
          mask: { backdropFilter: 'blur(4px)' },
        }}
        centered
        footer={
          <Space style={{ marginTop: 16 }}>
            <Button onClick={handleCloseModal}>取消</Button>
            <Button type="primary" onClick={handleConfirm} disabled={!tempValue}>
              选择
            </Button>
          </Space>
        }
      >
        <div style={{ paddingTop: 8 }}>
          <VoiceSelector
            options={options}
            value={tempValue}
            onChange={setTempValue}
            onPreview={onPreview}
            previewingVoiceType={previewingVoiceType}
          />
        </div>
      </Modal>
    </>
  );
};

export default VoiceSelectInput;
