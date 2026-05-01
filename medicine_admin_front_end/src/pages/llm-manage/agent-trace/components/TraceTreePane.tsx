import { Empty, Tree } from 'antd';
import React, { useMemo } from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import TraceSpanIdentity from './TraceSpanIdentity';

interface AgentTraceTreeDataNode {
  /** Tree 节点唯一键。 */
  key: string;
  /** Tree 节点标题。 */
  title: React.ReactNode;
  /** 真实 Span 唯一标识。 */
  sourceSpanId?: string;
  /** 子 Tree 节点。 */
  children?: AgentTraceTreeDataNode[];
}

export interface TraceTreePaneProps {
  /** Trace 详情。 */
  detail: AgentTraceTypes.DetailVo;
  /** 当前选中的树节点 ID。 */
  selectedNodeId: string | null;
  /** 选中树节点回调。 */
  onSelectNode: (node: AgentTraceTypes.SpanTreeNode) => void;
}

/**
 * 渲染左侧 Trace 树节点标题。
 *
 * @param node Span 树节点。
 * @param spanMap 真实 Span 明细索引。
 * @returns Tree 标题节点。
 */
function renderSpanTreeTitle(
  node: AgentTraceTypes.SpanTreeNode,
  spanMap: Map<string, AgentTraceTypes.SpanVo>,
) {
  const detailSpan = node.sourceSpanId ? spanMap.get(node.sourceSpanId) : undefined;
  return <TraceSpanIdentity detailSpan={detailSpan} span={node} variant="tree" showMeta={false} />;
}

/**
 * 将 Span 树转换成 Ant Design Tree 数据。
 *
 * @param nodes Span 树节点列表。
 * @param spanMap 真实 Span 明细索引。
 * @returns Ant Design Tree 节点列表。
 */
function buildTraceTreeData(
  nodes: AgentTraceTypes.SpanTreeNode[],
  spanMap: Map<string, AgentTraceTypes.SpanVo>,
): AgentTraceTreeDataNode[] {
  return nodes
    .filter((node) => Boolean(node.nodeId))
    .map((node) => ({
      key: node.nodeId as string,
      sourceSpanId: node.sourceSpanId,
      title: renderSpanTreeTitle(node, spanMap),
      children: node.children?.length ? buildTraceTreeData(node.children, spanMap) : undefined,
    }));
}

/**
 * 将 Span 树节点拍平成 nodeId 映射。
 *
 * @param nodes Span 树节点列表。
 * @returns nodeId 到树节点的映射。
 */
function buildTraceTreeNodeMap(nodes: AgentTraceTypes.SpanTreeNode[]) {
  const nodeMap = new Map<string, AgentTraceTypes.SpanTreeNode>();
  const visit = (currentNodes: AgentTraceTypes.SpanTreeNode[]) => {
    currentNodes.forEach((node) => {
      if (node.nodeId) {
        nodeMap.set(node.nodeId, node);
      }
      if (node.children?.length) {
        visit(node.children);
      }
    });
  };
  visit(nodes);
  return nodeMap;
}

/**
 * Trace 左侧树面板。
 *
 * @param props Trace 树面板属性。
 * @returns Trace 树面板节点。
 */
const TraceTreePane: React.FC<TraceTreePaneProps> = ({ detail, selectedNodeId, onSelectNode }) => {
  /** 后端返回的 Span 树节点。 */
  const spanTreeData = useMemo(() => detail.spanTree || [], [detail.spanTree]);
  /** 真实 Span 明细索引。 */
  const spanMap = useMemo(
    () =>
      new Map(
        (detail.spans || [])
          .filter((span) => Boolean(span.spanId))
          .map((span) => [span.spanId as string, span]),
      ),
    [detail.spans],
  );
  /** Ant Design Tree 使用的 Span 树节点。 */
  const traceTreeData = useMemo(
    () => buildTraceTreeData(spanTreeData, spanMap),
    [spanMap, spanTreeData],
  );
  /** nodeId 到 Span 树节点的映射。 */
  const traceTreeNodeMap = useMemo(() => buildTraceTreeNodeMap(spanTreeData), [spanTreeData]);

  return (
    <div className={styles.traceTreeBody}>
      {traceTreeData.length ? (
        <Tree
          key={detail.traceId}
          blockNode
          defaultExpandAll
          showLine={{ showLeafIcon: false }}
          selectedKeys={selectedNodeId ? [selectedNodeId] : []}
          treeData={traceTreeData}
          onSelect={(selectedKeys) => {
            const selectedKey = selectedKeys[0];
            if (!selectedKey) {
              return;
            }
            const selectedNode = traceTreeNodeMap.get(String(selectedKey));
            if (selectedNode) {
              onSelectNode(selectedNode);
            }
          }}
        />
      ) : (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无 Span" />
      )}
    </div>
  );
};

export default TraceTreePane;
