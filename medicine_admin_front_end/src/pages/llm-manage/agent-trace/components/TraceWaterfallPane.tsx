import { Empty } from 'antd';
import { ChevronDown, ChevronRight } from 'lucide-react';
import React, { useEffect, useMemo, useState } from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import TraceSpanIdentity from './TraceSpanIdentity';

/** 瀑布图默认最小画布宽度。 */
const WATERFALL_MIN_CANVAS_WIDTH = 720;

/** 瀑布图默认最大画布宽度。 */
const WATERFALL_MAX_CANVAS_WIDTH = 2600;

/** 每秒对应的瀑布图画布宽度。 */
const WATERFALL_PIXEL_PER_SECOND = 18;

/** 瀑布图推荐刻度数量。 */
const WATERFALL_TARGET_TICK_COUNT = 6;

/** 瀑布图行数据。 */
interface TraceWaterfallRow {
  /** 当前树节点。 */
  node: AgentTraceTypes.SpanTreeNode;
  /** 当前树节点对应的真实 Span。 */
  detailSpan?: AgentTraceTypes.SpanVo;
  /** 树节点 ID。 */
  nodeId: string;
  /** 节点层级深度。 */
  depth: number;
  /** 是否存在子节点。 */
  hasChildren: boolean;
  /** 当前节点是否折叠。 */
  collapsed: boolean;
  /** 起始时间戳毫秒。 */
  startedAtMs: number | null;
  /** 结束时间戳毫秒。 */
  endedAtMs: number | null;
  /** 耗时毫秒。 */
  durationMs: number;
}

/** 瀑布图时间轴范围。 */
interface TraceWaterfallTimelineRange {
  /** 时间轴起点毫秒。 */
  startMs: number;
  /** 时间轴终点毫秒。 */
  endMs: number;
  /** 时间轴总跨度毫秒。 */
  durationMs: number;
}

export interface TraceWaterfallPaneProps {
  /** Trace 详情。 */
  detail: AgentTraceTypes.DetailVo;
  /** 当前选中的树节点 ID。 */
  selectedNodeId: string | null;
  /** 选中节点回调。 */
  onSelectNode: (node: AgentTraceTypes.SpanTreeNode) => void;
}

/**
 * 解析时间文本为时间戳。
 *
 * @param value 时间文本。
 * @returns 时间戳毫秒；非法时返回 null。
 */
function parseTimestamp(value?: string) {
  if (!value?.trim()) {
    return null;
  }
  const normalizedValue = value.includes('T') ? value : value.replace(' ', 'T');
  const timestamp = new Date(normalizedValue).getTime();
  return Number.isFinite(timestamp) ? timestamp : null;
}

/**
 * 解析耗时毫秒。
 *
 * @param value 原始耗时。
 * @returns 耗时毫秒；非法时返回 null。
 */
function parseDurationMs(value?: number) {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return Math.max(value, 0);
  }
  return null;
}

/**
 * 解析瀑布图行耗时。
 *
 * @param node 树节点。
 * @param detailSpan 真实 Span。
 * @returns 耗时毫秒。
 */
function resolveRowDurationMs(
  node: AgentTraceTypes.SpanTreeNode,
  detailSpan?: AgentTraceTypes.SpanVo,
) {
  const durationMs = parseDurationMs(detailSpan?.durationMs ?? node.durationMs);
  if (durationMs !== null) {
    return durationMs;
  }
  const startedAtMs = parseTimestamp(detailSpan?.startedAt);
  const endedAtMs = parseTimestamp(detailSpan?.endedAt);
  if (startedAtMs !== null && endedAtMs !== null) {
    return Math.max(endedAtMs - startedAtMs, 0);
  }
  return 0;
}

/**
 * 将 Span 树拍平成瀑布图行。
 *
 * @param spanTree Span 树节点。
 * @param spanMap 真实 Span 映射。
 * @param collapsedNodeIds 已折叠节点 ID 集合。
 * @returns 瀑布图行列表。
 */
function flattenWaterfallRows(
  spanTree: AgentTraceTypes.SpanTreeNode[],
  spanMap: Map<string, AgentTraceTypes.SpanVo>,
  collapsedNodeIds: Set<string> = new Set(),
) {
  const rows: TraceWaterfallRow[] = [];

  /**
   * 递归访问树节点。
   *
   * @param nodes 当前层级节点。
   * @param depth 当前层级深度。
   * @returns 无返回值。
   */
  const visit = (nodes: AgentTraceTypes.SpanTreeNode[], depth: number) => {
    nodes.forEach((node) => {
      if (!node.nodeId) {
        return;
      }
      const detailSpan = node.sourceSpanId ? spanMap.get(node.sourceSpanId) : undefined;
      const startedAtMs = parseTimestamp(detailSpan?.startedAt);
      const durationMs = resolveRowDurationMs(node, detailSpan);
      const endedAtMs =
        startedAtMs !== null ? startedAtMs + durationMs : parseTimestamp(detailSpan?.endedAt);
      const hasChildren = Boolean(node.children?.length);
      const collapsed = collapsedNodeIds.has(node.nodeId);
      rows.push({
        node,
        detailSpan,
        nodeId: node.nodeId,
        depth,
        hasChildren,
        collapsed,
        startedAtMs,
        endedAtMs,
        durationMs,
      });
      if (hasChildren && !collapsed) {
        visit(node.children || [], depth + 1);
      }
    });
  };

  visit(spanTree, 0);
  return rows;
}

