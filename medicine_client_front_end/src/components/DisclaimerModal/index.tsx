/**
 * 免责声明弹窗组件。
 *
 * 首次进入首页时展示，用户点击"我已知晓并同意"后
 * 将状态持久化到 localStorage，后续不再弹出。
 */

import React from 'react'
import { AlertTriangle } from 'lucide-react'
import { markDisclaimerAgreed } from '@/utils/disclaimer'
import styles from './index.module.less'

/**
 * 组件属性。
 */
interface DisclaimerModalProps {
  /** 是否展示弹窗 */
  open: boolean
  /** 用户同意后的回调 */
  onAgree: () => void
}

/**
 * 免责声明弹窗。
 *
 * @param props 组件属性
 * @returns React 节点
 */
const DisclaimerModal: React.FC<DisclaimerModalProps> = ({ open, onAgree }) => {
  if (!open) {
    return null
  }

  const handleAgree = (): void => {
    markDisclaimerAgreed()
    onAgree()
  }

  return (
    <div className={styles.mask}>
      <div className={styles.panel} onClick={e => e.stopPropagation()}>
        <div className={styles.header}>
          <div className={styles.iconWrap}>
            <AlertTriangle size={26} />
          </div>
          <h3 className={styles.title}>免责声明</h3>
        </div>

        <div className={styles.body}>
          <p className={styles.content}>
            本系统仅供<span className={styles.warning}>个人面试作品展示</span>
            使用，系统内所有药品数据均从网络公开渠道获取，
            <span className={styles.highlight}>不代表真实药品信息</span>
            ，系统也不会提供任何实际的医药相关服务。
            <br />
            <br />
            <span className={styles.highlight}>请勿在平台上发布任何违法违规信息</span>。
          </p>
          <p className={styles.contact}>系统内数据均来源于网络，如有侵权请联系删除</p>
        </div>

        <div className={styles.footer}>
          <button className={styles.agreeBtn} onClick={handleAgree} type='button'>
            我已知晓并同意
          </button>
        </div>
      </div>
    </div>
  )
}

export default DisclaimerModal
