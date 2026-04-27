import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import { Button, Divider, Empty, Input, Spin, message } from 'antd';
import React, { useCallback, useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';

import { deleteKnowledgeBase, knowledgeBaseList } from '@/api/llm-manage/knowledgeBase';
import type { KnowledgeBaseTypes } from '@/api/llm-manage/knowledgeBase';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { buildKnowledgeBaseDocumentPath } from '@/router/paths';

import KnowledgeBaseCard from './components/KnowledgeBaseCard';
import KnowledgeBaseDrawer from './components/KnowledgeBaseDrawer';
import styles from './index.module.less';

const KNOWLEDGE_BASE_PAGE_SIZE = 50;

const KnowledgeBase: React.FC = () => {
  const navigate = useNavigate();
  const [list, setList] = useState<KnowledgeBaseTypes.KnowledgeBaseListVo[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [total, setTotal] = useState(0);

  const observerTarget = useRef<HTMLDivElement>(null);
  const loadingRef = useRef(false);

  const fetchList = useCallback(
    async (page: number, isReset = false) => {
      if (loadingRef.current) return;
      loadingRef.current = true;
      if (page === 1) setLoading(true);
      else setLoadingMore(true);
      try {
        const res = await knowledgeBaseList({
          pageNum: page,
          pageSize: KNOWLEDGE_BASE_PAGE_SIZE,
          displayName: searchText || undefined,
        });
        const newRows = res?.rows || [];
        setList((prev) => (isReset ? newRows : [...prev, ...newRows]));
        setHasMore(newRows.length === KNOWLEDGE_BASE_PAGE_SIZE);
        setTotal(Number(res?.total || 0));
      } finally {
        loadingRef.current = false;
        if (page === 1) setLoading(false);
        else setLoadingMore(false);
      }
    },
    [searchText],
  );

  useEffect(() => {
    setCurrentPage(1);
    fetchList(1, true);
  }, [fetchList]);

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !loadingRef.current) {
          const nextPage = currentPage + 1;
          setCurrentPage(nextPage);
          fetchList(nextPage, false);
        }
      },
      { threshold: 0.1 },
    );

    if (observerTarget.current) {
      observer.observe(observerTarget.current);
    }

    return () => observer.disconnect();
  }, [hasMore, currentPage, fetchList]);

  const handleAdd = () => {
    setEditId(null);
    setDrawerOpen(true);
  };

  const handleEdit = (item: KnowledgeBaseTypes.KnowledgeBaseListVo) => {
    setEditId(item.id ?? null);
    setDrawerOpen(true);
  };

  const handleDelete = async (id: number) => {
    await deleteKnowledgeBase(id);
    message.success('删除成功');
    setCurrentPage(1);
    fetchList(1, true);
  };

  const handleDrawerClose = () => {
    setDrawerOpen(false);
    setEditId(null);
  };

  const handleSuccess = () => {
    setCurrentPage(1);
    fetchList(1, true);
  };

  const handleSearch = (value: string) => {
    setSearchText(value);
  };

  const handleRefresh = () => {
    setCurrentPage(1);
    fetchList(1, true);
  };

  const handleCardClick = (item: KnowledgeBaseTypes.KnowledgeBaseListVo) => {
    if (item.id) {
      navigate(
        buildKnowledgeBaseDocumentPath({
          knowledgeBaseId: item.id,
          knowledgeBaseName: item.displayName || item.knowledgeName || '',
        }),
      );
    }
  };

  return (
    <PageContainer>
      <div className={styles.container}>
        <div className={styles.toolbar}>
          <div className={styles.toolbarLeft}>
            <Input.Search
              autoComplete={TEXT_INPUT_AUTOCOMPLETE}
              placeholder="搜索知识库"
              allowClear
              onSearch={handleSearch}
              style={{ width: 280 }}
              prefix={<SearchOutlined />}
            />
            <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
              刷新
            </Button>
          </div>
          <PermissionButton
            type="primary"
            icon={<PlusOutlined />}
            access={ADMIN_PERMISSIONS.knowledgeBase.add}
            onClick={handleAdd}
          >
            新增知识库
          </PermissionButton>
        </div>
        <Divider className={styles.divider} />

        {loading ? (
          <div className={styles.loadingWrapper}>
            <Spin size="large" />
          </div>
        ) : list.length === 0 ? (
          <div className={styles.emptyWrapper}>
            <Empty description="暂无知识库" />
          </div>
        ) : (
          <>
            <div className={styles.grid}>
              {list.map((item) => (
                <KnowledgeBaseCard
                  key={item.id}
                  item={item}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  onClick={handleCardClick}
                />
              ))}
            </div>
            {hasMore && (
              <div ref={observerTarget} style={{ textAlign: 'center', padding: '24px 0' }}>
                {loadingMore && <Spin />}
              </div>
            )}
            {!hasMore && list.length > 0 && (
              <div style={{ textAlign: 'center', padding: '24px 0', color: '#999' }}>
                没有更多了
              </div>
            )}
          </>
        )}

        <KnowledgeBaseDrawer
          open={drawerOpen}
          editId={editId}
          onClose={handleDrawerClose}
          onSuccess={handleSuccess}
        />
      </div>
    </PageContainer>
  );
};

export default KnowledgeBase;
