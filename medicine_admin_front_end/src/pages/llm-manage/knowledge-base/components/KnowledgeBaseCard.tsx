import { CopyOutlined, DeleteOutlined, EditOutlined, MoreOutlined } from '@ant-design/icons';
import { Dropdown, message, Popconfirm, Tooltip, Typography } from 'antd';
import React from 'react';

import type { KnowledgeBaseTypes } from '@/api/llm-manage/knowledgeBase';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';
import { usePermission } from '@/hooks/usePermission';
import styles from './KnowledgeBaseCard.module.less';

const { Paragraph } = Typography;

interface KnowledgeBaseCardProps {
  item: KnowledgeBaseTypes.KnowledgeBaseListVo;
  onEdit: (item: KnowledgeBaseTypes.KnowledgeBaseListVo) => void;
  onDelete: (id: number) => void;
  onClick?: (item: KnowledgeBaseTypes.KnowledgeBaseListVo) => void;
}

const KnowledgeBaseCard: React.FC<KnowledgeBaseCardProps> = ({
  item,
  onEdit,
  onDelete,
  onClick,
}) => {
  const { canAccess } = usePermission();
  const canUpdateKnowledgeBase = canAccess(ADMIN_PERMISSIONS.knowledgeBase.update);
  const canDeleteKnowledgeBase = canAccess(ADMIN_PERMISSIONS.knowledgeBase.delete);

  const handleCopyId = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (item.id) {
      navigator.clipboard.writeText(String(item.id));
      message.success('已复制知识库 ID');
    }
  };

  const menuItems = [
    {
      key: 'edit',
      label: '编辑',
      icon: <EditOutlined />,
      disabled: !canUpdateKnowledgeBase,
      onClick: (info: any) => {
        info.domEvent.stopPropagation();
        onEdit(item);
      },
    },
    {
      key: 'delete',
      label: (
        <Popconfirm
          title="确认删除"
          description={`确定要删除知识库「${item.displayName}」吗？`}
          onConfirm={(e) => {
            e?.stopPropagation();
            if (item.id) onDelete(item.id);
          }}
          onCancel={(e) => e?.stopPropagation()}
          okText="确认"
          cancelText="取消"
        >
          <span onClick={(e) => e.stopPropagation()}>删除</span>
        </Popconfirm>
      ),
      icon: <DeleteOutlined />,
      danger: true,
      disabled: !canDeleteKnowledgeBase,
    },
  ];

  const formatDateTime = (dateStr?: string) => {
    if (!dateStr) return '-';
    // 只显示日期部分，让卡片看起来更整洁
    return dateStr.split('T')[0];
  };

  return (
    <div className={styles.card} onClick={() => onClick?.(item)}>
      <div className={styles.header}>
        {item.cover ? (
          <img className={styles.cover} src={item.cover} alt={item.displayName} />
        ) : (
          <div className={styles.coverPlaceholder}>{(item.displayName || '知')[0]}</div>
        )}
        <div className={styles.titleArea}>
          <Paragraph className={styles.displayName} ellipsis={{ tooltip: item.displayName }}>
            {item.displayName || item.knowledgeName}
          </Paragraph>
          <div className={styles.knowledgeName}>
            <Tooltip title={`ID: ${item.id}`}>
              <span className={styles.knowledgeNameText}>ID: {item.id}</span>
            </Tooltip>
            <CopyOutlined className={styles.copyIcon} onClick={handleCopyId} />
          </div>
        </div>
      </div>

      <div className={styles.divider} />

      <div className={styles.statsRow}>
        <div className={styles.statItem}>
          <div className={styles.statValue}>{formatDateTime(item.detail?.updateTime)}</div>
          <div className={styles.statLabel}>更新时间</div>
        </div>
        <div className={styles.statItem}>
          <div className={styles.statValue}>{item.detail?.fileCount ?? 0}</div>
          <div className={styles.statLabel}>文件数量</div>
        </div>
        <div className={styles.statItem}>
          <div className={styles.statValue}>{item.detail?.chunkCount ?? 0}</div>
          <div className={styles.statLabel}>切片数量</div>
        </div>
      </div>

      <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
        <div className={styles.moreBtn} onClick={(e) => e.stopPropagation()}>
          <MoreOutlined />
        </div>
      </Dropdown>
    </div>
  );
};

export default KnowledgeBaseCard;
