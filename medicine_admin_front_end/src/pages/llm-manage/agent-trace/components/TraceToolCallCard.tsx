import { Space, Typography } from 'antd';
import { ChevronDown, ChevronRight, Wrench } from 'lucide-react';
import React, { useState } from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import { renderDuration, renderJsonPayload, renderStatusTag } from './traceShared';
import { resolveToolName } from './traceModelUtils';

const { Text } = Typography;

export interface TraceToolCallCardProps {
  /** 工具调用详情。 */
  call: AgentTraceTypes.ModelToolCallVo;
  /** 默认是否展开工具调用详情。 */
  defaultExpanded?: boolean;
}

/**
 * 判断工具调用是否存在错误。
 *
 * @param call 工具调用详情。
 * @returns 存在错误返回 true。
 */
function hasToolCallError(call: AgentTraceTypes.ModelToolCallVo) {
  return call.errorPayload !== undefined && call.errorPayload !== null && call.errorPayload !== '';
}

/**
 * 渲染工具调用 ID。
 *
 * @param call 工具调用详情。
 * @returns 工具调用 ID 节点。
 */
function renderToolCallId(call: AgentTraceTypes.ModelToolCallVo) {
  if (!call.id) {
    return null;
  }
  return (
    <Text copyable type="secondary" className={styles.traceToolCallId}>
      {call.id}
    </Text>
  );
}

/**
 * 工具调用统一卡片。
 *
 * @param props 组件属性。
 * @returns 工具调用卡片节点。
 */
const TraceToolCallCard: React.FC<TraceToolCallCardProps> = ({ call, defaultExpanded = false }) => {
  const [expanded, setExpanded] = useState(defaultExpanded);
  const hasError = hasToolCallError(call);

  return (
    <div className={styles.traceToolCallCard}>
      <button
        type="button"
        className={styles.traceToolCallHeaderButton}
        aria-expanded={expanded}
        onClick={() => setExpanded((current) => !current)}
      >
        <span className={styles.traceToolCallIcon}>
          <Wrench size={14} />
        </span>
        <div className={styles.traceToolCallHeaderMain}>
          <div className={styles.traceToolCallTitleLine}>
            <Text strong className={styles.traceToolCallName}>
              {resolveToolName(call)}
            </Text>
            <Space size={6} wrap className={styles.traceToolCallMeta}>
              {call.status ? renderStatusTag(call.status) : null}
              {call.durationMs !== undefined && call.durationMs !== null ? (
                <Text type="secondary">{renderDuration(call.durationMs)}</Text>
              ) : null}
              {hasError ? <Text type="danger">执行异常</Text> : null}
            </Space>
          </div>
          {renderToolCallId(call)}
        </div>
        <span className={styles.traceToolCallChevron}>
          {expanded ? <ChevronDown size={15} /> : <ChevronRight size={15} />}
        </span>
      </button>
      {expanded ? (
        <div className={styles.traceToolCallBody}>
          <div className={styles.traceToolCallPayloadGrid}>
            <div className={styles.traceToolCallPayload}>
              <Text type="secondary">入参</Text>
              {renderJsonPayload(call.arguments)}
            </div>
            <div className={styles.traceToolCallPayload}>
              {hasError ? (
                <>
                  <Text type="danger">错误</Text>
                  {renderJsonPayload(call.errorPayload)}
                </>
              ) : (
                <>
                  <Text type="secondary">返回结果</Text>
                  {renderJsonPayload(call.outputPayload)}
                </>
              )}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
};

export default TraceToolCallCard;