/**
 * 计算瀑布图时间轴范围。
 *
 * @param rows 瀑布图行列表。
 * @returns 时间轴范围。
 */
function resolveTimelineRange(rows: TraceWaterfallRow[]): TraceWaterfallTimelineRange {
  const timedRows = rows.filter((row) => row.startedAtMs !== null);
  if (!timedRows.length) {
    return { startMs: 0, endMs: 1000, durationMs: 1000 };
  }
  const startMs = Math.min(...timedRows.map((row) => row.startedAtMs as number));
  const endMs = Math.max(...timedRows.map((row) => row.endedAtMs ?? (row.startedAtMs as number)));
  const safeEndMs = endMs > startMs ? endMs : startMs + 1000;
  return { startMs, endMs: safeEndMs, durationMs: safeEndMs - startMs };
}

/**
 * 计算瀑布图条形定位样式。
 *
 * @param row 瀑布图行。
 * @param range 时间轴范围。
 * @returns 条形定位样式。
 */
function resolveWaterfallBarStyle(
  row: TraceWaterfallRow,
  range: TraceWaterfallTimelineRange,
): React.CSSProperties {
  if (row.startedAtMs === null) {
    return { left: 0, width: 0 };
  }
  const leftPercent = ((row.startedAtMs - range.startMs) / range.durationMs) * 100;
  const widthPercent = (Math.max(row.durationMs, 0) / range.durationMs) * 100;
  return {
    left: `${Math.max(Math.min(leftPercent, 100), 0)}%`,
    width: row.durationMs > 0 ? `max(${Math.max(widthPercent, 0.2)}%, 3px)` : '3px',
  };
}

/**
 * 计算刻度秒级步长。
 *
 * @param totalSeconds 时间轴总秒数。
 * @returns 刻度步长秒数。
 */
function resolveNiceSecondStep(totalSeconds: number) {
  const expectedStep = Math.max(totalSeconds / WATERFALL_TARGET_TICK_COUNT, 1);
  const availableSteps = [1, 2, 5, 10, 15, 30, 60, 120, 300, 600];
  return availableSteps.find((step) => step >= expectedStep) ?? 600;
}

/**
 * 计算时间轴刻度。
 *
 * @param range 时间轴范围。
 * @returns 距离起点的刻度毫秒列表。
 */
function resolveTimelineTicks(range: TraceWaterfallTimelineRange) {
  const totalSeconds = range.durationMs / 1000;
  const stepMs = resolveNiceSecondStep(totalSeconds) * 1000;
  const ticks: number[] = [];
  for (let tickMs = stepMs; tickMs < range.durationMs; tickMs += stepMs) {
    ticks.push(tickMs);
  }
  return ticks;
}

/**
 * 格式化时间轴刻度。
 *
 * @param tickMs 刻度毫秒。
 * @returns 刻度展示文本。
 */
function formatTickText(tickMs: number) {
  if (tickMs < 1000) {
    return `${Math.round(tickMs)}ms`;
  }
  const seconds = tickMs / 1000;
  return Number.isInteger(seconds) ? `${seconds}s` : `${seconds.toFixed(1)}s`;
}

/**
 * 解析瀑布图画布最小宽度。
 *
 * @param range 时间轴范围。
 * @returns 画布最小宽度。
 */
function resolveCanvasMinWidth(range: TraceWaterfallTimelineRange) {
  const timelineWidth = (range.durationMs / 1000) * WATERFALL_PIXEL_PER_SECOND;
  return Math.round(
    Math.min(Math.max(timelineWidth, WATERFALL_MIN_CANVAS_WIDTH), WATERFALL_MAX_CANVAS_WIDTH),
  );
}

/**
 * 根据 Span 类型解析瀑布图条形样式。
 *
 * @param spanType Span 类型。
 * @returns 条形样式类名。
 */
function resolveWaterfallBarClassName(spanType?: string) {
  if (spanType === 'model') {
    return styles.waterfallBarModel;
  }
  if (spanType === 'tool') {
    return styles.waterfallBarTool;
  }
  return styles.waterfallBarDefault;
}

