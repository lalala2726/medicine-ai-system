/**
 * ModelSelector — 聊天界面模型选择器
 *
 * 仅用于管理端智能助手聊天页面，仅展示“自定义模型名称 + 文案”。
 */
import { CheckOutlined, DownOutlined } from '@ant-design/icons';
import { Popover, Spin } from 'antd';
import React, { useCallback, useState } from 'react';

import type { SystemModelTypes } from '@/api/llm-manage/systemModels';

import styles from './index.module.less';

export interface ModelSelectorProps {
  /** 可选模型列表 */
  options: SystemModelTypes.AdminAssistantChatDisplayModel[];
  /** 当前选中的模型值（customModelName） */
  value: string | undefined;
  /** 选择后的回调 */
  onChange: (option: SystemModelTypes.AdminAssistantChatDisplayModel) => void;
  /** 加载中状态 */
  loading?: boolean;
  /** 是否禁用选择器 */
  disabled?: boolean;
  /** 无可选模型时展示的占位文案 */
  emptyLabel?: string;
}

/**
 * 下拉列表渲染内容。
 *
 * @param options 模型选项列表。
 * @param value 当前选中值。
 * @param onSelect 选中回调。
 * @returns 下拉列表 JSX。
 */
function DropdownContent({
  options,
  value,
  onSelect,
}: {
  options: SystemModelTypes.AdminAssistantChatDisplayModel[];
  value: string | undefined;
  onSelect: (opt: SystemModelTypes.AdminAssistantChatDisplayModel) => void;
}) {
  return (
    <div className={styles.dropdownOverlay}>
      <div className={styles.sectionLabel}>模型</div>
      {options.map((opt) => {
        const isActive = opt.customModelName === value;
        return (
          <button
            key={`${opt.customModelName}-${opt.actualModelName}`}
            type="button"
            className={`${styles.modelItem} ${isActive ? styles.active : ''}`}
            onClick={() => onSelect(opt)}
          >
            <div className={styles.modelItemLeft}>
              <span className={styles.modelName}>{opt.customModelName}</span>
              {opt.description && <span className={styles.modelDesc}>{opt.description}</span>}
            </div>
            {isActive && <CheckOutlined className={styles.checkIcon} />}
          </button>
        );
      })}
    </div>
  );
}

/**
 * 聊天界面顶部模型选择器组件。
 *
 * @param options 可选模型列表。
 * @param value 当前选中的模型值。
 * @param onChange 选中回调，返回完整的 ModelOption 对象。
 * @param loading 是否正在加载模型列表。
 */
const ModelSelector: React.FC<ModelSelectorProps> = ({
  options,
  value,
  onChange,
  loading = false,
  disabled = false,
  emptyLabel = '未配置模型',
}) => {
  const [open, setOpen] = useState(false);

  /** 选中某个模型后关闭下拉并回调父组件。 */
  const handleSelect = useCallback(
    (opt: SystemModelTypes.AdminAssistantChatDisplayModel) => {
      setOpen(false);
      onChange(opt);
    },
    [onChange],
  );

  const currentLabel =
    options.find((item) => item.customModelName === value)?.customModelName || value || '选择模型';

  if (loading && options.length === 0) {
    return (
      <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, padding: '4px 10px' }}>
        <Spin size="small" />
        <span style={{ fontSize: 13, color: 'var(--ant-color-text-secondary)' }}>加载模型…</span>
      </div>
    );
  }

  if (disabled || options.length === 0) {
    return (
      <button type="button" className={`${styles.trigger} ${styles.triggerDisabled}`} disabled>
        {emptyLabel}
        <DownOutlined className={styles.triggerIcon} />
      </button>
    );
  }

  return (
    <Popover
      open={open}
      onOpenChange={setOpen}
      trigger="click"
      placement="bottomLeft"
      arrow={false}
      overlayInnerStyle={{ padding: 0, borderRadius: 12, boxShadow: 'none' }}
      content={<DropdownContent options={options} value={value} onSelect={handleSelect} />}
    >
      <button type="button" className={styles.trigger}>
        {currentLabel}
        <DownOutlined className={`${styles.triggerIcon} ${open ? styles.open : ''}`} />
      </button>
    </Popover>
  );
};

export default ModelSelector;
