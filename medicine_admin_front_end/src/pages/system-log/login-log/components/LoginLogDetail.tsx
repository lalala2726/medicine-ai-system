import { Descriptions, Drawer, Empty, message, Spin, Tag, Typography } from 'antd';
import React, { useEffect, useState } from 'react';
import { getLoginLogDetail, type LoginLogVo } from '@/api/systemLog/loginLog';

const { Text } = Typography;

interface LoginLogDetailProps {
  open: boolean;
  onClose: () => void;
  logId: string | number | null;
}

// 登录来源映射
const loginSourceMap: Record<string, { text: string; color: string }> = {
  admin: { text: '管理端', color: 'blue' },
  client: { text: '客户端', color: 'green' },
};

// 登录方式映射
const loginTypeMap: Record<string, { text: string; color: string }> = {
  PASSWORD: { text: '密码登录', color: 'default' },
  SMS: { text: '短信登录', color: 'cyan' },
  WECHAT: { text: '微信登录', color: 'green' },
  AUTO: { text: '自动登录', color: 'purple' },
};

const LoginLogDetail: React.FC<LoginLogDetailProps> = ({ open, onClose, logId }) => {
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<LoginLogVo | null>(null);
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
      const result = await getLoginLogDetail(logId);
      setDetail(result);
    } catch (error) {
      console.error('获取登录日志详情失败:', error);
      messageApi.error('获取登录日志详情失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Drawer title="登录日志详情" placement="right" width={640} open={open} onClose={onClose}>
      {contextHolder}
      <Spin spinning={loading}>
        {detail ? (
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="日志ID" span={2}>
              {detail.id}
            </Descriptions.Item>
            <Descriptions.Item label="登录账号">{detail.username || '-'}</Descriptions.Item>
            <Descriptions.Item label="用户ID">{detail.userId || '-'}</Descriptions.Item>
            <Descriptions.Item label="登录来源">
              <Tag color={loginSourceMap[detail.loginSource || '']?.color || 'default'}>
                {loginSourceMap[detail.loginSource || '']?.text || detail.loginSource || '-'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="登录方式">
              <Tag color={loginTypeMap[detail.loginType || '']?.color || 'default'}>
                {loginTypeMap[detail.loginType || '']?.text || detail.loginType || '-'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="登录状态">
              <Tag color={detail.loginStatus === 1 ? 'success' : 'error'}>
                {detail.loginStatus === 1 ? '成功' : '失败'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="登录时间">{detail.loginTime || '-'}</Descriptions.Item>
            <Descriptions.Item label="IP地址">
              <Text copyable>{detail.ipAddress || '-'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="设备类型">{detail.deviceType || '-'}</Descriptions.Item>
            <Descriptions.Item label="操作系统">{detail.os || '-'}</Descriptions.Item>
            <Descriptions.Item label="浏览器" span={2}>
              {detail.browser || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="User-Agent" span={2}>
              <Text copyable style={{ fontSize: 12, wordBreak: 'break-all' }}>
                {detail.userAgent || '-'}
              </Text>
            </Descriptions.Item>
            {detail.loginStatus === 0 && detail.failReason && (
              <Descriptions.Item label="失败原因" span={2}>
                <Text type="danger" style={{ wordBreak: 'break-all' }}>
                  {detail.failReason}
                </Text>
              </Descriptions.Item>
            )}
          </Descriptions>
        ) : (
          <Empty description="暂无日志详情" />
        )}
      </Spin>
    </Drawer>
  );
};

export default LoginLogDetail;
