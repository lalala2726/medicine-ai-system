import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Checkbox, Dialog } from '@nutui/nutui-react'
import { ChevronLeft, Trash2, ShoppingBag } from 'lucide-react'
import { useCartStore } from '@/stores/cartStore'
import type { CartItem } from '@/stores/cartStore'
import CartItemCard from './components/CartItemCard'
import { showNotify } from '@/utils/notify'
import PullRefresh from '@/components/PullRefresh'
import SkeletonBlock from '@/components/SkeletonBlock'
import styles from './index.module.less'
import Empty from '@/components/Empty'

/**
 * 购物车首屏骨架屏数量。
 */
const CART_SKELETON_COUNT = 3

/**
 * 渲染购物车商品骨架屏卡片。
 *
 * @param index - 骨架屏卡片序号。
 * @returns 购物车商品骨架节点。
 */
const renderCartSkeletonCard = (index: number) => (
  <div key={`cart-skeleton-${index}`} className={styles.cartSkeletonCard} aria-hidden='true'>
    <SkeletonBlock className={styles.cartSkeletonCheckbox} />
    <SkeletonBlock className={styles.cartSkeletonImage} />
    <div className={styles.cartSkeletonInfo}>
      <SkeletonBlock className={styles.cartSkeletonTitle} />
      <SkeletonBlock className={styles.cartSkeletonTitleShort} />
      <SkeletonBlock className={styles.cartSkeletonSpec} />
      <div className={styles.cartSkeletonBottom}>
        <SkeletonBlock className={styles.cartSkeletonPrice} />
        <SkeletonBlock className={styles.cartSkeletonQuantity} />
      </div>
    </div>
  </div>
)

const Cart: React.FC = () => {
  const navigate = useNavigate()
  const {
    items,
    loading,
    fetchCartList,
    removeItem,
    removeItems,
    updateQuantity,
    toggleSelect,
    toggleSelectAll,
    getTotalPrice,
    isAllSelected
  } = useCartStore()

  const [editMode, setEditMode] = useState(false)

  // 组件挂载时从后端加载购物车数据
  useEffect(() => {
    fetchCartList()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 返回上一页
  const handleBack = () => {
    navigate(-1)
  }

  // 切换编辑模式
  const handleToggleEditMode = () => {
    setEditMode(!editMode)
  }

  // 删除单个商品（滑动删除，不需要确认）
  const handleDelete = async (item: CartItem) => {
    await removeItem(item.id)
  }

  // 删除单个商品（点击删除，需要确认）
  const handleDeleteWithConfirm = (item: CartItem) => {
    Dialog.confirm({
      title: '确认删除',
      content: `确定要删除"${item.name}"吗？`,
      onConfirm: async () => {
        await removeItem(item.id)
      }
    })
  }

  // 删除选中的商品
  const handleDeleteSelected = () => {
    const selectedItems = items.filter(item => item.selected)
    if (selectedItems.length === 0) {
      showNotify('请先选择要删除的商品')
      return
    }

    Dialog.confirm({
      title: '确认删除',
      content: `确定要删除选中的 ${selectedItems.length} 件商品吗？`,
      onConfirm: async () => {
        const cartIds = selectedItems.map(item => item.id)
        await removeItems(cartIds)
        setEditMode(false)
      }
    })
  }

  // 结算
  const handleCheckout = () => {
    const selectedItems = items.filter(item => item.selected)
    if (selectedItems.length === 0) {
      showNotify('请先选择要结算的商品')
      return
    }
    // 跳转到结算页面
    navigate('/checkout')
  }

  // 跳转到商品详情
  const handleProductClick = (productId: number) => {
    navigate(`/product/${productId}`)
  }

  /**
   * 跳转到首页继续浏览商品。
   *
   * @returns void 无返回值。
   */
  const handleGoHome = (): void => {
    navigate('/')
  }

  // 下拉刷新处理
  /**
   * 处理购物车下拉刷新。
   *
   * @returns Promise<void> 无返回值。
   */
  const handleRefresh = async (): Promise<void> => {
    try {
      await fetchCartList()
    } catch {
      showNotify('刷新失败，请稍后重试')
    }
  }

  // 计算选中商品数量
  const selectedCount = items.filter(item => item.selected).length

  // 计算总价
  const totalPrice = getTotalPrice()

  return (
    <div className={styles.cartPage}>
      {/* 顶部导航栏 */}
      <div className={styles.cartHeader}>
        <div className={styles.headerLeft} onClick={handleBack}>
          <ChevronLeft size={24} color='#0d1b12' />
        </div>
        <div className={styles.headerTitle}>购物车</div>
        <div className={styles.headerRight} onClick={items.length > 0 ? handleToggleEditMode : undefined}>
          {items.length > 0 && (editMode ? '完成' : '管理')}
        </div>
      </div>

      <PullRefresh mode='self' style={{ backgroundColor: 'var(--background-light)' }} onRefresh={handleRefresh}>
        {/* 购物车列表 */}
        <div className={styles.cartContent}>
          <div className={styles.cartContentWrapper}>
            {loading ? (
              <div className={styles.cartList}>
                {Array.from({ length: CART_SKELETON_COUNT }).map((_, index) => renderCartSkeletonCard(index))}
              </div>
            ) : items.length === 0 ? (
              <Empty
                image={<ShoppingBag size={64} color='var(--nutui-color-primary)' opacity={0.6} />}
                description='您的购物车还是空的'
              >
                <Button type='primary' className={styles.emptyActionButton} onClick={handleGoHome}>
                  去看看
                </Button>
              </Empty>
            ) : (
              <div className={styles.cartList}>
                {items.map(item => (
                  <CartItemCard
                    key={item.id}
                    item={item}
                    editMode={editMode}
                    onToggleSelect={toggleSelect}
                    onUpdateQuantity={updateQuantity}
                    onDelete={handleDelete}
                    onProductClick={handleProductClick}
                    onDeleteWithConfirm={handleDeleteWithConfirm}
                  />
                ))}
              </div>
            )}
          </div>
        </div>
      </PullRefresh>

      {/* 底部结算栏 */}
      {items.length > 0 && (
        <div className={styles.cartFooter}>
          <div className={styles.footerLeft}>
            <Checkbox checked={isAllSelected()} onChange={toggleSelectAll}>
              全选
            </Checkbox>
          </div>
          <div className={styles.footerRight}>
            {!editMode ? (
              <>
                <div className={styles.totalInfo}>
                  <div className={styles.totalLabel}>合计:</div>
                  <div className={styles.totalPrice}>
                    <span className={styles.priceSymbol}>¥</span>
                    <span className={styles.priceValue}>{totalPrice.toFixed(2)}</span>
                  </div>
                </div>
                <Button type='primary' className={styles.checkoutBtn} onClick={handleCheckout}>
                  结算({selectedCount})
                </Button>
              </>
            ) : (
              <Button type='primary' className={styles.deleteBtn} onClick={handleDeleteSelected}>
                <Trash2 size={16} style={{ marginRight: '4px' }} />
                删除({selectedCount})
              </Button>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default Cart
