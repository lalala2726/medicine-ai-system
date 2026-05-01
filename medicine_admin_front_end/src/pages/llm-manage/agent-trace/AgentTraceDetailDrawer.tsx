import { Empty, Segmented, Spin, message } from 'antd';
import React, { useEffect, useMemo, useState } from 'react';
import { getAgentTraceDetail, type AgentTraceTypes } from '@/api/agent/trace';
import ResizableDrawer from '@/components/ResizableDrawer';
import ResizableSplitPane from '@/components/ResizableSplitPane';
import TraceConversationInspector from './components/TraceConversationInspector';
import TraceNavigationPane from './components/TraceNavigationPane';
import TraceOverviewInspector from './components/TraceOverviewInspector';
import TraceSpanInspector from './components/TraceSpanInspector';
import {
  findPreferredSpanTreeNodeBySourceSpanId,
  findSpanTreeNodeById,
  resolveInitialSelectedNodeId,
} from './components/traceShared';
import styles from './index.module.less';

export interface AgentTraceDetailDrawerProps {
  /** 抽屉是否打开。 */
  open: boolean;
  /** 当前 Trace ID。 */
  traceId: string | null;
  /** 关闭抽屉回调。 */
  onClose: () => void;
}

/**
 * 智能体跟踪详情抽屉。
 *
 * @param props 抽屉属性。
 * @returns 智能体跟踪详情抽屉节点。
 */
const AgentTraceDetailDrawer: React.FC<AgentTraceDetailDrawerProps> = ({
  open,
  traceId,
  onClose,
}) => {
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<AgentTraceTypes.DetailVo | null>(null);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [detailViewMode, setDetailViewMode] = useState<'message' | 'detail'>('detail');
  const [messageApi, contextHolder] = message.useMessage();

  /** 当前选中的树节点。 */
  const selectedTreeNode = useMemo(
    () => findSpanTreeNodeById(detail?.spanTree || [], selectedNodeId),
    [detail?.spanTree, selectedNodeId],
  );

  /** 当前选中的真实 Span。 */
  const selectedSpan = useMemo(
    () => detail?.spans?.find((item) => item.spanId === selectedTreeNode?.sourceSpanId) ?? null,
    [detail?.spans, selectedTreeNode?.sourceSpanId],
  );

  /** 当前是否选中顶层 root trace。 */
  const selectedRootTrace = Boolean(
    detail?.rootSpanId && selectedTreeNode?.sourceSpanId === detail.rootSpanId,
  );

  /** 当前选中节点的可读消息视图。 */
  const selectedMessageView = useMemo(() => {
    if (selectedRootTrace) {
      return detail?.overviewDetail?.messageView || null;
    }
    if (selectedSpan?.modelDetail?.messageView) {
      return selectedSpan.modelDetail.messageView;
    }
    if (selectedSpan?.nodeDetail?.messageView) {
      return selectedSpan.nodeDetail.messageView;
    }
    return null;
  }, [detail?.overviewDetail?.messageView, selectedRootTrace, selectedSpan]);

  /**
   * 根据真实 Span ID 选中左侧树节点。
   *
   * @param spanId 真实 Span ID。
   * @returns 无返回值。
   */
  const selectSpanById = (spanId?: string | null) => {
    const nextNode = findPreferredSpanTreeNodeBySourceSpanId(detail?.spanTree || [], spanId);
    if (nextNode?.nodeId) {
      setSelectedNodeId(nextNode.nodeId);
    }
  };

  /**
   * 详情抽屉打开时加载 Trace 详情。
   *
   * @returns effect 清理函数。
   */
  useEffect(() => {
    if (!open || !traceId) {
      setDetail(null);
      setSelectedNodeId(null);
      setDetailViewMode('detail');
      return;
    }

    let cancelled = false;
    setLoading(true);
    setDetailViewMode('detail');
    getAgentTraceDetail(traceId)
      .then((result) => {
        if (cancelled) return;
        setDetail(result);
        setSelectedNodeId(resolveInitialSelectedNodeId(result));
      })
      .catch(() => {
        if (cancelled) return;
        messageApi.error('获取 Trace 详情失败');
      })
      .finally(() => {
        if (cancelled) return;
        setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [open, traceId, messageApi]);

  return (
    <ResizableDrawer
      title="智能体跟踪详情"
      defaultWidth={1280}
      minWidth={1100}
      storageKey="agent-trace-detail-drawer-width"
      open={open}
      onClose={onClose}
      className={styles.traceDrawer}
    >
      {contextHolder}
      <Spin spinning={loading}>
        {detail ? (
          <ResizableSplitPane
            className={styles.traceWorkspace}
            defaultStartWidth={470}
            minStartWidth={400}
            maxStartWidth={640}
            minEndWidth={620}
            storageKey="agent-trace-detail-split-width"
            startPane={
              <TraceNavigationPane
                detail={detail}
                selectedNodeId={selectedNodeId}
                onSelectNode={(node) => setSelectedNodeId(node.nodeId || null)}
              />
            }
            endPane={
              <section className={styles.traceDetailPane}>
                <div className={styles.traceDetailViewSwitch}>
                  <Segmented
                    size="small"
                    value={detailViewMode}
                    options={[
                      { label: '消息', value: 'message' },
                      { label: '详情', value: 'detail' },
                    ]}
                    onChange={(value) => setDetailViewMode(value as 'message' | 'detail')}
                  />
                </div>
                <div className={styles.traceDetailViewBody}>
                  {detailViewMode === 'message' ? (
                    <TraceConversationInspector messageView={selectedMessageView} />
                  ) : selectedRootTrace ? (
                    <TraceOverviewInspector detail={detail} />
                  ) : (
                    <TraceSpanInspector
                      selectedSpan={selectedSpan}
                      selectedTreeNode={selectedTreeNode}
                      onSelectSpanId={selectSpanById}
                    />
                  )}
                </div>
              </section>
            }
          />
        ) : (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无 Trace 详情" />
        )}
      </Spin>
    </ResizableDrawer>
  );
};

export default AgentTraceDetailDrawer;
