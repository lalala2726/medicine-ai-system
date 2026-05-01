import XMarkdown from '@ant-design/x-markdown';
import { Empty, Space, Tag, Typography } from 'antd';
import { ChevronDown, ChevronRight } from 'lucide-react';
import React, { useState } from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import { useThemeContext } from '@/contexts/ThemeContext';
import styles from '../index.module.less';

import '@ant-design/x-markdown/themes/light.css';
import '@ant-design/x-markdown/themes/dark.css';

const { Text } = Typography;

export interface TraceSystemPromptBlockProps {
  /** 系统提示词。 */
  systemPrompt?: AgentTraceTypes.ModelSystemPromptVo | null;
}

/**
 * 系统提示词折叠展示块。
 *
 * @param props 组件属性。
 * @returns 系统提示词展示节点。
 */
const TraceSystemPromptBlock: React.FC<TraceSystemPromptBlockProps> = ({ systemPrompt }) => {
  const [expanded, setExpanded] = useState(false);
  const { isDark } = useThemeContext();
  const markdownThemeClassName = isDark ? 'x-markdown-dark' : 'x-markdown-light';

  if (!systemPrompt?.content) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无系统提示词" />;
  }

  return (
    <div className={styles.systemPromptBlock}>
      <div
        className={styles.systemPromptToggle}
        aria-expanded={expanded}
        onClick={() => setExpanded((current) => !current)}
      >
        <div className={styles.systemPromptTitle}>
          <Space size={4}>
            <Text type="secondary" style={{ display: 'flex', alignItems: 'center' }}>
              {expanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
            </Text>
            <Tag
              color="blue"
              bordered={false}
              style={{ margin: 0, borderRadius: 4, fontWeight: 500 }}
            >
              系统提示词
            </Tag>
          </Space>
        </div>

        {!expanded && (
          <Text type="secondary" className={styles.systemPromptPreview}>
            {systemPrompt.content}
          </Text>
        )}

        <div onClick={(e) => e.stopPropagation()} className={styles.systemPromptAction}>
          <Text copyable={{ text: systemPrompt.content }} type="secondary">
            复制
          </Text>
        </div>
      </div>
      {expanded ? (
        <div className={styles.modelSystemPrompt}>
          <XMarkdown
            className={markdownThemeClassName}
            content={systemPrompt.content}
            paragraphTag="div"
            openLinksInNewTab
          />
        </div>
      ) : null}
    </div>
  );
};

export default TraceSystemPromptBlock;
