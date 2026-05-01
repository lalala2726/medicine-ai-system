import { Descriptions, Typography } from 'antd';
import React from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import { renderDuration, renderJsonPayload, renderStatusTag } from './traceShared';

const { Text } = Typography;

export interface TraceModelAttributesPanelProps {
  /** 当前选中的模型 Span。 */
  selectedSpan: AgentTraceTypes.SpanVo;
  /** 模型结构化详情。 */
  modelDetail: AgentTraceTypes.ModelDetailVo;
}

/**
 * 渲染模型属性面板。
 *
 * @param props 组件属性。
 * @returns 模型属性面板节点。
 */
const TraceModelAttributesPanel: React.FC<TraceModelAttributesPanelProps> = ({
  selectedSpan,
  modelDetail,
}) => (
  <div className={styles.modelPanelContent}>
    <Descriptions
      bordered
      size="small"
      column={2}
      className={styles.modelDescriptionTable}
      items={[
        {
          key: 'modelName',
          label: '模型名称',
          children: <Text copyable>{modelDetail.modelName || selectedSpan.name || '-'}</Text>,
        },
        {
          key: 'status',
          label: '状态',
          children: renderStatusTag(selectedSpan.status),
        },
        {
          key: 'modelClass',
          label: '模型类',
          children: modelDetail.modelClass || '-',
        },
        {
          key: 'slot',
          label: '模型槽位',
          children: modelDetail.slot || '-',
        },
        {
          key: 'finishReason',
          label: '结束原因',
          children: modelDetail.finishReason || '-',
        },
        {
          key: 'duration',
          label: '耗时',
          children: renderDuration(selectedSpan.durationMs),
        },
      ]}
    />

    <section className={styles.modelPlainSection}>
      <div className={styles.modelPlainSectionHeader}>
        <Text strong>模型设置</Text>
      </div>
      <div className={styles.modelJsonBlock}>{renderJsonPayload(modelDetail.settings)}</div>
    </section>

    <section className={styles.modelPlainSection}>
      <div className={styles.modelPlainSectionHeader}>
        <Text strong>原始属性</Text>
      </div>
      <div className={styles.modelJsonBlock}>{renderJsonPayload(selectedSpan.attributes)}</div>
    </section>
  </div>
);

export default TraceModelAttributesPanel;
