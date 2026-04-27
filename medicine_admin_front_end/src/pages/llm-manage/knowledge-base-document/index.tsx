import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import {
  DeleteOutlined,
  DownloadOutlined,
  DownOutlined,
  EditOutlined,
  EyeOutlined,
  FileExcelOutlined,
  FilePdfOutlined,
  FilePptOutlined,
  FileTextOutlined,
  FileUnknownOutlined,
  FileWordOutlined,
  ImportOutlined,
  SearchOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Button,
  Dropdown,
  Input,
  message,
  Modal,
  Pagination,
  Select,
  Space,
  Tag,
  Typography,
  Descriptions,
  Drawer,
} from 'antd';
import React, { useCallback, useMemo, useRef, useState } from 'react';

import {
  deleteDocuments,
  getDocumentList,
  renameDocument,
  getDocumentById,
} from '@/api/llm-manage/knowledgeBase';
import type { KbDocumentTypes } from '@/api/llm-manage/knowledgeBase';
import PermissionButton from '@/components/PermissionButton';
import TableActionGroup from '@/components/TableActionGroup';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { usePermission } from '@/hooks/usePermission';
import {
  buildKnowledgeBaseChunkPath,
  buildKnowledgeBaseImportPath,
  buildKnowledgeBaseSearchPath,
  routePaths,
} from '@/router/paths';
import type { CaptchaVerificationResult } from '@/api/core/captcha';
import { useThemeContext } from '@/contexts/ThemeContext';
import SliderCaptchaModal from '@/pages/login/components/SliderCaptchaModal';

type DocumentRecord = KbDocumentTypes.DocumentVo;

// ==================== 工具函数 ====================

/**
 * 字节转 MB，最小显示 0.01MB
 */
function formatFileSize(bytes: number | null | undefined): string {
  if (bytes == null || bytes <= 0) return '-';
  const mb = bytes / (1024 * 1024);
  if (mb < 0.01) return '0.01M';
  return `${mb.toFixed(2)}M`;
}

/**
 * 通过 URL 触发浏览器强制下载。
 * 先用 fetch 将文件内容拉回本地转成 Blob URL，
 * 再以 <a download> 触发下载，从而绕过跨域资源（OSS/CDN）
 * 导致浏览器直接预览而非下载的问题。
 */
async function downloadByUrl(url: string, fileName?: string): Promise<void> {
  const fallbackDownload = () => {
    // fetch 失败时回退：直接新标签打开，让浏览器自行处理保存
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName || 'download';
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  try {
    const response = await fetch(url, { mode: 'cors' });
    if (!response.ok) {
      fallbackDownload();
      return;
    }
    const blob = await response.blob();
    const blobUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = fileName || 'download';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    // 稍后释放临时 Blob URL，避免内存泄漏
    setTimeout(() => URL.revokeObjectURL(blobUrl), 10_000);
  } catch {
    fallbackDownload();
  }
}

/**
 * 根据文件类型返回对应图标
 */
function getFileIcon(fileType?: string) {
  if (!fileType) return <FileUnknownOutlined style={{ fontSize: 24, color: '#8c8c8c' }} />;
  const type = fileType.toLowerCase();

  if (type === 'pdf') return <FilePdfOutlined style={{ fontSize: 24, color: '#ff4d4f' }} />;
  if (type === 'word') return <FileWordOutlined style={{ fontSize: 24, color: '#1677ff' }} />;
  if (type === 'ppt') return <FilePptOutlined style={{ fontSize: 24, color: '#fa8c16' }} />;
  if (type === 'excel') return <FileExcelOutlined style={{ fontSize: 24, color: '#52c41a' }} />;
  if (type === 'text') return <FileTextOutlined style={{ fontSize: 24, color: '#8c8c8c' }} />;
  if (type === 'markdown') return <FileTextOutlined style={{ fontSize: 24, color: '#1890ff' }} />;

  return <FileUnknownOutlined style={{ fontSize: 24, color: '#8c8c8c' }} />;
}

// 文件类型筛选选项
const FILE_TYPE_OPTIONS = [
  { label: '全部文件', value: '' },
  { label: 'PDF', value: 'pdf' },
  { label: 'Word', value: 'word' },
  { label: 'PPT', value: 'ppt' },
  { label: 'Excel', value: 'excel' },
  { label: 'Text', value: 'text' },
  { label: 'Markdown', value: 'markdown' },
];

// stage 状态映射
const STAGE_MAP: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待处理', color: 'default' },
  STARTED: { text: '处理中', color: 'processing' },
  PROCESSING: { text: '处理中', color: 'processing' },
  INSERTING: { text: '入库中', color: 'processing' },
  COMPLETED: { text: '数据完成', color: 'success' },
  FAILED: { text: '失败', color: 'error' },
};

