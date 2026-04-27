import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import {
  EditOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import {
  Button,
  Drawer,
  Empty,
  Input,
  message,
  Modal,
  Pagination,
  Spin,
  Switch,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import {
  addChunk,
  getChunkList,
  updateChunkContent,
  updateChunkStatus,
  type KbChunkTypes,
} from '@/api/llm-manage/knowledgeBaseChunk';
import PermissionButton, { NO_PERMISSION_BUTTON_TIP } from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { usePermission } from '@/hooks/usePermission';
import { buildKnowledgeBaseDocumentPath } from '@/router/paths';
import styles from './index.module.less';

type ChunkRecord = KbChunkTypes.ChunkVo;

// stage 状态映射
const STAGE_MAP: Record<string, { text: string; color: string; desc: string }> = {
  PENDING: { text: '待处理', color: 'default', desc: '本地已创建占位切片，等待 AI 处理' },
  STARTED: { text: '已开始', color: 'processing', desc: 'AI 已开始处理切片任务' },
  COMPLETED: { text: '已完成', color: 'success', desc: '切片任务已完成' },
  FAILED: { text: '失败', color: 'error', desc: '切片任务处理失败' },
};

const ChunkList: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { canAccess } = usePermission();
  const documentId = Number(searchParams.get('documentId'));
  const documentName = searchParams.get('documentName') || '切片列表';
  const knowledgeBaseId = searchParams.get('knowledgeBaseId');
  const knowledgeBaseName = searchParams.get('knowledgeBaseName') || '知识库';

  const [messageApi, contextHolder] = message.useMessage();

  // 列表状态
  const [list, setList] = useState<ChunkRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [searchText, setSearchText] = useState('');

  // 展开详情的切片 ID 集合
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set());

  // 状态切换 loading（key 为 chunk id）
  const [statusLoadingMap, setStatusLoadingMap] = useState<Record<number, boolean>>({});

  // 抽屉状态
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerType, setDrawerType] = useState<'add' | 'edit'>('add');
  const [editingRecord, setEditingRecord] = useState<ChunkRecord | null>(null);
  const [chunkContent, setChunkContent] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // 构建返回文档列表页的 URL
  const documentPageUrl = buildKnowledgeBaseDocumentPath({
    knowledgeBaseId: knowledgeBaseId || '',
    knowledgeBaseName,
  });
  const canUpdateChunk = canAccess(ADMIN_PERMISSIONS.kbDocumentChunk.update);

  const fetchList = useCallback(async () => {
    if (!documentId) return;
    setLoading(true);
    try {
      const result = await getChunkList(documentId, {
        pageNum: currentPage,
        pageSize,
      });
      setList(result?.rows ?? []);
      setTotal(Number(result?.total ?? 0));
    } finally {
      setLoading(false);
    }
  }, [documentId, currentPage, pageSize]);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  // 搜索（前端过滤）
  const handleSearch = useCallback((value: string) => {
    setSearchText(value);
    setCurrentPage(1);
  }, []);

  // 分页
  const handlePageChange = useCallback((page: number, size: number) => {
    setCurrentPage(page);
    setPageSize(size);
  }, []);

  // 切换详情展开
  const toggleDetail = useCallback((id: number) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }, []);

  // 状态切换（0=启用，1=禁用）
  const handleStatusChange = useCallback(
    (record: ChunkRecord, checked: boolean) => {
      const targetStatus: 0 | 1 = checked ? 0 : 1;

      if (targetStatus === 1) {
        // 禁用 → 需要二次确认
        const modal = Modal.confirm({
          title: '确认禁用切片',
          content: (
            <span>
              禁用后该切片将<strong>无法被向量搜索到</strong>，确定要禁用吗？
            </span>
          ),
          okText: '确认禁用',
          cancelText: '取消',
          okButtonProps: { danger: true },
          onOk: async () => {
            setStatusLoadingMap((prev) => ({ ...prev, [record.id]: true }));
            try {
              await updateChunkStatus({ id: record.id, status: 1 });
              messageApi.success('已禁用');
            } finally {
              setStatusLoadingMap((prev) => ({ ...prev, [record.id]: false }));
              modal.destroy();
              fetchList();
            }
          },
        });
      } else {
        // 启用 → 也给一个确认弹窗
        const modal = Modal.confirm({
          title: '确认启用切片',
          content: '启用后该切片将重新参与向量搜索，确定要启用吗？',
          okText: '确认启用',
          cancelText: '取消',
          onOk: async () => {
            setStatusLoadingMap((prev) => ({ ...prev, [record.id]: true }));
            try {
              await updateChunkStatus({ id: record.id, status: 0 });
              messageApi.success('已启用');
            } finally {
              setStatusLoadingMap((prev) => ({ ...prev, [record.id]: false }));
              modal.destroy();
              fetchList();
            }
          },
        });
      }
    },
    [messageApi, fetchList],
  );

  // 新增切片
  const handleAddOpen = useCallback(() => {
    setDrawerType('add');
    setEditingRecord(null);
    setChunkContent('');
    setDrawerOpen(true);
  }, []);

  // 修改切片
  const handleEditOpen = useCallback((record: ChunkRecord) => {
    setDrawerType('edit');
    setEditingRecord(record);
    setChunkContent(record.content || '');
    setDrawerOpen(true);
  }, []);

  // 提交
  const handleSubmit = useCallback(async () => {
    const trimmedContent = chunkContent.trim();
    if (!trimmedContent) {
      messageApi.warning('切片内容不能为空');
      return;
    }

    setSubmitting(true);
    try {
      if (drawerType === 'add') {
        await addChunk({ documentId, content: trimmedContent });
        messageApi.success('新增切片提交成功，等待处理中');
      } else {
        if (!editingRecord) return;
        if (editingRecord.content === trimmedContent) {
          messageApi.info('内容未修改');
          setDrawerOpen(false);
          return;
        }
        await updateChunkContent({ id: editingRecord.id, content: trimmedContent });
        messageApi.success('切片修改提交成功，向量重建中');
      }
      setDrawerOpen(false);
      fetchList();
    } catch {
      fetchList();
    } finally {
      setSubmitting(false);
    }
  }, [chunkContent, drawerType, documentId, editingRecord, messageApi, fetchList]);

  // 前端搜索过滤
  const filteredList = searchText
    ? list.filter((item) => item.content?.toLowerCase().includes(searchText.toLowerCase()))
    : list;

  return (
    <PageContainer
      title="切片列表"
      onBack={() => navigate(documentPageUrl)}
      breadcrumb={{
        items: [{ title: '大模型管理' }, { title: '切片列表' }],
      }}
    >
      <div style={{ padding: 24 }}>
        {contextHolder}

        {/* 顶部标题栏 */}
        <div className={styles.pageHeader}>
          <div className={styles.headerLeft}>
            <h2 className={styles.pageTitle}>{documentName}</h2>
          </div>
          <div className={styles.headerRight}>
            <Input.Search
              autoComplete={TEXT_INPUT_AUTOCOMPLETE}
              placeholder="搜索切片"
              allowClear
              onSearch={handleSearch}
              style={{ width: 220 }}
              prefix={<SearchOutlined />}
            />
            <Button icon={<ReloadOutlined />} onClick={fetchList}>
              刷新
            </Button>
            <PermissionButton
              type="primary"
              icon={<PlusOutlined />}
              access={ADMIN_PERMISSIONS.kbDocumentChunk.add}
              onClick={handleAddOpen}
            >
              新增切片
            </PermissionButton>
          </div>
        </div>

        {/* 切片卡片列表 */}
        {loading ? (
          <div className={styles.loadingWrapper}>
            <Spin size="large" />
          </div>
        ) : filteredList.length === 0 ? (
          <div className={styles.emptyWrapper}>
            <Empty description={searchText ? '无匹配切片' : '暂无切片'} />
          </div>
        ) : (
          <div className={styles.chunkList}>
            {filteredList.map((item) => {
              const stageInfo = STAGE_MAP[item.stage] || {
                text: item.stage,
                color: 'default',
                desc: '',
              };
              const isExpanded = expandedIds.has(item.id);
              const isProcessing = item.stage === 'PENDING' || item.stage === 'STARTED';
              const isEnabled = item.status === 0;
              const statusSwitchLoading = !!statusLoadingMap[item.id];

              return (
                <div key={item.id} className={styles.chunkCard}>
                  {/* 卡片头部 */}
                  <div className={styles.chunkHeader}>
                    <div className={styles.chunkMeta}>
                      <span className={styles.chunkIndex}>
                        切片 {String(item.chunkIndex).padStart(2, '0')}
                      </span>
                      <span className={styles.chunkStat}>{item.charCount} 字符</span>
                    </div>
                    <div className={styles.chunkActions}>
                      <Tooltip
                        title={
                          canUpdateChunk
                            ? isEnabled
                              ? '点击可禁用（禁用后该切片无法被向量搜索）'
                              : '点击可启用'
                            : NO_PERMISSION_BUTTON_TIP
                        }
                      >
                        <span>
                          <Switch
                            size="small"
                            checked={isEnabled}
                            loading={statusSwitchLoading}
                            disabled={!canUpdateChunk}
                            checkedChildren="启用"
                            unCheckedChildren="禁用"
                            onChange={(checked) => handleStatusChange(item, checked)}
                          />
                        </span>
                      </Tooltip>
                      <PermissionButton
                        type="text"
                        size="small"
                        icon={<EditOutlined />}
                        access={ADMIN_PERMISSIONS.kbDocumentChunk.update}
                        disabled={isProcessing}
                        onClick={() => handleEditOpen(item)}
                      >
                        编辑
                      </PermissionButton>
                      <Button
                        type="text"
                        size="small"
                        icon={<InfoCircleOutlined />}
                        onClick={() => toggleDetail(item.id)}
                      >
                        {isExpanded ? '收起' : '详情'}
                      </Button>
                    </div>
                  </div>

                  {/* 内容 */}
                  <div className={styles.chunkContent}>
                    <Typography.Paragraph
                      ellipsis={{
                        rows: 4,
                        expandable: 'collapsible',
                        symbol: (expanded: boolean) => (expanded ? '收起' : '展开'),
                      }}
                      style={{ margin: 0, color: 'inherit' }}
                    >
                      {item.content}
                    </Typography.Paragraph>
                  </div>

                  {/* 详情信息（点击详情展开） */}
                  {isExpanded && (
                    <div className={styles.chunkDetail}>
                      <div className={styles.detailItem}>
                        <span className={styles.detailLabel}>状态</span>
                        <Tooltip title={stageInfo.desc}>
                          <Tag color={stageInfo.color}>{stageInfo.text}</Tag>
                        </Tooltip>
                      </div>
                      <div className={styles.detailItem}>
                        <span className={styles.detailLabel}>向量 ID</span>
                        <Typography.Text
                          className={styles.detailValue}
                          copyable={!!item.vectorId}
                          style={{ fontSize: 13 }}
                        >
                          {item.vectorId || '-'}
                        </Typography.Text>
                      </div>
                      <div className={styles.detailItem}>
                        <span className={styles.detailLabel}>更新时间</span>
                        <span className={styles.detailValue}>{item.updatedAt || '-'}</span>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* 分页 */}
        {total > 0 && (
          <div className={styles.paginationWrapper}>
            <Pagination
              current={currentPage}
              pageSize={pageSize}
              total={total}
              onChange={handlePageChange}
              showSizeChanger
              showQuickJumper
              showTotal={(t) => `共 ${t} 条`}
            />
          </div>
        )}

        {/* 编辑 / 新增抽屉 */}
        <Drawer
          title={drawerType === 'add' ? '新增切片' : '编辑切片内容'}
          open={drawerOpen}
          onClose={() => !submitting && setDrawerOpen(false)}
          width={680}
          destroyOnClose
          styles={{
            body: { display: 'flex', flexDirection: 'column', padding: 0, overflow: 'hidden' },
          }}
          footer={
            <div className={styles.drawerFooter}>
              <Button onClick={() => !submitting && setDrawerOpen(false)} disabled={submitting}>
                取消
              </Button>
              <PermissionButton
                type="primary"
                loading={submitting}
                access={
                  drawerType === 'add'
                    ? ADMIN_PERMISSIONS.kbDocumentChunk.add
                    : ADMIN_PERMISSIONS.kbDocumentChunk.update
                }
                onClick={handleSubmit}
              >
                {drawerType === 'add' ? '提交' : '保存'}
              </PermissionButton>
            </div>
          }
        >
          <div className={styles.drawerBody}>
            <div className={styles.drawerTextarea}>
              <Input.TextArea
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                placeholder="请输入切片内容"
                value={chunkContent}
                onChange={(e) => setChunkContent(e.target.value)}
                disabled={submitting}
                showCount
                maxLength={10000}
                style={{ flex: 1, resize: 'none' }}
              />
            </div>
            {drawerType === 'edit' && (
              <Typography.Paragraph
                type="secondary"
                style={{ margin: '12px 24px 0', fontSize: 13, flexShrink: 0 }}
              >
                提示：修改切片内容后，后台会异步重新生成并更新向量。在完成前，切片状态将显示为"处理中"。
              </Typography.Paragraph>
            )}
          </div>
        </Drawer>
      </div>
    </PageContainer>
  );
};

export default ChunkList;
