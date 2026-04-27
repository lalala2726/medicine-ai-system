import { Empty, Input, Tag, Typography, theme } from 'antd';
import React, { useMemo, useState } from 'react';
import type { ProductTagSelectGroup } from './productTagUtils';

const { Search } = Input;
const { CheckableTag } = Tag;
const { Text } = Typography;

export interface ProductTagFlatSelectProps {
  /** 当前选中的标签 ID 列表 */
  value?: string[];
  /** 标签变化回调 */
  onChange?: (value: string[]) => void;
  /** 标签分组数据 */
  groups: ProductTagSelectGroup[];
  /** 是否禁用 */
  disabled?: boolean;
  /** 空数据提示 */
  emptyText?: string;
}

/**
 * 商品标签按类型分组平铺多选组件
 * 采用标签云 + 搜索的形式。
 */
const ProductTagFlatSelect: React.FC<ProductTagFlatSelectProps> = ({
  value = [],
  onChange,
  groups,
  disabled = false,
  emptyText = '暂无可选标签',
}) => {
  const { token } = theme.useToken();
  const [searchText, setSearchText] = useState('');

  // 统一转换 value 为 string 数组，确保比对一致性
  const selectedIds = useMemo(() => new Set((value || []).map(String)), [value]);

  // 根据搜索文本过滤标签
  const filteredGroups = useMemo(() => {
    if (!searchText.trim()) return groups;
    const lowerSearch = searchText.toLowerCase();
    return groups
      .map((group) => ({
        ...group,
        options: group.options.filter((opt) => opt.label.toLowerCase().includes(lowerSearch)),
      }))
      .filter((group) => group.options.length > 0);
  }, [groups, searchText]);

  /**
   * 处理标签点击
   */
  const handleTagChange = (tagId: string, checked: boolean) => {
    if (disabled) return;
    const currentValue = value || [];
    const nextValue = checked
      ? [...currentValue, tagId]
      : currentValue.filter((id) => String(id) !== String(tagId));
    onChange?.(nextValue);
  };

  if (!groups.length) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={emptyText} />;
  }

  return (
    <div style={{ width: '100%' }}>
      {/* 搜索区域 */}
      <div style={{ marginBottom: 16 }}>
        <Search
          placeholder="快速搜索标签..."
          allowClear
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          style={{ maxWidth: 360 }}
          disabled={disabled}
        />
      </div>

      {/* 标签列表展示 */}
      {filteredGroups.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={searchText ? '未找到匹配的标签' : emptyText}
        />
      ) : (
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: 0,
            border: `1px solid ${token.colorBorderSecondary}`,
            borderRadius: '8px',
            overflow: 'hidden',
          }}
        >
          {filteredGroups.map((group, index) => (
            <div
              key={group.typeId}
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                backgroundColor: index % 2 === 0 ? token.colorBgContainer : token.colorFillAlter,
                padding: '12px 16px',
                borderBottom:
                  index === filteredGroups.length - 1
                    ? 'none'
                    : `1px solid ${token.colorBorderSecondary}`,
              }}
            >
              <div
                style={{
                  minWidth: 100,
                  width: 100,
                  paddingTop: 4,
                  paddingRight: 16,
                  flexShrink: 0,
                }}
              >
                <Text strong style={{ color: token.colorTextSecondary, fontSize: '14px' }}>
                  {group.typeName}
                </Text>
              </div>
              <div
                style={{
                  flex: 1,
                  display: 'flex',
                  flexWrap: 'wrap',
                  gap: '8px 12px',
                }}
              >
                {group.options.map((opt) => {
                  const isChecked = selectedIds.has(String(opt.value));
                  return (
                    <CheckableTag
                      key={opt.value}
                      checked={isChecked}
                      onChange={(checked) => handleTagChange(String(opt.value), checked)}
                      style={{
                        margin: 0,
                        padding: '2px 12px',
                        border: isChecked
                          ? `1px solid ${token.colorPrimary}`
                          : `1px solid ${token.colorBorder}`,
                        borderRadius: '4px',
                        backgroundColor: isChecked ? token.colorPrimaryBg : token.colorBgContainer,
                        color: isChecked ? token.colorPrimary : 'inherit',
                        fontSize: '13px',
                        transition: 'all 0.2s',
                        cursor: disabled ? 'not-allowed' : 'pointer',
                        userSelect: 'none',
                      }}
                    >
                      {opt.label}
                    </CheckableTag>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

ProductTagFlatSelect.displayName = 'ProductTagFlatSelect';

export default ProductTagFlatSelect;
