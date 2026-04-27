import React, { useState, useEffect } from 'react'
import { Popup, Button, Dialog } from '@nutui/nutui-react'
import { Location, Edit, Del, Star, StarFill } from '@nutui/icons-react'
import { getAddressList, deleteAddress, setDefaultAddress, type UserAddressTypes } from '@/api/userAddress'
import AddressForm from '@/components/AddressForm'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import styles from './index.module.less'
import Empty from '@/components/Empty'
import SkeletonBlock from '@/components/SkeletonBlock'

/**
 * 地址选择弹层骨架屏数量。
 */
const ADDRESS_SELECTOR_SKELETON_COUNT = 3

interface AddressSelectorProps {
  visible: boolean
  onClose: () => void
  onSelect: (address: UserAddressTypes.UserAddressVo) => void
}

/**
 * 渲染地址选择弹层骨架屏卡片。
 *
 * @param index - 骨架屏卡片序号。
 * @returns 地址选择骨架节点。
 */
const renderAddressSelectorSkeletonCard = (index: number) => (
  <div key={`address-selector-skeleton-${index}`} className={styles.addressSkeletonCard} aria-hidden='true'>
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

const AddressSelector: React.FC<AddressSelectorProps> = ({ visible, onClose, onSelect }) => {
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
    if (visible) {
      loadAddressList()
    }
  }, [visible])

  // 选择地址
  const handleSelectAddress = (address: UserAddressTypes.UserAddressVo) => {
    onSelect(address)
    onClose()
  }

  // 编辑地址
  const handleEditAddress = (e: React.MouseEvent, address: UserAddressTypes.UserAddressVo) => {
    e.stopPropagation()
    setEditingAddress(address)
    setShowAddressForm(true)
  }

  // 删除地址
  const handleDeleteAddress = async (e: React.MouseEvent, addressId: string) => {
    e.stopPropagation()

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
  const handleSetDefault = async (e: React.MouseEvent, addressId: string) => {
    e.stopPropagation()

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
    <>
      <Popup visible={visible} position='bottom' round closeable onClose={onClose} style={{ height: '60vh' }}>
        <div className={styles.addressSelector}>
          <div className={styles.selectorHeader}>
            <h3 className={styles.selectorTitle}>选择收货地址</h3>
          </div>

          <div className={styles.selectorContent}>
            {loading ? (
              <div className={styles.addressSkeletonList}>
                {Array.from({ length: ADDRESS_SELECTOR_SKELETON_COUNT }).map((_, index) =>
                  renderAddressSelectorSkeletonCard(index)
                )}
              </div>
            ) : addressList.length === 0 ? (
              <Empty description='暂无收货地址' />
            ) : (
              <div className={styles.addressList}>
                {addressList.map(address => (
                  <div
                    key={address.id}
                    className={`${styles.addressCard} ${address.isDefault === 1 ? styles.active : ''}`}
                    onClick={() => handleSelectAddress(address)}
                  >
                    <div className={styles.addressHeader}>
                      <div className={styles.addressUser}>
                        <span className={styles.userName}>{address.receiverName}</span>
                        <span className={styles.userPhone}>{address.receiverPhone}</span>
                      </div>
                      {address.isDefault === 1 && <div className={styles.defaultBadge}>默认</div>}
                    </div>

                    <div className={styles.addressDetail}>
                      <Location width={14} height={14} className={styles.locationIcon} />
                      <span className={styles.addressText}>{formatFullAddress(address)}</span>
                    </div>

                    <div className={styles.addressActions}>
                      <div className={styles.actionBtn} onClick={e => handleSetDefault(e, address.id!)}>
                        {address.isDefault === 1 ? (
                          <StarFill width={16} height={16} color='#fa2c19' />
                        ) : (
                          <Star width={16} height={16} />
                        )}
                        <span>默认</span>
                      </div>
                      <div className={styles.actionBtn} onClick={e => handleEditAddress(e, address)}>
                        <Edit width={16} height={16} />
                        <span>编辑</span>
                      </div>
                      <div className={styles.actionBtn} onClick={e => handleDeleteAddress(e, address.id!)}>
                        <Del width={16} height={16} />
                        <span>删除</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className={styles.selectorFooter}>
            <Button block type='primary' onClick={handleAddAddress}>
              新增收货地址
            </Button>
          </div>
        </div>
      </Popup>

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
    </>
  )
}

export default AddressSelector
