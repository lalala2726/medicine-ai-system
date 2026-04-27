import { Button, Card, Descriptions, Progress, Space, Spin, Tag, Alert, message } from 'antd';
import dayjs from 'dayjs';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getEsIndexConfig, triggerEsIndexRebuild, type EsIndexTypes } from '@/api/system/es-index';
import PermissionButton from '@/components/PermissionButton';
import { ADMIN_PERMISSIONS } from '@/constants/permissions';

/** 商品索引运行状态轮询间隔（毫秒）。 */
const ES_INDEX_POLLING_INTERVAL = 5000;

/**
 * 将时间字符串格式化为统一展示文本。
 * @param value 时间字符串。
 * @returns 格式化后的时间文本。
 */
function formatDateTime(value?: string): string {
  if (!value) {
    return '-';
  }
  const dateValue = dayjs(value);
  if (!dateValue.isValid()) {
    return value;
  }
  return dateValue.format('YYYY-MM-DD HH:mm:ss');
}

/**
 * 将布尔值映射为“是/否”文案。
 * @param value 布尔值。
 * @returns 展示文案。
 */
function formatBooleanText(value?: boolean): string {
  return value ? '是' : '否';
}

/**
 * 解析商品索引任务触发来源文案。
 * @param triggerSource 触发来源。
 * @returns 展示文案。
 */
function resolveTriggerSourceLabel(triggerSource?: string): string {
  if (triggerSource === 'startup') {
    return '启动自动触发';
  }
  if (triggerSource === 'manual') {
    return '手动触发';
  }
  return '-';
}

/**
 * 计算当前商品索引重建进度百分比。
 * @param rebuildStatus 商品索引重建状态。
 * @returns 进度百分比。
 */
function resolveProgressPercent(
  rebuildStatus?: EsIndexTypes.ProductIndexRebuildStatusVo,
): number | undefined {
  const totalCount = Number(rebuildStatus?.totalCount ?? 0);
  const processedCount = Number(rebuildStatus?.processedCount ?? 0);
  if (totalCount <= 0) {
    return undefined;
  }
  return Math.min(100, Math.round((processedCount / totalCount) * 100));
}

/**
 * ES 与商品索引配置区域。
 * @returns 页面节点。
 */
