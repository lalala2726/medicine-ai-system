import { Tooltip, Typography } from 'antd';
import React from 'react';
import styles from '../index.module.less';

const { Text } = Typography;

export interface TraceTextPreviewProps {
  /** 预览文本。 */
  text?: string | null;
}

/**
 * Trace 列表输入输出文本预览组件。
 *
 * @param props 文本预览属性。
 * @returns 文本预览节点。
 */
const TraceTextPreview: React.FC<TraceTextPreviewProps> = ({ text }) => {
  const normalizedText = String(text || '').trim();
  if (!normalizedText) {
    return <Text type="secondary">-</Text>;
  }

  return (
    <Tooltip title={<div className={styles.traceTextTooltip}>{normalizedText}</div>}>
      <Text copyable={{ text: normalizedText }} className={styles.traceTextPreview}>
        {normalizedText}
      </Text>
    </Tooltip>
  );
};

export default TraceTextPreview;