/**
 * Trace 瀑布图面板。
 *
 * @param props Trace 瀑布图面板属性。
 * @returns Trace 瀑布图节点。
 */
const TraceWaterfallPane: React.FC<TraceWaterfallPaneProps> = ({
  detail,
  selectedNodeId,
  onSelectNode,
}) => {
  const [collapsedNodeIds, setCollapsedNodeIds] = useState<Set<string>>(() => new Set());
  const spanTreeData = useMemo(() => detail.spanTree || [], [detail.spanTree]);
  const spanMap = useMemo(
    () =>
      new Map(
        (detail.spans || [])
          .filter((span) => Boolean(span.spanId))
          .map((span) => [span.spanId as string, span]),
      ),
    [detail.spans],
  );
  const rows = useMemo(
    () => flattenWaterfallRows(spanTreeData, spanMap, collapsedNodeIds),
    [collapsedNodeIds, spanMap, spanTreeData],
  );
  const range = useMemo(() => resolveTimelineRange(rows), [rows]);
  const ticks = useMemo(() => resolveTimelineTicks(range), [range]);
  const canvasMinWidth = useMemo(() => resolveCanvasMinWidth(range), [range]);

  /**
   * Trace 切换时重置瀑布图折叠状态。
   *
   * @returns 无返回值。
   */
  useEffect(() => {
    setCollapsedNodeIds(new Set());
  }, [detail.traceId]);

  /**
   * 切换指定节点的折叠状态。
   *
   * @param nodeId 节点 ID。
   * @returns 无返回值。
   */
  const toggleCollapsedNode = (nodeId: string) => {
    setCollapsedNodeIds((currentNodeIds) => {
      const nextNodeIds = new Set(currentNodeIds);
      if (nextNodeIds.has(nodeId)) {
        nextNodeIds.delete(nodeId);
      } else {
        nextNodeIds.add(nodeId);
      }
      return nextNodeIds;
    });
  };

  if (!rows.length) {
    return (
      <div className={styles.traceWaterfallPane}>
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无 Span" />
      </div>
    );
  }

  return (
    <div className={styles.traceWaterfallPane}>
      <div className={styles.waterfallScroll}>
        <div className={styles.waterfallCanvas} style={{ minWidth: canvasMinWidth }}>
          <div className={styles.waterfallGridLines} aria-hidden>
            {ticks.map((tickMs) => (
              <span
                key={tickMs}
                className={styles.waterfallGridLine}
                style={{ left: `${(tickMs / range.durationMs) * 100}%` }}
              />
            ))}
          </div>
          <div className={styles.waterfallTimelineHeader}>
            {ticks.map((tickMs) => (
              <span
                key={tickMs}
                className={styles.waterfallTick}
                style={{ left: `${(tickMs / range.durationMs) * 100}%` }}
              >
                {formatTickText(tickMs)}
              </span>
            ))}
          </div>
          <div className={styles.waterfallRows}>
            {rows.map((row) => {
              const spanType = row.detailSpan?.spanType || row.node.spanType;
              const selected = selectedNodeId === row.nodeId;
              const barStyle = resolveWaterfallBarStyle(row, range);
              return (
                <div
                  key={row.nodeId}
                  role="button"
                  tabIndex={0}
                  aria-selected={selected}
                  className={[styles.waterfallRow, selected ? styles.waterfallRowSelected : '']
                    .filter(Boolean)
                    .join(' ')}
                  onClick={() => onSelectNode(row.node)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault();
                      onSelectNode(row.node);
                    }
                  }}
                >
                  <div
                    className={[styles.waterfallBar, resolveWaterfallBarClassName(spanType)].join(
                      ' ',
                    )}
                    style={barStyle}
                  />
                  <div className={styles.waterfallLabel} style={{ left: barStyle.left }}>
                    <div
                      className={styles.waterfallBarContent}
                      style={{ paddingLeft: row.depth * 12 + 4 }}
                    >
                      {row.hasChildren ? (
                        <button
                          type="button"
                          className={styles.waterfallToggle}
                          aria-label={row.collapsed ? '展开节点' : '折叠节点'}
                          onClick={(event) => {
                            event.stopPropagation();
                            toggleCollapsedNode(row.nodeId);
                          }}
                        >
                          {row.collapsed ? <ChevronRight size={13} /> : <ChevronDown size={13} />}
                        </button>
                      ) : (
                        <span className={styles.waterfallTogglePlaceholder} />
                      )}
                      <TraceSpanIdentity
                        span={{ ...row.node, durationMs: row.durationMs }}
                        detailSpan={row.detailSpan}
                        variant="tree"
                        showMeta={false}
                      />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};

export default TraceWaterfallPane;
