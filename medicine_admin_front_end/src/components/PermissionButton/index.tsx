import type { ButtonProps } from 'antd';
import { Button, Tooltip } from 'antd';
import React from 'react';
import { usePermission } from '@/hooks/usePermission';
import type { PermissionMatchMode } from '@/utils/permission';

/** 无权限按钮默认提示。 */
export const NO_PERMISSION_BUTTON_TIP = '无权限操作';

export interface PermissionButtonProps extends ButtonProps {
  /** 按钮需要的权限编码。 */
  access?: string | string[];
  /** 多权限编码的匹配模式。 */
  accessMode?: PermissionMatchMode;
  /** 无权限时展示的提示文案。 */
  noPermissionTip?: React.ReactNode;
}

/**
 * 权限按钮组件。
 * @param props 按钮属性与权限配置。
 * @returns 带权限禁用态的按钮节点。
 */
const PermissionButton: React.FC<PermissionButtonProps> = ({
  access,
  accessMode = 'any',
  noPermissionTip = NO_PERMISSION_BUTTON_TIP,
  disabled,
  children,
  ...buttonProps
}) => {
  const { canAccess } = usePermission();
  const allowed = canAccess(access, accessMode);
  const resolvedDisabled = disabled || !allowed;

  const buttonNode = (
    <Button {...buttonProps} disabled={resolvedDisabled}>
      {children}
    </Button>
  );

  if (allowed) {
    return buttonNode;
  }

  return (
    <Tooltip title={noPermissionTip}>
      <span
        onClick={(event) => {
          event.preventDefault();
          event.stopPropagation();
        }}
      >
        {buttonNode}
      </span>
    </Tooltip>
  );
};

export default PermissionButton;
