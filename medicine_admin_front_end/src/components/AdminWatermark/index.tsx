import { Watermark } from 'antd';
import React, { useEffect, useMemo, useState } from 'react';
import { getAdminWatermarkConfig, type SecurityConfigTypes } from '@/api/system/security-config';

/**
 * 管理端水印组件属性。
 */
export interface AdminWatermarkProps {
  /** 当前登录用户。 */
  currentUser?: API.CurrentUser;
  /** 子节点。 */
  children: React.ReactNode;
}

/**
 * 解析水印内容列表。
 *
 * @param config 管理端水印配置
 * @param currentUser 当前登录用户
 * @returns 水印内容列表
 */
function resolveWatermarkContent(
  config?: SecurityConfigTypes.AdminWatermarkConfig | null,
  currentUser?: API.CurrentUser,
): string | undefined {
  if (!config?.enabled || !currentUser) {
    return undefined;
  }

  const content: string[] = [];
  const resolvedUsername = String(currentUser.username || currentUser.name || '').trim();
  const resolvedUserId = String(currentUser.id ?? currentUser.userid ?? '').trim();

  if (config.showUsername && resolvedUsername) {
    content.push(`${resolvedUsername}`);
  }
  if (config.showUserId && resolvedUserId) {
    content.push(`${resolvedUserId}`);
  }

  return content.length > 0 ? content.join(' ') : undefined;
}

/**
 * 管理端全局水印组件。
 * 登录后静默读取水印配置，读取失败或配置为空时默认不显示。
 *
 * @param props 组件属性
 * @returns 带水印或不带水印的内容节点
 */
const AdminWatermark: React.FC<AdminWatermarkProps> = ({ currentUser, children }) => {
  const [watermarkConfig, setWatermarkConfig] = useState<
    SecurityConfigTypes.AdminWatermarkConfig | null | undefined
  >(undefined);
  const currentUserIdentityKey = `${currentUser?.id ?? ''}:${currentUser?.userid ?? ''}:${currentUser?.username ?? ''}`;
  const hasCurrentUser = Boolean(currentUser);

  useEffect(() => {
    /**
     * 加载管理端水印配置。
     */
    const loadWatermarkConfig = async () => {
      if (!hasCurrentUser) {
        setWatermarkConfig(null);
        return;
      }
      try {
        const config = await getAdminWatermarkConfig({
          skipErrorHandler: true,
        });
        setWatermarkConfig(config || null);
      } catch (error) {
        console.error('loadWatermarkConfig error:', error);
        setWatermarkConfig(null);
      }
    };

    void loadWatermarkConfig();
  }, [currentUserIdentityKey, hasCurrentUser]);

  const watermarkContent = useMemo(
    () => resolveWatermarkContent(watermarkConfig, currentUser),
    [currentUser, watermarkConfig],
  );

  if (!watermarkContent) {
    return <>{children}</>;
  }

  return (
    <Watermark
      content={watermarkContent}
      font={{
        fontSize: 16,
      }}
      gap={[120, 120]}
    >
      {children}
    </Watermark>
  );
};

export default AdminWatermark;
