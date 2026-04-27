import { Drawer } from 'antd';
import React from 'react';
import WalletBalanceTab from './WalletBalanceTab';
import styles from './UserWalletModal.module.less';

/**
 * 用户钱包抽屉宽度。
 */
const USER_WALLET_DRAWER_WIDTH = 900;

/**
 * 用户钱包弹窗属性。
 */
interface UserWalletModalProps {
  /** 是否打开弹窗。 */
  open: boolean;
  /** 关闭弹窗回调。 */
  onClose: () => void;
  /** 用户ID。 */
  userId?: number | null;
}

/**
 * 用户钱包弹窗。
 * @param props 组件属性。
 * @returns 用户钱包弹窗节点。
 */
const UserWalletModal: React.FC<UserWalletModalProps> = ({ open, onClose, userId }) => {
  return (
    <Drawer
      title="用户钱包"
      open={open}
      onClose={onClose}
      width={USER_WALLET_DRAWER_WIDTH}
      placement="right"
      rootClassName={styles.walletDrawer}
      destroyOnHidden
    >
      {userId ? <WalletBalanceTab userId={userId} containerPadding={0} /> : null}
    </Drawer>
  );
};

export default UserWalletModal;
