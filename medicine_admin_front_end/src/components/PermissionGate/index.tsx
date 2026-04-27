import React from 'react';
import { usePermission } from '@/hooks/usePermission';
import type { PermissionMatchMode } from '@/utils/permission';

export interface PermissionGateProps {
  /** 需要的权限编码。 */
  access?: string | string[];
  /** 多权限编码的匹配模式。 */
  accessMode?: PermissionMatchMode;
  /** 有权限时渲染的内容。 */
  children: React.ReactNode;
  /** 无权限时渲染的内容。 */
  denied?: React.ReactNode;
}

/**
 * 权限内容门禁组件。
 * @param props 内容权限配置。
 * @returns 按权限渲染后的内容节点。
 */
const PermissionGate: React.FC<PermissionGateProps> = ({
  access,
  accessMode = 'any',
  children,
  denied = null,
}) => {
  const { canAccess } = usePermission();
  return canAccess(access, accessMode) ? <>{children}</> : <>{denied}</>;
};

export default PermissionGate;
