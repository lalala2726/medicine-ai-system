import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { Button, Input, Modal } from 'antd';
import React, { useState } from 'react';
import styles from './FeedbackModal.module.less';

const REASONS = ['内容不准确', '回复太简洁', '回复太繁琐', '表达不通顺', '逻辑有误', '其他'];

interface FeedbackModalProps {
  open: boolean;
  onCancel: () => void;
  onSubmit: (data: { reasons: string[]; comment: string }) => void;
  loading?: boolean;
}

const FeedbackModal: React.FC<FeedbackModalProps> = ({ open, onCancel, onSubmit, loading }) => {
  const [selectedReasons, setSelectedReasons] = useState<string[]>([]);
  const [comment, setComment] = useState('');

  const toggleReason = (reason: string) => {
    setSelectedReasons((prev) =>
      prev.includes(reason) ? prev.filter((r) => r !== reason) : [...prev, reason],
    );
  };

  const handleSubmit = () => {
    onSubmit({ reasons: selectedReasons, comment });
    // 状态重置由 destroyOnHidden 自动处理，不在此处提前清空
  };

  const handleClose = () => {
    onCancel();
  };

  return (
    <Modal
      title="分享反馈"
      open={open}
      onCancel={handleClose}
      footer={null}
      destroyOnHidden
      centered
      width={480}
    >
      <div style={{ marginTop: 16 }}>
        <div className={styles.reasonGrid}>
          {REASONS.map((reason) => (
            <button
              key={reason}
              type="button"
              className={`${styles.reasonBtn} ${selectedReasons.includes(reason) ? styles.active : ''}`}
              onClick={() => toggleReason(reason)}
            >
              {reason}
            </button>
          ))}
        </div>

        <Input.TextArea
          autoComplete={TEXT_INPUT_AUTOCOMPLETE}
          placeholder="分享信息（可选）"
          rows={4}
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          style={{ borderRadius: 8 }}
        />

        <div className={styles.footer}>
          <Button
            type="primary"
            onClick={handleSubmit}
            loading={loading}
            style={{ borderRadius: 8 }}
          >
            提交
          </Button>
        </div>
      </div>
    </Modal>
  );
};

export default FeedbackModal;
