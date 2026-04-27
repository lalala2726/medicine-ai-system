import {
  ApiOutlined,
  BookOutlined,
  CloudOutlined,
  ClusterOutlined,
  DashboardOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  LockOutlined,
  MessageOutlined,
  RobotOutlined,
  SettingOutlined,
  ShopOutlined,
  ShoppingOutlined,
  SmileOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import React from 'react';

const DEFAULT_ICON_SIZE = 16;
const iconDefaultSizes: Partial<Record<string, number>> = {
  'llms-settings': 19,
  provider: 17,
};
const systemIconRegistry = {
  api: ApiOutlined,
  book: BookOutlined,
  cloud: CloudOutlined,
  cluster: ClusterOutlined,
  dashboard: DashboardOutlined,
  experiment: ExperimentOutlined,
  'file-search': FileSearchOutlined,
  'file-text': FileTextOutlined,
  lock: LockOutlined,
  message: MessageOutlined,
  robot: RobotOutlined,
  setting: SettingOutlined,
  shop: ShopOutlined,
  shopping: ShoppingOutlined,
  smile: SmileOutlined,
  team: TeamOutlined,
  user: UserOutlined,
} as const;

export interface AppIconProps {
  name: string;
  size?: number | string;
  className?: string;
}

const AppIcon: React.FC<AppIconProps> = ({ name, size, className }) => {
  const [hasError, setHasError] = React.useState(false);
  const resolvedSize = size ?? iconDefaultSizes[name] ?? DEFAULT_ICON_SIZE;
  const src = `/icons/${name}.svg`;
  const SystemIcon = systemIconRegistry[name as keyof typeof systemIconRegistry];

  React.useEffect(() => {
    setHasError(false);
  }, [name]);

  if (SystemIcon) {
    return <SystemIcon className={className} style={{ fontSize: resolvedSize }} />;
  }

  if (hasError) {
    return null;
  }

  return (
    <>
      <img
        alt=""
        aria-hidden="true"
        data-icon-loader={name}
        draggable={false}
        onError={() => setHasError(true)}
        src={src}
        style={{ display: 'none' }}
      />
      <span
        aria-hidden="true"
        className={className}
        data-icon={name}
        style={{
          display: 'inline-block',
          width: resolvedSize,
          height: resolvedSize,
          flexShrink: 0,
          backgroundColor: 'currentColor',
          maskImage: `url(${src})`,
          maskRepeat: 'no-repeat',
          maskPosition: 'center',
          maskSize: 'contain',
          WebkitMaskImage: `url(${src})`,
          WebkitMaskRepeat: 'no-repeat',
          WebkitMaskPosition: 'center',
          WebkitMaskSize: 'contain',
        }}
      />
    </>
  );
};

AppIcon.displayName = 'AppIcon';

export default AppIcon;
