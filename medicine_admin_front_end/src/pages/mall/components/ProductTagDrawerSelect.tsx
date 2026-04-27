import { PlusOutlined, DownOutlined, CameraOutlined } from '@ant-design/icons';
import { Button, Drawer, Space, Spin, Tag, Typography, theme } from 'antd';
import React, { useMemo, useState, useEffect } from 'react';
import ProductTagGroupSelect from './ProductTagGroupSelect';
import AiTagRecognitionModal from './AiTagRecognitionModal';
import type { ProductTagLike, ProductTagSelectGroup } from './productTagUtils';

const { Text } = Typography;

export interface ProductTagDrawerSelectProps {
  /** 当前选中的标签 ID 列表 (antd Form 会自动传入) */
  value?: string[];
  /** 标签变化回调 (antd Form 会自动处理) */
  onChange?: (value: string[]) => void;
  /** 标签分组数据 */
  groups: ProductTagSelectGroup[];
  /** 当前商品已绑定标签详情 */
  selectedTags?: ProductTagLike[];
  /** 标签分组刷新回调 */
  onGroupsRefresh?: () => Promise<void> | void;
  /** 是否在加载中 */
  loading?: boolean;
  /** 是否禁用 */
  disabled?: boolean;
  /** 占位符 */
  placeholder?: string;
}

/**
 * 商品标签抽屉式选择组件。
 * 触发区域模拟 antd Select 的样式，点击后打开抽屉。
 */
