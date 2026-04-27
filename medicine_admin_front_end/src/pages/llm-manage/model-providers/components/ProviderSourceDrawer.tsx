import { TEXT_INPUT_AUTOCOMPLETE } from '@/constants/autocomplete';
import { BlockOutlined, LinkOutlined, SearchOutlined } from '@ant-design/icons';
import { Card, Drawer, Empty, Input, Space, Spin, Tag, Typography } from 'antd';
import React, { useMemo, useState } from 'react';

import type { ModelProviderTypes } from '@/api/llm-manage/modelProviders';

import { PROVIDER_TYPE_LABELS } from '../shared';
import styles from '../index.module.less';

const { Title, Paragraph } = Typography;

interface ProviderSourceDrawerProps {
  open: boolean;
  loading: boolean;
  presets: ModelProviderTypes.ProviderPresetItem[];
  onCancel: () => void;
  onSelectPreset: (providerKey: string) => void;
}

const ProviderSourceDrawer: React.FC<ProviderSourceDrawerProps> = ({
  open,
  loading,
  presets,
  onCancel,
  onSelectPreset,
}) => {
  const [searchValue, setSearchValue] = useState('');

  const filteredPresets = useMemo(() => {
    let list = [...presets];
    // Sort by name
    list.sort((a, b) => a.providerName.localeCompare(b.providerName));

    if (searchValue) {
      const lowerSearch = searchValue.toLowerCase();
      list = list.filter(
        (p) =>
          p.providerName.toLowerCase().includes(lowerSearch) ||
          p.providerKey.toLowerCase().includes(lowerSearch) ||
          p.description?.toLowerCase().includes(lowerSearch),
      );
    }
    return list;
  }, [presets, searchValue]);

  return (
    <Drawer
      title={
        <div className={styles.drawerTitle}>
          <BlockOutlined />
          <span>选择模型提供商</span>
        </div>
      }
      placement="right"
      onClose={onCancel}
      open={open}
      width={640}
      extra={
        <Space>
          <Input
            autoComplete={TEXT_INPUT_AUTOCOMPLETE}
            placeholder="搜索预设提供商..."
            prefix={<SearchOutlined />}
            variant="filled"
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
            style={{ width: 240 }}
            allowClear
          />
        </Space>
      }
      styles={{
        body: { padding: '16px 24px' },
      }}
    >
      <div className={styles.drawerContent}>
        <section className={styles.drawerSection}>
          <div className={styles.sectionHeader}>
            <Title level={5} className={styles.sectionTitle}>
              <LinkOutlined /> 阿里云百联预设
            </Title>
          </div>

          <Spin spinning={loading}>
            {filteredPresets.length > 0 ? (
              <div className={styles.presetGrid}>
                {filteredPresets.map((preset) => (
                  <Card
                    key={preset.providerKey}
                    className={styles.providerCard}
                    onClick={() => onSelectPreset(preset.providerKey)}
                  >
                    <div className={styles.providerCardHeader}>
                      <div className={styles.providerCardTitleInfo}>
                        <Title level={5} style={{ margin: 0 }} ellipsis>
                          {preset.providerName}
                        </Title>
                      </div>
                    </div>
                    <Paragraph
                      className={styles.providerDescription}
                      type="secondary"
                      ellipsis={{ rows: 2 }}
                    >
                      {preset.description || '官方直连，支持更多原生特性。'}
                    </Paragraph>
                    <div className={styles.providerMeta}>
                      <Tag bordered={false}>预设</Tag>
                      <Tag>{PROVIDER_TYPE_LABELS[preset.providerType]}</Tag>
                    </div>
                  </Card>
                ))}
              </div>
            ) : (
              !loading && (
                <div className={styles.sourceEmpty}>
                  <Empty
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description={searchValue ? '未找到相关预设' : '暂无可用预设'}
                  />
                </div>
              )
            )}
          </Spin>
        </section>
      </div>
    </Drawer>
  );
};

export default ProviderSourceDrawer;