import styles from './index.module.less';

const KnowledgeBaseDocument: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { isDark } = useThemeContext();
  const { canAccess } = usePermission();
  const knowledgeBaseId = Number(searchParams.get('id'));
  const knowledgeBaseName = searchParams.get('name') || '知识库';

  const actionRef = useRef<ActionType | null>(null);
  const [messageApi, contextHolder] = message.useMessage();
  const [selectedRows, setSelectedRows] = useState<DocumentRecord[]>([]);
  const [fileTypeFilter, setFileTypeFilter] = useState('');
  const [renameRecordId, setRenameRecordId] = useState<number | null>(null);
  const [renameValue, setRenameValue] = useState('');
  const [renameLoading, setRenameLoading] = useState(false);

  // 详情弹窗
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [detailRecord, setDetailRecord] = useState<DocumentRecord | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  // 文件预览验证码
  const [pendingPreviewUrl, setPendingPreviewUrl] = useState('');
  const [previewCaptchaOpen, setPreviewCaptchaOpen] = useState(false);

  // 查看详情
  const handleDetailOpen = useCallback(async (record: DocumentRecord) => {
    setDetailModalOpen(true);
    setDetailLoading(true);
    try {
      const data = await getDocumentById(record.id);
      setDetailRecord(data);
    } catch {
      // 错误已由 request 拦截器处理
      setDetailModalOpen(false);
    } finally {
      setDetailLoading(false);
    }
  }, []);

  // 分页 refs（request 回调中始终取到最新值，避免闭包陈旧问题）
  const currentPageRef = useRef(1);
  const pageSizeRef = useRef(10);
  const searchTextRef = useRef('');
  const fileTypeFilterRef = useRef('');
  // 分页展示状态（驱动底部 Pagination 组件渲染）
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  // 搜索
  const handleSearch = useCallback((value: string) => {
    searchTextRef.current = value;
    currentPageRef.current = 1;
    setCurrentPage(1);
    actionRef.current?.reload();
  }, []);

  // 文件类型筛选变化
  const handleFileTypeChange = useCallback((value: string) => {
    fileTypeFilterRef.current = value;
    setFileTypeFilter(value);
    currentPageRef.current = 1;
    setCurrentPage(1);
    actionRef.current?.reload();
  }, []);

  // 分页变化
  const handlePageChange = useCallback((page: number, size: number) => {
    currentPageRef.current = page;
    pageSizeRef.current = size;
    setCurrentPage(page);
    setPageSize(size);
    actionRef.current?.reload();
  }, []);

  // 下载单个文件
  const handleDownload = useCallback(async (record: DocumentRecord) => {
    if (!record.fileUrl) {
      message.warning('文件地址不存在');
      return;
    }
    await downloadByUrl(record.fileUrl, record.fileName);
  }, []);

  // 批量下载
  const handleBatchDownload = useCallback(async () => {
    if (!selectedRows.length) {
      messageApi.warning('请选择要下载的文件');
      return;
    }
    for (const row of selectedRows) {
      if (row.fileUrl) {
        await downloadByUrl(row.fileUrl, row.fileName);
      }
    }
    setSelectedRows([]); // 下载完成后自动取消全选
  }, [selectedRows, messageApi]);

  // 删除文档
  const handleDelete = useCallback(
    (records: DocumentRecord[]) => {
      if (!records.length) {
        messageApi.warning('请选择要删除的文件');
        return;
      }
      Modal.confirm({
        title: '确认删除',
        content: `确定要删除选中的 ${records.length} 个文档吗？删除后不可恢复。`,
        okText: '确认',
        cancelText: '取消',
        okButtonProps: { danger: true },
        onOk: async () => {
          const ids = records.map((r) => r.id);
          await deleteDocuments({ documentIds: ids });
          messageApi.success('删除成功');
          setSelectedRows([]);
          actionRef.current?.reload();
        },
      });
    },
    [messageApi],
  );

  // 重命名
  const handleRenameOpen = useCallback((record: DocumentRecord) => {
    if (!record.id) return;
    setRenameRecordId(record.id);
    setRenameValue(record.fileName || '');
  }, []);

  const handleRenameConfirm = useCallback(async () => {
    if (!renameRecordId) return;
    const trimmed = renameValue.trim();
    if (!trimmed) {
      messageApi.warning('文件名不能为空');
      return;
    }
    setRenameLoading(true);
    try {
      await renameDocument({ id: renameRecordId, fileName: trimmed });
      messageApi.success('重命名成功');
      setRenameRecordId(null);
      actionRef.current?.reload();
    } finally {
      setRenameLoading(false);
    }
  }, [renameRecordId, renameValue, messageApi]);

  // 预览
  const handlePreview = useCallback((record: DocumentRecord) => {
    if (!record.fileUrl) {
      message.warning('文件地址不存在');
      return;
    }
    const type = (record.fileType || '').toLowerCase();
    const officeTypes = ['word', 'ppt', 'excel'];

    // PDF / 纯文本 / Markdown：直接在浏览器新标签页预览
    if (type === 'pdf' || ['text', 'markdown'].includes(type)) {
      window.open(record.fileUrl, '_blank', 'noopener,noreferrer');
      return;
    }

    // Office 文档：先警告再滑块验证后跳转微软在线预览
    if (officeTypes.includes(type)) {
      const officePreviewUrl = `https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(record.fileUrl)}`;
      Modal.confirm({
        title: '外部服务预览提示',
        content:
          '该文档将通过微软官方在线预览服务（Office Online）进行渲染，文件内容会经由微软服务器处理。若文档包含敏感或机密信息，建议使用「下载」功能在本地查看。',
        okText: '继续预览',
        cancelText: '取消',
        centered: true,
        onOk: () => {
          setPendingPreviewUrl(officePreviewUrl);
          setPreviewCaptchaOpen(true);
        },
      });
      return;
    }

    // 未知类型：提示不支持在线预览
    message.info('该文件类型暂不支持在线预览，请下载后查看');
  }, []);

  /**
   * 预览滑块验证码取消。
   */
  const handlePreviewCaptchaCancel = useCallback(() => {
    setPreviewCaptchaOpen(false);
    setPendingPreviewUrl('');
  }, []);

  /**
   * 预览滑块验证码通过后跳转。
   */
  const handlePreviewCaptchaVerified = useCallback(
    (_result: CaptchaVerificationResult) => {
      setPreviewCaptchaOpen(false);
      if (pendingPreviewUrl) {
        window.open(pendingPreviewUrl, '_blank', 'noopener,noreferrer');
      }
      setPendingPreviewUrl('');
    },
    [pendingPreviewUrl],
  );

  const columns: ProColumns<DocumentRecord>[] = useMemo(
    () => [
      {
        title: '文件名',
        dataIndex: 'fileName',
        ellipsis: true,
        width: 320,
        render: (_, record) => (
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {getFileIcon(record.fileType)}
            <div style={{ minWidth: 0, flex: 1 }}>
              {renameRecordId === record.id ? (
                <Input
                  autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                  value={renameValue}
                  onChange={(e) => setRenameValue(e.target.value)}
                  onPressEnter={handleRenameConfirm}
                  onBlur={handleRenameConfirm}
                  disabled={renameLoading}
                  autoFocus
                  style={{ display: 'block' }}
                />
              ) : (
                <>
                  <Typography.Text
                    ellipsis={{ tooltip: record.fileName }}
                    style={{ display: 'block', fontWeight: 500 }}
                  >
                    {record.fileName}
                  </Typography.Text>
                  <Typography.Text
                    type="secondary"
                    style={{ fontSize: 12, display: 'block', marginTop: 2 }}
                    copyable={{ text: String(record.id), tooltips: ['复制 ID', '已复制'] }}
                  >
                    ID: {record.id}
                  </Typography.Text>
                </>
              )}
            </div>
          </div>
        ),
      },
      {
        title: '文件大小',
        dataIndex: 'fileSize',
        width: 100,
        render: (_, record) => formatFileSize(record.fileSize),
      },
      {
        title: '切片数量',
        dataIndex: 'chunkCount',
        width: 100,
      },
      {
        title: '文件状态',
        dataIndex: 'stage',
        width: 100,
        render: (_, record) => {
          const stageInfo = STAGE_MAP[record.stage] || {
            text: record.stage,
            color: 'default',
          };
          return <Tag color={stageInfo.color}>{stageInfo.text}</Tag>;
        },
      },
      {
        title: '上传时间',
        dataIndex: 'createdAt',
        width: 170,
      },
      {
        title: '更新时间',
        dataIndex: 'updatedAt',
        width: 170,
      },
      {
        title: '操作',
        dataIndex: 'option',
        valueType: 'option',
        width: 180,
        fixed: 'right',
        align: 'center',
        render: (_, record) => (
          <TableActionGroup>
            <PermissionButton
              type="link"
              size="small"
              access={ADMIN_PERMISSIONS.kbDocumentChunk.list}
              onClick={() => {
                navigate(
                  buildKnowledgeBaseChunkPath({
                    documentId: record.id,
                    documentName: record.fileName || '',
                    knowledgeBaseId,
                    knowledgeBaseName,
                  }),
                );
              }}
            >
              切片
            </PermissionButton>
            <PermissionButton
              type="link"
              size="small"
              access={ADMIN_PERMISSIONS.kbDocument.query}
              onClick={() => handleDetailOpen(record)}
            >
              详情
            </PermissionButton>
            <Dropdown
              menu={{
                items: [
                  {
                    key: 'preview',
                    label: '预览',
                    icon: <EyeOutlined />,
                    onClick: () => handlePreview(record),
                  },
                  {
                    key: 'download',
                    label: '下载',
                    icon: <DownloadOutlined />,
                    onClick: () => handleDownload(record),
                  },
                  {
                    key: 'rename',
                    label: '重命名',
                    icon: <EditOutlined />,
                    disabled: !canAccess(ADMIN_PERMISSIONS.kbDocument.update),
                    onClick: () => handleRenameOpen(record),
                  },
                  {
                    key: 'delete',
                    label: '删除',
                    icon: <DeleteOutlined />,
                    danger: true,
                    disabled: !canAccess(ADMIN_PERMISSIONS.kbDocument.delete),
                    onClick: () => handleDelete([record]),
                  },
                ],
              }}
              trigger={['click']}
            >
              <Button type="link" size="small">
                更多 <DownOutlined />
              </Button>
            </Dropdown>
          </TableActionGroup>
        ),
      },
    ],
    [
      handleDownload,
      handleDelete,
      handleRenameOpen,
      handlePreview,
      handleDetailOpen,
      renameRecordId,
      renameValue,
      renameLoading,
      handleRenameConfirm,
      canAccess,
      knowledgeBaseId,
      knowledgeBaseName,
      navigate,
    ],
  );

  // 批量管理下拉菜单
  const batchMenuItems = useMemo(
    () => [
      {
        key: 'batchDelete',
        label: '删除',
        icon: <DeleteOutlined />,
        danger: true,
        disabled: !canAccess(ADMIN_PERMISSIONS.kbDocument.delete),
        onClick: () => handleDelete(selectedRows),
      },
      {
        key: 'batchDownload',
        label: '下载',
        icon: <DownloadOutlined />,
        onClick: handleBatchDownload,
      },
    ],
    [selectedRows, handleDelete, handleBatchDownload, canAccess],
  );

  return (
    <PageContainer
      title={knowledgeBaseName}
      onBack={() => navigate(routePaths.llmKnowledgeBase)}
      breadcrumb={{
        items: [{ title: '大模型管理' }, { title: knowledgeBaseName }],
      }}
      extra={[
        <PermissionButton
          key="import"
          type="primary"
          icon={<ImportOutlined />}
          access={ADMIN_PERMISSIONS.kbDocument.import}
          onClick={() =>
            navigate(
              buildKnowledgeBaseImportPath({
                knowledgeBaseId,
                knowledgeBaseName,
              }),
            )
          }
        >
          导入知识
        </PermissionButton>,
        <PermissionButton
          key="search"
          icon={<SearchOutlined />}
          access={ADMIN_PERMISSIONS.knowledgeBase.query}
          onClick={() =>
            navigate(
              buildKnowledgeBaseSearchPath({
                knowledgeBaseId,
                knowledgeBaseName,
              }),
            )
          }
        >
          知识检索
        </PermissionButton>,
      ]}
    >
      <div className={styles.container}>
        {contextHolder}

        <ProTable<DocumentRecord>
          actionRef={actionRef}
          rowKey="id"
          search={false}
          options={false}
          headerTitle={
            <Space>
              <Input.Search
                autoComplete={TEXT_INPUT_AUTOCOMPLETE}
                placeholder="输入文件名称/标签搜索"
                allowClear
                onSearch={handleSearch}
                style={{ width: 220 }}
              />
              <Button
                icon={<ReloadOutlined />}
                onClick={() => actionRef.current?.reload()}
                title="刷新"
              />
              <Select
                value={fileTypeFilter}
                onChange={handleFileTypeChange}
                options={FILE_TYPE_OPTIONS}
                style={{ width: 140 }}
              />
              <Dropdown
                menu={{ items: batchMenuItems }}
                disabled={selectedRows.length === 0}
                trigger={['click']}
              >
                <Button>
                  批量管理 <DownOutlined />
                </Button>
              </Dropdown>
            </Space>
          }
          toolbar={{
            search: false,
          }}
          request={async () => {
            const fileType = fileTypeFilterRef.current;
            const queryFileType = fileType ? fileType.split(',')[0] : undefined;
            const result = await getDocumentList(knowledgeBaseId, {
              pageNum: currentPageRef.current,
              pageSize: pageSizeRef.current,
              fileName: searchTextRef.current || undefined,
              fileType: queryFileType,
            });
            const t = Number(result?.total ?? 0);
            setTotal(t);
            return {
              data: result?.rows ?? [],
              success: true,
              total: t,
            };
          }}
          columns={columns}
          pagination={false}
          rowSelection={{
            onChange: (_, rows) => setSelectedRows(rows),
            selectedRowKeys: selectedRows.map((r) => r.id),
          }}
        />
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
      </div>

      {/* 详情抽屉 */}
      <Drawer
        title="文档详情"
        open={detailModalOpen}
        onClose={() => setDetailModalOpen(false)}
        width={700}
        destroyOnHidden
        loading={detailLoading}
      >
        {detailRecord && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="文档 ID" span={2}>
              {detailRecord.id}
            </Descriptions.Item>
            <Descriptions.Item label="文件名" span={2}>
              {detailRecord.fileName}
            </Descriptions.Item>
            <Descriptions.Item label="文件类型">{detailRecord.fileType}</Descriptions.Item>
            <Descriptions.Item label="文件大小">
              {formatFileSize(detailRecord.fileSize)}
            </Descriptions.Item>
            <Descriptions.Item label="切片模式">{detailRecord.chunkMode || '-'}</Descriptions.Item>
            <Descriptions.Item label="切片数量">{detailRecord.chunkCount ?? 0}</Descriptions.Item>
            <Descriptions.Item label="切片长度">{detailRecord.chunkSize ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="切片重叠长度">
              {detailRecord.chunkOverlap ?? '-'}
            </Descriptions.Item>
            <Descriptions.Item label="处理状态">
              {STAGE_MAP[detailRecord.stage]?.text || detailRecord.stage}
            </Descriptions.Item>
            <Descriptions.Item label="错误信息" span={2}>
              {detailRecord.lastError ? (
                <Typography.Text type="danger">{detailRecord.lastError}</Typography.Text>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="文件 URL" span={2}>
              <Typography.Text
                ellipsis={{ tooltip: detailRecord.fileUrl }}
                copyable={!!detailRecord.fileUrl}
              >
                {detailRecord.fileUrl || '-'}
              </Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="创建人">{detailRecord.createBy || '-'}</Descriptions.Item>
            <Descriptions.Item label="最后更新人">{detailRecord.updateBy || '-'}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{detailRecord.createdAt}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{detailRecord.updatedAt}</Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>

      {/* Office 文档预览滑块验证码 */}
      <SliderCaptchaModal
        open={previewCaptchaOpen}
        onCancel={handlePreviewCaptchaCancel}
        onVerified={handlePreviewCaptchaVerified}
        isDark={isDark}
      />
    </PageContainer>
  );
};

export default KnowledgeBaseDocument;