const ProductTagDrawerSelect: React.FC<ProductTagDrawerSelectProps> = ({
  value = [],
  onChange,
  groups,
  selectedTags = [],
  onGroupsRefresh,
  loading = false,
  disabled = false,
  placeholder = '请选择商品标签',
}) => {
  const { token } = theme.useToken();
  const [visible, setVisible] = useState(false);
  // 使用内部状态暂存编辑中的标签，点击确定后再同步给 Form
  const [tempValue, setTempValue] = useState<string[]>([]);
  const [dynamicOptionLabelMap, setDynamicOptionLabelMap] = useState<Record<string, string>>({});
  const [aiModalOpen, setAiModalOpen] = useState(false);

  // 同步外部 value 到内部状态
  useEffect(() => {
    if (visible) {
      setTempValue((value || []).map(String));
    }
  }, [visible, value]);

  // 扁平化所有选项，用于根据 ID 查找名称
  const allOptionsMap = useMemo(() => {
    const map = new Map<string, string>();
    groups.forEach((group) => {
      group.options.forEach((opt) => {
        map.set(String(opt.value), opt.label);
      });
    });
    selectedTags.forEach((tag) => {
      const tagId = tag.id ? String(tag.id) : '';
      if (tagId && tag.name) {
        map.set(tagId, tag.name);
      }
    });
    Object.entries(dynamicOptionLabelMap).forEach(([tagId, tagName]) => {
      if (tagId && tagName) {
        map.set(tagId, tagName);
      }
    });
    return map;
  }, [dynamicOptionLabelMap, groups, selectedTags]);

  const selectedIds = useMemo(() => (value || []).map(String), [value]);

  const handleRemoveTag = (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    if (disabled) return;
    const nextValue = selectedIds.filter((item) => item !== id);
    onChange?.(nextValue);
  };

  const showDrawer = () => {
    if (!disabled) {
      setVisible(true);
    }
  };

  const handleOk = () => {
    onChange?.(tempValue);
    setVisible(false);
  };

  const handleCancel = () => {
    setVisible(false);
  };

  /** AI 标签识别确认后，将匹配的标签合并到暂存列表。 */
  const handleAiTagConfirm = (tagIds: string[]) => {
    setTempValue(tagIds);
  };

  const handleClear = () => {
    setTempValue([]);
  };

  // 渲染触发区域 (模拟 Select 样式)
  const renderTrigger = () => {
    return (
      <div
        onClick={showDrawer}
        style={{
          minHeight: '32px',
          padding: '2px 8px',
          background: disabled ? token.colorBgContainerDisabled : token.colorBgContainer,
          border: `1px solid ${token.colorBorder}`,
          borderRadius: token.borderRadius,
          cursor: disabled ? 'not-allowed' : 'pointer',
          display: 'flex',
          flexWrap: 'wrap',
          gap: '4px',
          alignItems: 'center',
          transition: 'all 0.3s',
          boxSizing: 'border-box',
          position: 'relative',
          paddingRight: '30px', // 为右侧图标留出空间
        }}
        onMouseEnter={(e) => {
          if (!disabled) e.currentTarget.style.borderColor = token.colorPrimary;
        }}
        onMouseLeave={(e) => {
          if (!disabled) e.currentTarget.style.borderColor = token.colorBorder;
        }}
      >
        {selectedIds.length > 0 ? (
          selectedIds.map((id) => {
            const label = allOptionsMap.get(id) || id;
            return (
              <Tag
                key={id}
                closable={!disabled}
                onClose={(e) => handleRemoveTag(e as any, id)}
                style={{
                  margin: 0,
                  display: 'inline-flex',
                  alignItems: 'center',
                  fontSize: '12px',
                }}
              >
                {label}
              </Tag>
            );
          })
        ) : (
          <Text type="secondary" style={{ marginLeft: 4 }}>
            {placeholder}
          </Text>
        )}

        {/* 右侧下拉箭头/加号图标 */}
        <div
          style={{
            position: 'absolute',
            right: '8px',
            top: '50%',
            transform: 'translateY(-50%)',
            display: 'flex',
            alignItems: 'center',
            color: token.colorTextQuaternary,
          }}
        >
          {selectedIds.length > 0 && !disabled ? (
            <PlusOutlined style={{ fontSize: '12px' }} />
          ) : (
            <DownOutlined style={{ fontSize: '10px' }} />
          )}
        </div>
      </div>
    );
  };

  return (
    <div style={{ width: '100%' }}>
      <Spin spinning={loading}>{renderTrigger()}</Spin>

      <Drawer
        title="选择商品标签"
        placement="right"
        width={550}
        onClose={handleCancel}
        open={visible}
        destroyOnClose
        extra={
          <Space>
            <Button
              icon={<CameraOutlined />}
              onClick={() => setAiModalOpen(true)}
              style={{ borderRadius: '16px' }}
              size="small"
            >
              AI 图片识别
            </Button>
            <Button onClick={handleClear} size="small">
              清空已选
            </Button>
            <Button type="primary" onClick={handleOk} size="small">
              确定
            </Button>
          </Space>
        }
        footer={
          <div style={{ display: 'flex', justifyContent: 'flex-end', padding: '10px 0' }}>
            <Space>
              <Button onClick={handleCancel}>取消</Button>
              <Button type="primary" onClick={handleOk}>
                保存并关闭
              </Button>
            </Space>
          </div>
        }
      >
        <div style={{ marginBottom: 16, padding: '0 4px' }}>
          <Text type="secondary">
            已选择{' '}
            <Text strong style={{ color: token.colorPrimary }}>
              {tempValue.length}
            </Text>{' '}
            个标签
          </Text>
        </div>
        <ProductTagGroupSelect
          value={tempValue}
          onChange={setTempValue}
          groups={groups}
          selectedTags={selectedTags}
          onGroupsRefresh={onGroupsRefresh}
          onOptionLabelMapChange={setDynamicOptionLabelMap}
          disabled={disabled}
        />
      </Drawer>

      <AiTagRecognitionModal
        open={aiModalOpen}
        onClose={() => setAiModalOpen(false)}
        tagGroups={groups}
        currentTagIds={tempValue}
        onConfirm={handleAiTagConfirm}
      />
    </div>
  );
};

ProductTagDrawerSelect.displayName = 'ProductTagDrawerSelect';

export default ProductTagDrawerSelect;
