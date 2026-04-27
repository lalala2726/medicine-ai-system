import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Dialog } from '@nutui/nutui-react'
import { ChevronLeft, MapPin, Edit3, Trash2, Star, Plus } from 'lucide-react'
import { getAddressList, deleteAddress, setDefaultAddress, type UserAddressTypes } from '@/api/userAddress'
import AddressForm from '@/components/AddressForm'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import styles from './index.module.less'
import Empty from '@/components/Empty'
import SkeletonBlock from '@/components/SkeletonBlock'

/**
 * 地址列表首屏骨架屏数量。
 */
const ADDRESS_SKELETON_COUNT = 3

/**
 * 渲染地址列表骨架屏卡片。
 *
 * @param index - 骨架屏卡片序号。
 * @returns 地址卡片骨架节点。
 */
const renderAddressSkeletonCard = (index: number) => (
  <div key={`address-skeleton-${index}`} className={styles.addressSkeletonCard} aria-hidden='true'>
    <div className={styles.addressSkeletonHeader}>
      <div className={styles.addressSkeletonUser}>
        <SkeletonBlock className={styles.addressSkeletonName} />
        <SkeletonBlock className={styles.addressSkeletonPhone} />
      </div>
      <SkeletonBlock className={styles.addressSkeletonBadge} />
    </div>
    <div className={styles.addressSkeletonDetail}>
      <SkeletonBlock className={styles.addressSkeletonIcon} />
      <div className={styles.addressSkeletonTextGroup}>
        <SkeletonBlock className={styles.addressSkeletonText} />
        <SkeletonBlock className={styles.addressSkeletonTextShort} />
      </div>
    </div>
    <div className={styles.addressSkeletonActions}>
      <SkeletonBlock className={styles.addressSkeletonAction} />
      <SkeletonBlock className={styles.addressSkeletonAction} />
      <SkeletonBlock className={styles.addressSkeletonAction} />
    </div>
  </div>
)

const AddressList: React.FC = () => {
  const navigate = useNavigate()
  const [addressList, setAddressList] = useState<UserAddressTypes.UserAddressVo[]>([])
  const [loading, setLoading] = useState(false)
  const [showAddressForm, setShowAddressForm] = useState(false)
  const [editingAddress, setEditingAddress] = useState<UserAddressTypes.UserAddressVo | null>(null)

  // 加载地址列表
  const loadAddressList = async () => {
    try {
      setLoading(true)
      const list = await getAddressList()
      setAddressList(list)
    } catch (error) {
      console.error('加载地址列表失败:', error)
      showNotify('加载地址列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAddressList()
  }, [])

  // 返回上一页
  const handleBack = () => {
    navigate(-1)
  }

  // 编辑地址
  const handleEditAddress = (address: UserAddressTypes.UserAddressVo) => {
    setEditingAddress(address)
    setShowAddressForm(true)
  }

  // 删除地址
  const handleDeleteAddress = async (addressId: string) => {
    Dialog.confirm({
      title: '确认删除',
      content: '确定要删除这个地址吗?',
      onConfirm: async () => {
        try {
          await deleteAddress(addressId)
          showSuccessNotify('删除成功')
          loadAddressList()
        } catch (error) {
          console.error('删除地址失败:', error)
        }
      }
    })
  }

  // 设置默认地址
  const handleSetDefault = async (addressId: string) => {
    try {
      await setDefaultAddress(addressId)
      showSuccessNotify('设置成功')
      loadAddressList()
    } catch (error) {
      console.error('设置默认地址失败:', error)
    }
  }

  // 新增地址
  const handleAddAddress = () => {
    setEditingAddress(null)
    setShowAddressForm(true)
  }

  // 表单保存成功
  const handleFormSuccess = () => {
    setShowAddressForm(false)
    setEditingAddress(null)
    loadAddressList()
  }

  // 格式化完整地址
  const formatFullAddress = (address: UserAddressTypes.UserAddressVo) => {
    const parts = [address.address, address.detailAddress].filter(Boolean)
    return parts.join(' ')
  }

  return (
    <div className={styles.addressListPage}>
      {/* 顶部导航栏 */}
      <div className={styles.pageHeader}>
        <div className={styles.headerLeft} onClick={handleBack}>
          <ChevronLeft size={24} />
        </div>
        <div className={styles.headerTitle}>收货地址</div>
        <div className={styles.headerRight}></div>
      </div>

      {/* 地址列表内容 */}
      <div className={styles.pageContent}>
        {loading && addressList.length === 0 ? (
          <div className={styles.addressSkeletonList}>
            {Array.from({ length: ADDRESS_SKELETON_COUNT }).map((_, index) => renderAddressSkeletonCard(index))}
          </div>
        ) : addressList.length === 0 ? (
          <div className={styles.emptyWrapper}>
            <Empty description='暂无收货地址' />
            <Button type='primary' onClick={handleAddAddress} className={styles.emptyAddBtn}>
              添加收货地址
            </Button>
          </div>
        ) : (
          <div className={styles.addressList}>
            {addressList.map(address => (
              <div
                key={address.id}
                className={`${styles.addressCard} ${address.isDefault === 1 ? styles.active : ''}`}
                onClick={() => handleEditAddress(address)}
              >
                <div className={styles.addressHeader}>
                  <div className={styles.addressUser}>
                    <span className={styles.userName}>{address.receiverName}</span>
                    <span className={styles.userPhone}>{address.receiverPhone}</span>
                  </div>
                  {address.isDefault === 1 && <div className={styles.defaultBadge}>默认</div>}
                </div>

                <div className={styles.addressDetail}>
                  <MapPin size={14} className={styles.locationIcon} />
                  <span className={styles.addressText}>{formatFullAddress(address)}</span>
                </div>

                <div className={styles.addressActions}>
                  <div
                    className={`${styles.actionBtn} ${address.isDefault === 1 ? styles.active : ''}`}
                    onClick={e => {
                      e.stopPropagation()
                      handleSetDefault(address.id!)
                    }}
                  >
                    <Star size={16} fill={address.isDefault === 1 ? 'currentColor' : 'none'} />
                    <span>{address.isDefault === 1 ? '已设为默认' : '设为默认'}</span>
                  </div>
                  <div
                    className={styles.actionBtn}
                    onClick={e => {
                      e.stopPropagation()
                      handleEditAddress(address)
                    }}
                  >
                    <Edit3 size={16} />
                    <span>编辑</span>
                  </div>
                  <div
                    className={styles.actionBtn}
                    onClick={e => {
                      e.stopPropagation()
                      handleDeleteAddress(address.id!)
                    }}
                  >
                    <Trash2 size={16} />
                    <span>删除</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 底部新增按钮 */}
      {addressList.length > 0 && (
        <div className={styles.pageFooter}>
          <Button block type='primary' onClick={handleAddAddress}>
            <Plus size={20} />
            <span style={{ marginLeft: '8px' }}>新增收货地址</span>
          </Button>
        </div>
      )}

      {/* 地址表单弹窗 */}
      <AddressForm
        visible={showAddressForm}
        editData={editingAddress}
        onClose={() => {
          setShowAddressForm(false)
          setEditingAddress(null)
        }}
        onSuccess={handleFormSuccess}
      />
    </div>
  )
}

export default AddressList