const EsIndexSection: React.FC = () => {
  const [messageApi, contextHolder] = message.useMessage();
  const [loading, setLoading] = useState(false);
  const [triggering, setTriggering] = useState(false);
  const [config, setConfig] = useState<EsIndexTypes.EsIndexConfigVo | null>(null);
  const previousRunningRef = useRef(false);

  /**
   * 加载 ES 与商品索引概览。
   * @param options 加载选项。
   * @returns 无返回值。
   */
  const loadConfig = useCallback(
    async (options?: { showLoading?: boolean; showError?: boolean }) => {
      const showLoading = options?.showLoading ?? true;
      const showError = options?.showError ?? true;

      if (showLoading) {
        setLoading(true);
      }
      try {
        const nextConfig = await getEsIndexConfig();
        setConfig(nextConfig);
      } catch (error) {
        console.error('加载 ES 与商品索引概览失败:', error);
        if (showError) {
          messageApi.error('加载 ES 与商品索引概览失败，请稍后重试');
        }
      } finally {
        if (showLoading) {
          setLoading(false);
        }
      }
    },
    [messageApi],
  );

  /**
   * 手动刷新页面数据。
   * @returns 无返回值。
   */
  const handleRefresh = useCallback(async () => {
    await loadConfig();
  }, [loadConfig]);

  /**
   * 手动触发商品索引全量重建。
   * @returns 无返回值。
   */
  const handleManualRebuild = useCallback(async () => {
    try {
      setTriggering(true);
      const submitted = await triggerEsIndexRebuild();
      if (submitted) {
        messageApi.success('商品索引重建任务已提交');
      } else {
        messageApi.info('商品索引重建任务已在执行中');
      }
      await loadConfig({ showLoading: false, showError: true });
    } catch (error) {
      console.error('触发商品索引重建失败:', error);
      messageApi.error('触发商品索引重建失败，请稍后重试');
    } finally {
      setTriggering(false);
    }
  }, [loadConfig, messageApi]);

  useEffect(() => {
    void loadConfig();
  }, [loadConfig]);

  useEffect(() => {
    const isRunning = Boolean(config?.rebuildStatus?.running);
    const wasRunning = previousRunningRef.current;

    if (wasRunning && !isRunning) {
      const lastError = config?.rebuildStatus?.lastError;
      if (lastError) {
        messageApi.error(lastError);
      } else {
        messageApi.success('商品索引重建已完成');
      }
    }

    previousRunningRef.current = isRunning;
  }, [config, messageApi]);

  useEffect(() => {
    if (!config?.rebuildStatus?.running) {
      return undefined;
    }

    const timer = window.setTimeout(() => {
      void loadConfig({ showLoading: false, showError: false });
    }, ES_INDEX_POLLING_INTERVAL);

    return () => window.clearTimeout(timer);
  }, [config?.rebuildStatus?.running, loadConfig]);

  /** 当前商品索引重建进度百分比。 */
  const progressPercent = useMemo(() => resolveProgressPercent(config?.rebuildStatus), [config]);

  /** 当前是否允许再次触发重建。 */
  const rebuildButtonDisabled = Boolean(config?.rebuildStatus?.running) || triggering;

  return (
    <Spin spinning={loading}>
      {contextHolder}
      <Space direction="vertical" style={{ width: '100%' }} size={16}>
        <Space wrap>
          <Button onClick={() => void handleRefresh()}>刷新</Button>
          <PermissionButton
            type="primary"
            loading={triggering}
            access={ADMIN_PERMISSIONS.systemConfig.esIndexRebuild}
            disabled={rebuildButtonDisabled}
            onClick={() => void handleManualRebuild()}
          >
            手动重建
          </PermissionButton>
        </Space>

        <Card title="ES 状态卡片">
          <Descriptions column={{ xs: 1, md: 2 }} bordered size="small">
            <Descriptions.Item label="Elasticsearch 状态">
              {config?.esAvailable ? (
                <Tag color="success">可用</Tag>
              ) : (
                <Tag color="error">不可用</Tag>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="商品索引名称">{config?.indexName || '-'}</Descriptions.Item>
            <Descriptions.Item label="商品索引是否存在">
              {config?.indexExists ? <Tag color="processing">已存在</Tag> : <Tag>不存在</Tag>}
            </Descriptions.Item>
            <Descriptions.Item label="当前文档数量">{config?.documentCount ?? 0}</Descriptions.Item>
          </Descriptions>
        </Card>

        <Card title="商品索引任务卡片">
          <Space direction="vertical" style={{ width: '100%' }} size={16}>
            {progressPercent !== undefined ? (
              <Progress
                percent={progressPercent}
                status={config?.rebuildStatus?.running ? 'active' : 'success'}
                format={() =>
                  `${config?.rebuildStatus?.processedCount ?? 0}/${config?.rebuildStatus?.totalCount ?? 0}`
                }
              />
            ) : null}

            <Descriptions column={{ xs: 1, md: 2 }} bordered size="small">
              <Descriptions.Item label="运行状态">
                {config?.rebuildStatus?.running ? (
                  <Tag color="processing">运行中</Tag>
                ) : (
                  <Tag>空闲</Tag>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="触发来源">
                {resolveTriggerSourceLabel(config?.rebuildStatus?.triggerSource)}
              </Descriptions.Item>
              <Descriptions.Item label="累计处理数量">
                {config?.rebuildStatus?.processedCount ?? 0}
              </Descriptions.Item>
              <Descriptions.Item label="预计总数量">
                {config?.rebuildStatus?.totalCount ?? 0}
              </Descriptions.Item>
              <Descriptions.Item label="已完成批次数">
                {config?.rebuildStatus?.batchCount ?? 0}
              </Descriptions.Item>
              <Descriptions.Item label="最近完成时间">
                {formatDateTime(config?.rebuildStatus?.finishedTime)}
              </Descriptions.Item>
              <Descriptions.Item label="开始时间">
                {formatDateTime(config?.rebuildStatus?.startedTime)}
              </Descriptions.Item>
              <Descriptions.Item label="最近错误信息" span={1}>
                {config?.rebuildStatus?.lastError || '-'}
              </Descriptions.Item>
            </Descriptions>

            {config?.rebuildStatus?.lastError ? (
              <Alert
                type="error"
                showIcon
                message="最近一次重建存在错误"
                description={config.rebuildStatus.lastError}
              />
            ) : null}
          </Space>
        </Card>

        <Card title="启动策略说明卡片">
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            <Descriptions column={{ xs: 1, md: 2 }} bordered size="small">
              <Descriptions.Item label="启用启动自动重建">
                {formatBooleanText(config?.startupAutoRebuildEnabled)}
              </Descriptions.Item>
              <Descriptions.Item label="启动触发策略">
                {config?.startupTriggerPolicy || '-'}
              </Descriptions.Item>
            </Descriptions>
          </Space>
        </Card>
      </Space>
    </Spin>
  );
};

export default EsIndexSection;
