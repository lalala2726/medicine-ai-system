import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
/**
 * 会话列表组件
 *
 * - 首屏展示 50 条会话（内部按 20 条分页聚合）
 * - 滚动到底部后继续请求，每次追加 20 条
 * - 支持右键菜单：重命名、删除
 * - 支持新建会话、点击切换会话
 */
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import type { ConversationsProps } from '@ant-design/x';
import { Conversations } from '@ant-design/x';
import type { GetProp } from 'antd';
import { Button, Input, Modal, message, Skeleton } from 'antd';
import React, { useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';
import {
  type ConversationItem,
  deleteConversation,
  getConversationList,
  updateConversationTitle,
} from '@/api/agent';
import styles from './index.module.less';

/** 每页数量 */
const PAGE_SIZE = 20;
/** 首屏展示数量 */
const INITIAL_VISIBLE_COUNT = 50;
/** 首屏骨架条数 */
const INITIAL_SKELETON_ROWS = 10;
/** 分页骨架条数 */
const LOAD_MORE_SKELETON_ROWS = 3;

/** 暴露给父组件的方法 */
export interface ConversationListHandle {
  /** 刷新第一页（新建会话后调用） */
  refresh: () => void;
  /** 本地插入新建会话（用于首条消息完成后立即展示） */
  addCreatedConversation: (uuid: string) => void;
}

interface ConversationListProps {
  style?: React.CSSProperties;
  /** 当前选中的会话 UUID */
  activeKey?: string;
  /** 切换会话回调 */
  onConversationChange?: (conversationUuid: string | null) => void;
}

const ConversationList = React.forwardRef<ConversationListHandle, ConversationListProps>(
  ({ style, activeKey, onConversationChange }, ref) => {
    const NEW_CHAT_TITLE = '新聊天';
    const containerStyle: React.CSSProperties = {
      flexShrink: 0,
      minWidth:
        typeof style?.width === 'number' || typeof style?.width === 'string'
          ? style.width
          : undefined,
      ...style,
    };
    const listStyle: React.CSSProperties = {
      width: '100%',
      background: 'transparent',
      borderRadius: style?.borderRadius === undefined ? 0 : style.borderRadius,
    };

    // ---- 列表状态 ----
    const [conversations, setConversations] = useState<ConversationItem[]>([]);
    const [total, setTotal] = useState(0);
    const [loading, setLoading] = useState(false);
    const [initialLoading, setInitialLoading] = useState(true);
    const containerRef = useRef<HTMLDivElement>(null);
    // 用 ref 跟踪 loading，避免 useCallback 依赖 loading state 导致频繁重建
    const loadingRef = useRef(false);
    const totalRef = useRef(total);
    const visibleCountRef = useRef(0);
    const nextPageRef = useRef(1);
    // 首屏可能一次性聚合超过 50 条，剩余条目先放缓冲区，后续滚动再消费
    const pendingRowsRef = useRef<ConversationItem[]>([]);

    // ---- 重命名弹窗状态 ----
    const [renameModalOpen, setRenameModalOpen] = useState(false);
    const [renameTarget, setRenameTarget] = useState<ConversationItem | null>(null);
    const [renameValue, setRenameValue] = useState('');
    const [renameLoading, setRenameLoading] = useState(false);

    /** 是否还有更多数据可加载 */
    const hasMore = conversations.length < total;
    const isEmpty = !initialLoading && conversations.length === 0;
    const showLoadMoreSkeleton = loading && !initialLoading && conversations.length > 0;
    const hasMoreRef = useRef(hasMore);
    useEffect(() => {
      hasMoreRef.current = hasMore;
    }, [hasMore]);
    useEffect(() => {
      visibleCountRef.current = conversations.length;
    }, [conversations.length]);
    useEffect(() => {
      totalRef.current = total;
    }, [total]);

    // ======================== 数据加载 ========================

    /** 从缓冲区取出指定数量 */
    const consumePendingRows = useCallback((count: number): ConversationItem[] => {
      if (count <= 0 || pendingRowsRef.current.length === 0) return [];
      const rows = pendingRowsRef.current.slice(0, count);
      pendingRowsRef.current = pendingRowsRef.current.slice(count);
      return rows;
    }, []);

    /** 首次加载或刷新：首屏聚合到 50 条 */
    const loadInitialConversations = useCallback(async () => {
      if (loadingRef.current) return;
      loadingRef.current = true;
      setLoading(true);
      try {
        const allRows: ConversationItem[] = [];
        let page = 1;
        let serverTotal = 0;

        while (allRows.length < INITIAL_VISIBLE_COUNT) {
          const res = await getConversationList(page, PAGE_SIZE);
          const rows = res.rows ?? [];
          serverTotal = res.total ?? 0;

          if (rows.length === 0) break;
          allRows.push(...rows);
          page += 1;

          if (allRows.length >= serverTotal) break;
        }

        const visibleRows = allRows.slice(0, INITIAL_VISIBLE_COUNT);
        const pendingRows = allRows.slice(INITIAL_VISIBLE_COUNT);
        const knownTotal = Math.max(serverTotal, allRows.length);

        nextPageRef.current = page;
        pendingRowsRef.current = pendingRows;
        totalRef.current = knownTotal;
        setConversations(visibleRows);
        setTotal(knownTotal);
      } catch (err) {
        console.error('加载会话列表失败:', err);
      } finally {
        loadingRef.current = false;
        setLoading(false);
        setInitialLoading(false);
      }
    }, []);

    /** 继续加载：每次追加 20 条 */
    const fetchMoreConversations = useCallback(async () => {
      if (loadingRef.current || !hasMoreRef.current) return;

      loadingRef.current = true;
      setLoading(true);
      try {
        const chunk = consumePendingRows(PAGE_SIZE);

        while (
          chunk.length < PAGE_SIZE &&
          visibleCountRef.current + chunk.length < totalRef.current
        ) {
          const res = await getConversationList(nextPageRef.current, PAGE_SIZE);
          nextPageRef.current += 1;

          const rows = res.rows ?? [];
          if (rows.length === 0) break;

          const need = PAGE_SIZE - chunk.length;
          chunk.push(...rows.slice(0, need));
          if (rows.length > need) {
            pendingRowsRef.current = [...rows.slice(need), ...pendingRowsRef.current];
          }

          const knownTotal = Math.max(
            res.total ?? 0,
            visibleCountRef.current + chunk.length + pendingRowsRef.current.length,
          );
          if (knownTotal !== totalRef.current) {
            totalRef.current = knownTotal;
            setTotal(knownTotal);
          }
        }

        if (chunk.length > 0) {
          setConversations((prev) => [...prev, ...chunk]);
        } else {
          const alignedTotal = visibleCountRef.current;
          if (alignedTotal < totalRef.current) {
            totalRef.current = alignedTotal;
            setTotal(alignedTotal);
          }
        }
      } catch (err) {
        console.error('加载更多会话失败:', err);
      } finally {
        loadingRef.current = false;
        setLoading(false);
      }
    }, [consumePendingRows]);

    /** 本地 upsert 会话并置顶（新会话默认标题固定为“新聊天”） */
    const upsertCreatedConversation = useCallback((uuid: string) => {
      const existsInPending = pendingRowsRef.current.some(
        (item) => item.conversation_uuid === uuid,
      );
      pendingRowsRef.current = pendingRowsRef.current.filter(
        (item) => item.conversation_uuid !== uuid,
      );

      setConversations((prev) => {
        const index = prev.findIndex((item) => item.conversation_uuid === uuid);

        if (index === 0) {
          if (prev[0].title === NEW_CHAT_TITLE) return prev;
          return [{ ...prev[0], title: NEW_CHAT_TITLE }, ...prev.slice(1)];
        }

        if (index > 0) {
          const target = prev[index];
          const remain = prev.filter((_, i) => i !== index);
          return [{ ...target, title: NEW_CHAT_TITLE }, ...remain];
        }

        // 新会话不在可见列表和缓冲区中，需要更新 total
        if (!existsInPending) {
          setTotal((prevTotal) => {
            const next = prevTotal + 1;
            totalRef.current = next;
            return next;
          });
        }

        return [{ conversation_uuid: uuid, title: NEW_CHAT_TITLE }, ...prev];
      });
    }, []);

    /** 首次加载 */
    useEffect(() => {
      void loadInitialConversations();
    }, [loadInitialConversations]);

    /** 暴露给父组件的方法 */
    useImperativeHandle(
      ref,
      () => ({
        refresh: () => {
          void loadInitialConversations();
        },
        addCreatedConversation: (uuid: string) => upsertCreatedConversation(uuid),
      }),
      [loadInitialConversations, upsertCreatedConversation],
    );

    /** 滚动到底部加载更多（使用 ref 读取最新值，避免 stale closure） */
    const handleScroll = useCallback(() => {
      const el = containerRef.current;
      if (!el || loadingRef.current || !hasMoreRef.current) return;
      if (el.scrollHeight - el.scrollTop - el.clientHeight < 50) {
        void fetchMoreConversations();
      }
    }, [fetchMoreConversations]);

    useEffect(() => {
      const el = containerRef.current;
      if (!el) return;
      el.addEventListener('scroll', handleScroll, { passive: true });
      return () => el.removeEventListener('scroll', handleScroll);
    }, [handleScroll]);

    // ======================== 操作 ========================

    /** 新建会话 */
    const handleNewChat = () => {
      onConversationChange?.(null);
    };

    /** 切换会话 */
    const handleActiveChange: ConversationsProps['onActiveChange'] = (key) => {
      onConversationChange?.(key as string);
    };

    /** 删除会话 */
    const handleDelete = async (item: ConversationItem) => {
      Modal.confirm({
        title: '删除会话',
        content: `确定要删除会话「${item.title}」吗？`,
        okText: '确定',
        cancelText: '取消',
        okButtonProps: { danger: true },
        onOk: async () => {
          try {
            await deleteConversation(item.conversation_uuid);
            message.success('删除成功');
            pendingRowsRef.current = pendingRowsRef.current.filter(
              (c) => c.conversation_uuid !== item.conversation_uuid,
            );
            setConversations((prev) =>
              prev.filter((c) => c.conversation_uuid !== item.conversation_uuid),
            );
            setTotal((prev) => {
              const next = Math.max(prev - 1, 0);
              totalRef.current = next;
              return next;
            });
            if (activeKey === item.conversation_uuid) {
              onConversationChange?.(null);
            }
          } catch (err) {
            console.error('删除会话失败:', err);
          }
        },
      });
    };

    /** 打开重命名弹窗 */
    const handleRenameOpen = (item: ConversationItem) => {
      setRenameTarget(item);
      setRenameValue(item.title);
      setRenameModalOpen(true);
    };

    /** 提交重命名 */
    const handleRenameOk = async () => {
      if (!renameTarget || !renameValue.trim()) return;
      setRenameLoading(true);
      try {
        await updateConversationTitle(renameTarget.conversation_uuid, renameValue.trim());
        message.success('重命名成功');
        setConversations((prev) =>
          prev.map((c) =>
            c.conversation_uuid === renameTarget.conversation_uuid
              ? { ...c, title: renameValue.trim() }
              : c,
          ),
        );
        setRenameModalOpen(false);
      } catch (err) {
        console.error('重命名失败:', err);
      } finally {
        setRenameLoading(false);
      }
    };

    // ======================== 菜单配置 ========================

    /** 为每个会话生成右键操作菜单 */
    const menuConfig: ConversationsProps['menu'] = (conversation) => {
      const raw = conversations.find((c) => c.conversation_uuid === conversation.key);
      if (!raw) return undefined;
      return {
        items: [
          {
            key: 'rename',
            label: '重命名',
            icon: <EditOutlined />,
          },
          {
            key: 'delete',
            label: '删除',
            icon: <DeleteOutlined />,
            danger: true,
          },
        ],
        onClick: ({ key }: { key: string }) => {
          if (key === 'rename') handleRenameOpen(raw);
          if (key === 'delete') handleDelete(raw);
        },
      };
    };

    // ======================== 数据映射 ========================

    /** 将远程数据映射为 Conversations 组件的 items 格式 */
    const items: GetProp<ConversationsProps, 'items'> = conversations.map((c) => ({
      key: c.conversation_uuid,
      label: c.title,
    }));
    const containerClassName = [
      styles.scrollContainer,
      isEmpty ? styles.scrollContainerNoScroll : '',
    ]
      .filter(Boolean)
      .join(' ');

    const renderSkeleton = (rows: number) => (
      <div className={styles.skeletonList} aria-hidden>
        {Array.from({ length: rows }).map((_, index) => (
          <Skeleton.Input
            // biome-ignore lint/suspicious/noArrayIndexKey: skeleton placeholder has no stable data key.
            key={`conversation-skeleton-${index}`}
            active
            block
            size="small"
            className={styles.skeletonRow}
          />
        ))}
      </div>
    );

    return (
      <>
        <div className={styles.conversationWrapper} style={containerStyle}>
          {/* ---- 固定在顶部的新会话按钮 ---- */}
          <div className={styles.newChatButtonWrapper}>
            <Button
              icon={<PlusOutlined />}
              onClick={handleNewChat}
              className={styles.newChatButton}
            >
              新会话
            </Button>
          </div>

          {/* ---- 会话列表（可滚动） ---- */}
          <div ref={containerRef} className={containerClassName} data-conversation-list>
            {initialLoading ? (
              <div className={styles.loadingContainer}>{renderSkeleton(INITIAL_SKELETON_ROWS)}</div>
            ) : (
              <div className={styles.content}>
                <Conversations
                  className={styles.container}
                  items={items}
                  activeKey={activeKey}
                  onActiveChange={handleActiveChange}
                  menu={menuConfig}
                  style={listStyle}
                />
                {isEmpty && <div className={styles.emptyState}>暂时没有会话</div>}
                {showLoadMoreSkeleton && (
                  <div className={styles.loadMore}>{renderSkeleton(LOAD_MORE_SKELETON_ROWS)}</div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* ---- 重命名弹窗 ---- */}
        <Modal
          title="重命名会话"
          open={renameModalOpen}
          onOk={handleRenameOk}
          onCancel={() => setRenameModalOpen(false)}
          confirmLoading={renameLoading}
          okText="确定"
          cancelText="取消"
          destroyOnHidden
          centered
        >
          <Input
            autoComplete={TEXT_INPUT_AUTOCOMPLETE}
            value={renameValue}
            onChange={(e) => setRenameValue(e.target.value)}
            placeholder="请输入新的会话标题"
            maxLength={100}
            onPressEnter={handleRenameOk}
            autoFocus
          />
        </Modal>
      </>
    );
  },
);

ConversationList.displayName = 'ConversationList';

export default ConversationList;
