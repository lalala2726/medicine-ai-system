import { Segmented, Typography } from 'antd';
import React, { useState } from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import TraceTreePane from './TraceTreePane';
import TraceWaterfallPane from './TraceWaterfallPane';

const { Text } = Typography;

/** Trace 左侧视图模式本地存储键。 */
const TRACE_NAVIGATION_VIEW_STORAGE_KEY = 'agent-trace-navigation-view';

/** Trace 左侧视图模式。 */
type TraceNavigationView = 'tree' | 'waterfall';

export interface TraceNavigationPaneProps {
  /** Trace 详情。 */
  detail: AgentTraceTypes.DetailVo;
  /** 当前选中的树节点 ID。 */
  selectedNodeId: string | null;
  /** 选中节点回调。 */
  onSelectNode: (node: AgentTraceTypes.SpanTreeNode) => void;
}

/**
 * 判断当前环境是否可以访问浏览器窗口。
 *
 * @returns 可以访问 window 时返回 true。
 */
function canUseWindow() {
  return typeof window !== 'undefined';
}

/**
 * 判断字符串是否是合法视图模式。
 *
 * @param value 原始字符串。
 * @returns 合法视图模式返回 true。
 */
function isTraceNavigationView(value: string | null): value is TraceNavigationView {
  return value === 'tree' || value === 'waterfall';
}

/**
 * 读取用户上次选择的 Trace 左侧视图模式。
 *
 * @returns 视图模式；未设置时默认返回树形。
 */
function readStoredViewMode(): TraceNavigationView {
  if (!canUseWindow()) {
    return 'tree';
  }
  const storedValue = window.localStorage.getItem(TRACE_NAVIGATION_VIEW_STORAGE_KEY);
  return isTraceNavigationView(storedValue) ? storedValue : 'tree';
}

/**
 * 保存用户选择的 Trace 左侧视图模式。
 *
 * @param viewMode 视图模式。
 * @returns 无返回值。
 */
function writeStoredViewMode(viewMode: TraceNavigationView) {
  if (!canUseWindow()) {
    return;
  }
  window.localStorage.setItem(TRACE_NAVIGATION_VIEW_STORAGE_KEY, viewMode);
}

/**
 * Trace 左侧导航面板，负责树形和瀑布图视图切换。
 *
 * @param props Trace 左侧导航面板属性。
 * @returns Trace 左侧导航面板节点。
 */
const TraceNavigationPane: React.FC<TraceNavigationPaneProps> = ({
  detail,
  selectedNodeId,
  onSelectNode,
}) => {
  const [viewMode, setViewMode] = useState<TraceNavigationView>(() => readStoredViewMode());

  /**
   * 切换 Trace 左侧视图模式。
   *
   * @param nextViewMode 目标视图模式。
   * @returns 无返回值。
   */
  const handleViewModeChange = (nextViewMode: TraceNavigationView) => {
    setViewMode(nextViewMode);
    writeStoredViewMode(nextViewMode);
  };

  return (
    <aside className={styles.traceTreePane}>
      <div className={styles.tracePaneHeader}>
        <div className={styles.tracePaneHeaderInfo}>
          <Text strong>Trace</Text>
          <Text type="secondary">{detail.spans?.length ?? 0} spans</Text>
        </div>
        <Segmented
          size="small"
          value={viewMode}
          options={[
            { label: '树形', value: 'tree' },
            { label: '瀑布图', value: 'waterfall' },
          ]}
          onChange={(value) => handleViewModeChange(value as TraceNavigationView)}
        />
      </div>
      {viewMode === 'waterfall' ? (
        <TraceWaterfallPane
          detail={detail}
          selectedNodeId={selectedNodeId}
          onSelectNode={onSelectNode}
        />
      ) : (
        <TraceTreePane
          detail={detail}
          selectedNodeId={selectedNodeId}
          onSelectNode={onSelectNode}
        />
      )}
    </aside>
  );
};

export default TraceNavigationPane;
