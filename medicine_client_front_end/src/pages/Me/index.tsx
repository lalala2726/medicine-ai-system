import React, { useEffect, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Badge } from '@nutui/nutui-react'
import {
  CreditCard,
  Package,
  Truck,
  CheckCircle,
  RotateCcw,
  MapPin,
  User,
  History,
  TicketPercent,
  ShieldCheck,
  ChevronRight,
  LogOut
} from 'lucide-react'
import { useUserStore } from '@/stores/userStore'
import { useAuthStore } from '@/store/auth'
import { getUserBrief, getUserProfile } from '@/api/user'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import styles from './index.module.less'
import { useAvatarTransitionStore } from '@/stores/avatarTransitionStore'
import AvatarTransition from '@/components/AvatarTransition'
import PullRefresh from '@/components/PullRefresh'

const Me: React.FC = () => {
  const navigate = useNavigate()
  const { user, orderStats, updateFromBrief, updateFromProfile, setLoading } = useUserStore()
  const { clearTokens } = useAuthStore()
  const mePageRef = useRef<HTMLDivElement>(null)
  const avatarRef = useRef<HTMLImageElement>(null)
  const { setSourceRect, startAnimation, isAnimating, direction } = useAvatarTransitionStore()

  // 获取用户简要信息
  const fetchUserBrief = useCallback(async () => {
    try {
      const brief = await getUserBrief()
      updateFromBrief(brief)
    } catch (error) {
      console.error('获取用户简要信息失败:', error)
    }
  }, [updateFromBrief])

  // 获取用户详细资料
  const fetchUserProfileData = useCallback(async () => {
    try {
      const profile = await getUserProfile()
      updateFromProfile(profile)
    } catch (error) {
      console.error('获取用户资料失败:', error)
    }
  }, [updateFromProfile])

  // 页面加载时获取用户信息
  useEffect(() => {
    const initData = async () => {
      setLoading(true)
      try {
        await Promise.all([fetchUserBrief(), fetchUserProfileData()])
      } catch (error) {
        console.error('初始化数据失败', error)
        showNotify('获取用户信息失败')
      } finally {
        setLoading(false)
      }
    }
    initData()
  }, [fetchUserBrief, fetchUserProfileData, setLoading])

  // 下拉刷新处理
  /**
   * 处理个人中心下拉刷新。
   *
   * @returns Promise<void> 无返回值。
   */
  const handleRefresh = async (): Promise<void> => {
    await Promise.all([fetchUserBrief(), fetchUserProfileData()])
  }

  // 退出登录处理
  const handleLogout = () => {
    clearTokens()
    showSuccessNotify('退出登录成功')
    navigate('/login', { replace: true })
  }

  /**
   * 跳转到安全设置页面。
   *
   * @returns 无返回值
   */
  const handleOpenSecuritySettingsPage = (): void => {
    navigate('/security/settings')
  }

  // 订单类型
  const orderTypes = [
    {
      icon: <CreditCard size={24} />,
      label: '待支付',
      count: orderStats.pending,
      onClick: () => navigate('/orders?status=PENDING_PAYMENT')
    },
    {
      icon: <Package size={24} />,
      label: '待发货',
      count: orderStats.shipped,
      onClick: () => navigate('/orders?status=PENDING_SHIPMENT')
    },
    {
      icon: <Truck size={24} />,
      label: '待收货',
      count: orderStats.received,
      onClick: () => navigate('/orders?status=PENDING_RECEIPT')
    },
    {
      icon: <CheckCircle size={24} />,
      label: '已完成',
      count: 0,
      onClick: () => navigate('/orders?status=COMPLETED')
    },
    {
      icon: <RotateCcw size={24} />,
      label: '售后',
      count: orderStats.refund,
      onClick: () => navigate('/after-sale')
    }
  ]

  // 服务菜单 (Grid Layout)
  const serviceMenus = [
    {
      icon: <MapPin size={20} />,
      label: '收货地址',
      onClick: () => navigate('/address/list')
    },
    {
      icon: <CreditCard size={20} />,
      label: '我的钱包',
      onClick: () => navigate('/wallet')
    },
    {
      icon: <TicketPercent size={20} />,
      label: '我的优惠券',
      onClick: () => navigate('/coupon')
    },
    {
      icon: <User size={20} />,
      label: '就诊人',
      onClick: () => navigate('/patient/list')
    },
    {
      icon: <History size={20} />,
      label: '浏览历史',
      onClick: () => navigate('/view-history')
    },
    {
      icon: <ShieldCheck size={20} />,
      label: '安全设置',
      onClick: handleOpenSecuritySettingsPage
    }
  ]

  return (
    <div className={styles.mePage} ref={mePageRef}>
      <PullRefresh mode='external' scrollTargetRef={mePageRef} onRefresh={handleRefresh}>
        <div className={styles.meContainer}>
          {/* 顶部用户信息区 - Modern Clean Style */}
          <div className={styles.userHeader}>
            <div
              className={styles.userInfo}
              onClick={() => {
                // 获取头像位置并启动过渡动画
                if (avatarRef.current) {
                  const rect = avatarRef.current.getBoundingClientRect()
                  setSourceRect({ top: rect.top, left: rect.left, width: rect.width, height: rect.height }, user.avatar)
                  startAnimation()
                }
                navigate('/profile/edit')
              }}
            >
              <div className={styles.userDetails}>
                <div className={styles.userName}>
                  <span className={styles.nickname}>{user.nickname || '未设置昵称'}</span>
                </div>
                <div className={styles.userPhone}>{user.phone || '尚未绑定手机号'}</div>
              </div>
              <img
                ref={avatarRef}
                src={
                  user.avatar ||
                  'https://img12.360buyimg.com/imagetools/jfs/t1/196430/38/8105/14329/60c806a4Ed5062986/454e99fc3060965d.png'
                }
                alt='头像'
                className={styles.avatar}
                style={{ opacity: isAnimating && direction === 'backward' ? 0 : 1 }}
              />
            </div>
            <AvatarTransition targetRef={avatarRef} expectedDirection='backward' />
            <div className={styles.headerStats}>
              <div className={styles.statItem} onClick={() => navigate('/coupon')}>
                <span className={styles.statValue}>{user.coupons || 0}</span>
                <span className={styles.statLabel}>优惠券</span>
              </div>
              <div className={styles.statItem} onClick={() => navigate('/wallet')}>
                <span className={styles.statValue}>{user.balance || '0.00'}</span>
                <span className={styles.statLabel}>余额</span>
              </div>
            </div>
          </div>

          {/* 我的订单 */}
          <div className={styles.sectionCard}>
            <div className={styles.sectionHeader}>
              <span className={styles.sectionTitle}>我的订单</span>
              <div className={styles.sectionMore} onClick={() => navigate('/orders')}>
                <span>全部订单</span>
                <ChevronRight size={14} />
              </div>
            </div>
            <div className={styles.orderTypes}>
              {orderTypes.map((type, index) => (
                <div key={index} className={styles.orderTypeItem} onClick={type.onClick}>
                  <div className={styles.orderIconWrapper}>
                    {type.count > 0 ? (
                      <Badge value={type.count} top='-4px' right='-4px'>
                        <div className={styles.orderIcon}>{type.icon}</div>
                      </Badge>
                    ) : (
                      <div className={styles.orderIcon}>{type.icon}</div>
                    )}
                  </div>
                  <div className={styles.orderLabel}>{type.label}</div>
                </div>
              ))}
            </div>
          </div>

          {/* 我的服务 - Grid Layout */}
          <div className={styles.sectionCard}>
            <div className={styles.sectionHeader}>
              <span className={styles.sectionTitle}>我的服务</span>
            </div>
            <div className={styles.serviceGrid}>
              {serviceMenus.map((menu, index) => (
                <div key={index} className={styles.serviceItem} onClick={menu.onClick}>
                  <div className={styles.serviceIcon}>{menu.icon}</div>
                  <span className={styles.serviceLabel}>{menu.label}</span>
                </div>
              ))}
            </div>
          </div>

          {/* 系统设置 - List Layout */}
          <div className={styles.sectionCard} style={{ marginTop: 0 }}>
            <div className={styles.menuItem} onClick={handleLogout}>
              <div className={styles.menuLeft}>
                <div className={`${styles.menuIcon} ${styles.logoutIcon}`}>
                  <LogOut size={18} />
                </div>
                <span className={`${styles.menuLabel} ${styles.logoutLabel}`}>退出当前账号</span>
              </div>
              <ChevronRight size={16} color='#ccc' />
            </div>
          </div>
        </div>
      </PullRefresh>
    </div>
  )
}

export default Me
