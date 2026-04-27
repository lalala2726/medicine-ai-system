import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserTypes } from '@/api/user'

export interface BriefInfo {
  nickname: string
  avatar: string
  phone: string
  vipLevel: number
  coupons: number
  points: number
  balance: string
}

export interface ProfileInfo {
  realName?: string
  phoneNumber?: string
  birthday?: string
  avatar?: string
  nickname?: string
}

export interface UserProfile extends BriefInfo {
  realName?: string
  birthday?: string
}

export interface PointsInfo {
  totalPoints: number
  currentLevel: string
  growthValue: number
  maxGrowthValue: number
  nextLevel: string
}

export interface OrderStats {
  pending: number
  shipped: number
  received: number
  cancelled: number
  refund: number
  complete: number
}

interface UserStore {
  user: UserProfile
  profile: ProfileInfo
  pointsInfo: PointsInfo
  orderStats: OrderStats
  loading: boolean

  updateUser: (_user: Partial<UserProfile>) => void
  updatePoints: (_points: Partial<PointsInfo>) => void
  updateOrderStats: (_stats: Partial<OrderStats>) => void
  updateFromBrief: (_brief: UserTypes.UserBriefVo) => void
  updateFromProfile: (_profile: UserTypes.UserProfileDto) => void
  addPoints: (_points: number) => void
  deductPoints: (_points: number) => void
  setLoading: (_loading: boolean) => void
}

export const useUserStore = create<UserStore>()(
  persist(
    (set, get) => ({
      user: {
        nickname: '健康用户',
        realName: '',
        avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
        phone: '138****8888',
        vipLevel: 1,
        coupons: 0,
        points: 0,
        balance: '0.00'
      },

      profile: {
        realName: '',
        phoneNumber: '',
        birthday: '',
        avatar: '',
        nickname: ''
      },

      pointsInfo: {
        totalPoints: 2500,
        currentLevel: 'V1',
        growthValue: 0,
        maxGrowthValue: 200,
        nextLevel: 'V2'
      },

      orderStats: {
        pending: 0,
        shipped: 0,
        received: 0,
        cancelled: 0,
        refund: 0,
        complete: 0
      },

      loading: false,

      updateUser: userUpdate => {
        set(state => ({
          user: { ...state.user, ...userUpdate }
        }))
      },

      updatePoints: pointsUpdate => {
        set(state => ({
          pointsInfo: { ...state.pointsInfo, ...pointsUpdate }
        }))
      },

      updateOrderStats: statsUpdate => {
        set(state => ({
          orderStats: { ...state.orderStats, ...statsUpdate }
        }))
      },

      updateFromBrief: brief => {
        // 只更新脱敏的简要信息（来自 /user/brief）
        set(state => ({
          user: {
            ...state.user,
            avatar: brief.avatarUrl || state.user.avatar,
            nickname: brief.nickName || state.user.nickname,
            phone: brief.phoneNumber || state.user.phone,
            balance: brief.balance || '0.00',
            coupons: brief.couponCount || 0
          },
          orderStats: {
            pending: brief.payOrderCount || 0,
            shipped: brief.deliverOrderCount || 0,
            received: brief.receiveOrderCount || 0,
            cancelled: 0,
            refund: brief.afterSaleOrderCount || 0,
            complete: brief.completeOrderCount || 0
          }
        }))
      },

      updateFromProfile: profile => {
        // 只更新完整的未脱敏资料（来自 /user/profile）
        set(state => ({
          profile: {
            realName: profile.realName ?? state.profile.realName,
            phoneNumber: profile.phoneNumber ?? state.profile.phoneNumber,
            birthday: profile.birthday ?? state.profile.birthday,
            avatar: profile.avatar ?? state.profile.avatar,
            nickname: profile.nickname ?? state.profile.nickname
          }
        }))
      },

      addPoints: points => {
        const state = get()
        const newTotal = state.user.points + points
        const newGrowth = Math.min(
          state.pointsInfo.growthValue + Math.floor(points / 10),
          state.pointsInfo.maxGrowthValue
        )

        set({
          user: { ...state.user, points: newTotal },
          pointsInfo: { ...state.pointsInfo, growthValue: newGrowth }
        })
      },

      deductPoints: points => {
        const state = get()
        const newTotal = Math.max(state.user.points - points, 0)

        set({
          user: { ...state.user, points: newTotal }
        })
      },

      setLoading: loading => {
        set({ loading })
      }
    }),
    {
      name: 'user-storage'
    }
  )
)
