import { Descriptions, Drawer, Empty, message, Spin, Tag, Typography } from 'antd';
import React, { useEffect, useState } from 'react';
import ReactJson from 'react-json-view';
import { getOperationLogDetail, type OperationLogVo } from '@/api/systemLog/operationLog';

const { Text } = Typography;

interface OperationLogDetailProps {
  open: boolean;
  onClose: () => void;
  logId: string | number | null;
}

const OperationLogDetail: React.FC<OperationLogDetailProps> = ({ open, onClose, logId }) => {
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<OperationLogVo | null>(null);
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    if (open && logId) {
      fetchDetail();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, logId]);

  const fetchDetail = async () => {
    if (!logId) return;

    try {
      setLoading(true);
      const result = await getOperationLogDetail(logId);
      setDetail(result);
    } catch (error) {
      console.error('获取操作日志详情失败:', error);
      messageApi.error('获取操作日志详情失败');
    } finally {
      setLoading(false);
    }
  };

  // 解析 JSON 字符串
  const parseJson = (content?: string): { data: object | null; isValid: boolean } => {
    if (!content) {
      return { data: null, isValid: false };
    }
    try {
      const parsed = JSON.parse(content);
      return { data: parsed, isValid: true };
    } catch {
      return { data: null, isValid: false };
    }
  };

  // 渲染 JSON 内容（使用 react-json-view）
  const renderJsonView = (content?: string) => {
    if (!content) {
      return <Text type="secondary">-</Text>;
    }

    const { data, isValid } = parseJson(content);

    if (!isValid || !data) {
      return (
        <Text
          copyable
          style={{
            fontFamily: 'monospace',
            fontSize: 12,
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-all',
          }}
        >
          {content}
        </Text>
      );
    }

    return (
      <div
        style={{
          borderRadius: 4,
          overflow: 'auto',
          maxHeight: 400,
        }}
      >
        <ReactJson
          src={data}
          name={false}
          collapsed={2}
          enableClipboard
          displayDataTypes={false}
          displayObjectSize={false}
          indentWidth={2}
          collapseStringsAfterLength={100}
          theme="rjv-default"
        />
      </div>
    );
  };

  return (
    <Drawer title="操作日志详情" placement="right" width={800} open={open} onClose={onClose}>
      {contextHolder}
      <Spin spinning={loading}>
        {detail ? (
          <>
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="日志ID" span={2}>
                {detail.id}
              </Descriptions.Item>
              <Descriptions.Item label="业务模块">
                <Tag color="blue">{detail.module || '-'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="操作说明">{detail.action || '-'}</Descriptions.Item>
              <Descriptions.Item label="请求URI" span={2}>
                <Text copyable style={{ fontFamily: 'monospace', fontSize: 12 }}>
                  {detail.requestUri || '-'}
                </Text>
              </Descriptions.Item>
              <Descriptions.Item label="HTTP方法">
                <Tag
                  color={
                    detail.httpMethod === 'GET'
                      ? 'green'
                      : detail.httpMethod === 'POST'
                        ? 'blue'
                        : detail.httpMethod === 'PUT'
                          ? 'orange'
                          : detail.httpMethod === 'DELETE'
                            ? 'red'
                            : 'default'
                  }
                >
                  {detail.httpMethod || '-'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="方法名">
                <Text style={{ fontFamily: 'monospace', fontSize: 12 }}>
                  {detail.methodName || '-'}
                </Text>
              </Descriptions.Item>
              <Descriptions.Item label="操作人账号">{detail.username || '-'}</Descriptions.Item>
              <Descriptions.Item label="操作人ID">{detail.userId || '-'}</Descriptions.Item>
              <Descriptions.Item label="请求IP" span={2}>
                {detail.ip || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="执行状态">
                <Tag color={detail.success === 1 ? 'success' : 'error'}>
                  {detail.success === 1 ? '成功' : '失败'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="耗时">
                {detail.costTime ? `${detail.costTime} ms` : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间" span={2}>
                {detail.createTime || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="User-Agent" span={2}>
                <Text copyable style={{ fontSize: 12, wordBreak: 'break-all' }}>
                  {detail.userAgent || '-'}
                </Text>
              </Descriptions.Item>
            </Descriptions>

            {/* 请求参数 - 使用 JSON View */}
            <div style={{ marginTop: 16 }}>
              <div
                style={{
                  padding: '8px 16px',
                  background: '#fafafa',
                  border: '1px solid #d9d9d9',
                  borderBottom: 'none',
                  borderRadius: '4px 4px 0 0',
                  fontWeight: 500,
                }}
              >
                请求参数
              </div>
              <div
                style={{
                  padding: 16,
                  border: '1px solid #d9d9d9',
                  borderRadius: '0 0 4px 4px',
                  background: '#fff',
                }}
              >
                {renderJsonView(detail.requestParams)}
              </div>
            </div>

            {/* 返回结果 - 使用 JSON View */}
            <div style={{ marginTop: 16 }}>
              <div
                style={{
                  padding: '8px 16px',
                  background: '#fafafa',
                  border: '1px solid #d9d9d9',
                  borderBottom: 'none',
                  borderRadius: '4px 4px 0 0',
                  fontWeight: 500,
                }}
              >
                返回结果
              </div>
              <div
                style={{
                  padding: 16,
                  border: '1px solid #d9d9d9',
                  borderRadius: '0 0 4px 4px',
                  background: '#fff',
                }}
              >
                {renderJsonView(detail.responseResult)}
              </div>
            </div>

            {/* 错误信息 */}
            {detail.success === 0 && detail.errorMsg && (
              <div style={{ marginTop: 16 }}>
                <div
                  style={{
                    padding: '8px 16px',
                    background: '#fff2f0',
                    border: '1px solid #ffccc7',
                    borderBottom: 'none',
                    borderRadius: '4px 4px 0 0',
                    fontWeight: 500,
                    color: '#cf1322',
                  }}
                >
                  错误信息
                </div>
                <div
                  style={{
                    padding: 16,
                    border: '1px solid #ffccc7',
                    borderRadius: '0 0 4px 4px',
                    background: '#fff',
                  }}
                >
                  <Text type="danger" style={{ wordBreak: 'break-all' }}>
                    {detail.errorMsg}
                  </Text>
                </div>
              </div>
            )}
          </>
        ) : (
          <Empty description="暂无日志详情" />
        )}
      </Spin>
    </Drawer>
  );
};

export default OperationLogDetail;
