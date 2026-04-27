import React from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, ChevronRight, ShieldCheck, Smartphone, Lock } from 'lucide-react'
import { useUserStore } from '@/stores/userStore'
import styles from './index.module.less'

/**
 * 安全设置页面。
 *
 * @returns 页面节点
 */
const SecuritySettings: React.FC = () => {
  const navigate = useNavigate()
  const { user } = useUserStore()

  /**
   * 返回上一页。
   *
   * @returns 无返回值
   */
  const handleBack = (): void => {
    navigate(-1)
  }

  /**
   * 安全设置菜单项。
   */
  const settingMenus = [
    {
      key: 'phone',
      icon: <Smartphone size={18} />,
      label: '修改手机号',
      desc: `当前绑定：${user.phone || '未绑定手机号'}`,
      onClick: () => navigate('/phone/change')
    },
    {
      key: 'password',
      icon: <Lock size={18} />,
      label: '修改密码',
      desc: '更新登录密码，提升账号安全性',
      onClick: () => navigate('/password/change')
    }
  ]

  return (
    <div className={styles.securitySettingsPage}>
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={handleBack}>
          <ChevronLeft size={24} />
        </div>
        <div className={styles.navTitle}>安全设置</div>
        <div className={styles.navRight} />
      </div>

      <div className={styles.pageContent}>
        <div className={styles.heroCard}>
          <div className={styles.heroIcon}>
            <ShieldCheck size={22} />
          </div>
          <div className={styles.heroTitle}>账号安全中心</div>
          <div className={styles.heroSubtitle}>您可以在这里更新手机号和登录密码，敏感操作会先进行安全校验。</div>
        </div>

        <div className={styles.menuCard}>
          {settingMenus.map(menu => (
            <div key={menu.key} className={styles.menuItem} onClick={menu.onClick}>
              <div className={styles.menuLeft}>
                <div className={styles.menuIcon}>{menu.icon}</div>
                <div className={styles.menuText}>
                  <div className={styles.menuLabel}>{menu.label}</div>
                  <div className={styles.menuDesc}>{menu.desc}</div>
                </div>
              </div>
              <ChevronRight size={18} color='#9ca3af' />
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default SecuritySettings
