import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { Button, InputNumber, Popup, TextArea } from '@nutui/nutui-react'
import { CheckCircle2, ChevronLeft, ChevronRight, Circle, Clock3, MapPin } from 'lucide-react'
import { useCartStore } from '@/stores/cartStore'
import { newOrderTypes } from '@/api/order.ts'
import type { CartItem } from '@/stores/cartStore'
import { DeliveryType } from '@/types/orderStatus'
import { getDefaultAddress, type UserAddressTypes } from '@/api/userAddress'
import AddressSelector from '@/components/AddressSelector'
import SkeletonBlock from '@/components/SkeletonBlock'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import styles from './index.module.less'

/** 优惠券弹层高度。 */
const COUPON_POPUP_HEIGHT = '72vh'

/** 结算页首屏商品骨架屏数量。 */
const CHECKOUT_GOODS_SKELETON_COUNT = 2

/** 优惠券选择模式。 */
type CouponSelectionMode = 'AUTO' | 'NONE' | 'MANUAL'

/** 立即购买路由状态数据。 */
interface BuyNowData {
  isBuyNow: boolean
  product?: CartItem
}

/**
 * 渲染结算页商品清单骨架行。
 *
 * @param index 骨架屏商品序号。
 * @returns 商品清单骨架节点。
 */
const renderCheckoutGoodsSkeleton = (index: number) => (
  <div key={`checkout-goods-skeleton-${index}`} className={styles.checkoutSkeletonGoodsItem} aria-hidden='true'>
    <SkeletonBlock className={styles.checkoutSkeletonGoodsImage} />
    <div className={styles.checkoutSkeletonGoodsInfo}>
      <SkeletonBlock className={styles.checkoutSkeletonGoodsName} />
      <SkeletonBlock className={styles.checkoutSkeletonGoodsNameShort} />
      <SkeletonBlock className={styles.checkoutSkeletonGoodsSpec} />
      <div className={styles.checkoutSkeletonGoodsBottom}>
        <SkeletonBlock className={styles.checkoutSkeletonGoodsPrice} />
        <SkeletonBlock className={styles.checkoutSkeletonGoodsQuantity} />
      </div>
    </div>
  </div>
)

/**
 * 渲染结算页首屏结构骨架。
 *
 * @returns 结算页骨架屏节点。
 */
const renderCheckoutPageSkeleton = () => (
  <>
    <div className={styles.checkoutContent}>
      <div className={`${styles.addressCard} ${styles.checkoutSkeletonAddressCard}`} aria-hidden='true'>
        <SkeletonBlock className={styles.checkoutSkeletonAddressIcon} />
        <div className={styles.checkoutSkeletonAddressInfo}>
          <SkeletonBlock className={styles.checkoutSkeletonAddressUser} />
          <SkeletonBlock className={styles.checkoutSkeletonAddressText} />
          <SkeletonBlock className={styles.checkoutSkeletonAddressTextShort} />
        </div>
      </div>

      <div className={styles.goodsCard} aria-hidden='true'>
        <div className={styles.cardHeader}>
          <SkeletonBlock className={styles.checkoutSkeletonHeaderTitle} />
        </div>
        <div className={styles.checkoutSkeletonSnapshot}>
          <div className={styles.checkoutSkeletonSnapshotMain}>
            <SkeletonBlock className={styles.checkoutSkeletonSnapshotTitle} />
            <SkeletonBlock className={styles.checkoutSkeletonSnapshotDesc} />
          </div>
          <SkeletonBlock className={styles.checkoutSkeletonSnapshotBadge} />
        </div>
        <div className={styles.goodsList}>
          {Array.from({ length: CHECKOUT_GOODS_SKELETON_COUNT }).map((_, index) => renderCheckoutGoodsSkeleton(index))}
        </div>
        <div className={styles.checkoutSkeletonOptions}>
          <div className={styles.checkoutSkeletonOption}>
            <SkeletonBlock className={styles.checkoutSkeletonOptionLabel} />
            <SkeletonBlock className={styles.checkoutSkeletonOptionValue} />
          </div>
          <div className={styles.checkoutSkeletonOption}>
            <SkeletonBlock className={styles.checkoutSkeletonOptionLabel} />
            <SkeletonBlock className={styles.checkoutSkeletonOptionValueShort} />
          </div>
        </div>
      </div>

      <div className={styles.priceCard} aria-hidden='true'>
        <div className={styles.priceHeader}>
          <SkeletonBlock className={styles.checkoutSkeletonPriceTitle} />
        </div>
        <div className={styles.checkoutSkeletonPriceRow}>
          <SkeletonBlock className={styles.checkoutSkeletonPriceLabel} />
          <SkeletonBlock className={styles.checkoutSkeletonPriceValue} />
        </div>
        <div className={styles.checkoutSkeletonPriceRow}>
          <SkeletonBlock className={styles.checkoutSkeletonPriceLabel} />
          <SkeletonBlock className={styles.checkoutSkeletonPriceValueShort} />
        </div>
        <div className={styles.checkoutSkeletonPriceRowTotal}>
          <SkeletonBlock className={styles.checkoutSkeletonPriceLabelTotal} />
          <SkeletonBlock className={styles.checkoutSkeletonPriceValueTotal} />
        </div>
      </div>
    </div>

    <div className={styles.checkoutFooter}>
      <div className={styles.footerLeft}>
        <SkeletonBlock className={styles.checkoutSkeletonFooterLabel} />
        <SkeletonBlock className={styles.checkoutSkeletonFooterTotal} />
      </div>
      <SkeletonBlock className={styles.checkoutSkeletonSubmit} />
    </div>
  </>
)

/**
 * 结算页组件。
 * @returns JSX.Element 结算页视图。
 */
const Checkout: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { getSelectedItems, fetchCartList, fetchCartCount, removeItem, updateQuantity } = useCartStore()

  const [loading, setLoading] = useState(false)
  const [selectedItems, setSelectedItems] = useState<CartItem[]>([])
  const [selectedAddress, setSelectedAddress] = useState<UserAddressTypes.UserAddressVo | null>(null)
  const [showAddressSelector, setShowAddressSelector] = useState(false)
  const [remark, setRemark] = useState('')
  const [previewData, setPreviewData] = useState<newOrderTypes.OrderPreviewVo | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [couponSelectionMode, setCouponSelectionMode] = useState<CouponSelectionMode>('AUTO')
  const [selectedCouponId, setSelectedCouponId] = useState<string | undefined>(undefined)
  const [showCouponPopup, setShowCouponPopup] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [isBuyNow, setIsBuyNow] = useState(false)
  const previewRequestVersionRef = useRef(0)

  /**
   * 初始化商品数据和默认地址。
   * @returns 无返回值。
   */
  useEffect(() => {
    const initData = async () => {
      setLoading(true)
      try {
        const buyNowData = location.state as BuyNowData

        if (buyNowData?.isBuyNow) {
          setIsBuyNow(true)
          if (buyNowData.product) {
            setSelectedItems([buyNowData.product])
          } else {
            showNotify('商品数据错误')
            navigate(-1)
            return
          }
        } else {
          const items = getSelectedItems()
          if (items.length === 0) {
            showNotify('请先选择要结算的商品')
            navigate('/cart')
            return
          }
          setSelectedItems(items)
          setIsBuyNow(false)
        }

        const defaultAddr = await getDefaultAddress()
        if (defaultAddr) {
          setSelectedAddress(defaultAddr)
        }
      } catch (error) {
        console.error('初始化失败:', error)
      } finally {
        setLoading(false)
      }
    }

    initData()
  }, [location.state, getSelectedItems, navigate])

  /**
   * 将后端金额格式化为两位小数。
   * @param amount 后端返回的金额字符串。
   * @returns string 展示用金额文本。
   */
  const formatAmount = (amount?: string) => {
    if (!amount) {
      return '0.00'
    }
    return Number(amount).toFixed(2)
  }

  /**
   * 从预览数据构建商品映射，便于按商品 ID 快速读取预览单价。
   * @returns Map<number, newOrderTypes.OrderItemPreview> 商品预览映射。
   */
  const previewItemMap = useMemo(() => {
    const itemMap = new Map<number, newOrderTypes.OrderItemPreview>()
    const previewItems = previewData?.items || []
    previewItems.forEach(item => {
      const productId = Number(item.productId || 0)
      if (productId > 0) {
        itemMap.set(productId, item)
      }
    })
    return itemMap
  }, [previewData?.items])

  /**
   * 计算购物清单商品总件数（仅用于展示件数，不参与金额计算）。
   * @returns number 商品总件数。
   */
  const totalQuantity = useMemo(() => {
    const previewItems = previewData?.items
    if (previewItems && previewItems.length > 0) {
      return previewItems.reduce((count, item) => count + Number(item.quantity || 0), 0)
    }
    return selectedItems.reduce((count, item) => count + Number(item.quantity || 0), 0)
  }, [previewData?.items, selectedItems])

  /**
   * 读取当前可选优惠券列表。
   * @returns newOrderTypes.OrderCouponOptionVo[] 优惠券候选列表。
   */
  const couponCandidates = useMemo(() => {
    return previewData?.couponCandidates || []
  }, [previewData?.couponCandidates])

  /**
   * 根据当前选择模式解析真正生效的优惠券对象。
   * @returns newOrderTypes.OrderCouponOptionVo | undefined 当前生效的优惠券对象。
   */
  const effectiveSelectedCoupon = useMemo(() => {
    if (couponSelectionMode === 'NONE') {
      return undefined
    }

    if (couponSelectionMode === 'MANUAL' && selectedCouponId) {
      const matchedCoupon = couponCandidates.find(item => item.couponId === selectedCouponId)
      if (matchedCoupon) {
        return matchedCoupon
      }

      if (previewData?.selectedCoupon?.couponId === selectedCouponId) {
        return previewData.selectedCoupon
      }
    }

    return previewData?.selectedCoupon
  }, [couponCandidates, couponSelectionMode, previewData?.selectedCoupon, selectedCouponId])

  /**
   * 当前是否由用户明确设置为不使用优惠券。
   * @returns boolean 是否明确禁用优惠券。
   */
  const couponDisabledExplicitly = couponSelectionMode === 'NONE'

  /**
   * 当前界面上是否应该高亮“不使用优惠券”。
   * @returns boolean 是否高亮“不使用优惠券”。
   */
  const noCouponSelected = useMemo(() => {
    if (couponDisabledExplicitly) {
      return true
    }
    return !effectiveSelectedCoupon?.couponId
  }, [couponDisabledExplicitly, effectiveSelectedCoupon?.couponId])

  /**
   * 计算当前订单优惠金额数值。
   * @returns number 优惠金额。
   */
  const discountAmountValue = useMemo(() => {
    return Number(previewData?.discountAmount || 0)
  }, [previewData?.discountAmount])

  /**
   * 返回当前界面应该展示的优惠金额。
   * 用户明确选择“不使用优惠券”时，前端先按 0 展示，避免保留旧预览金额。
   *
   * @returns number 当前展示的优惠金额
   */
  const displayedDiscountAmountValue = useMemo(() => {
    if (couponDisabledExplicitly) {
      return 0
    }

    return discountAmountValue
  }, [couponDisabledExplicitly, discountAmountValue])

  /**
   * 返回当前界面应该展示的实付款金额文本。
   * 用户明确禁用优惠券时，优先按商品总额展示，等待后端新预览结果回写。
   *
   * @returns string 当前展示的实付款金额文本
   */
  const displayedTotalAmountText = useMemo(() => {
    if (couponDisabledExplicitly) {
      return formatAmount(previewData?.itemsAmount)
    }

    return formatAmount(previewData?.totalAmount)
  }, [couponDisabledExplicitly, previewData?.itemsAmount, previewData?.totalAmount])

  /**
   * 返回优惠券辅助说明。
   * @returns string 优惠券辅助说明文案。
   */
  const couponHintText = useMemo(() => {
    if (!selectedAddress?.id) {
      return '选择收货地址后可计算可用优惠'
    }
    if (previewLoading) {
      return '正在为当前订单计算优惠'
    }
    if (couponDisabledExplicitly) {
      return '本次下单将不使用优惠券'
    }
    if (effectiveSelectedCoupon?.couponDeductAmount) {
      return `预计优惠 ¥${formatAmount(effectiveSelectedCoupon.couponDeductAmount)}`
    }
    if (discountAmountValue > 0) {
      return `当前已优惠 ¥${formatAmount(previewData?.discountAmount)}`
    }
    if (couponCandidates.some(item => item.matched)) {
      return '可切换其他可用优惠券'
    }
    return '当前订单暂无可用优惠券'
  }, [
    couponCandidates,
    couponDisabledExplicitly,
    discountAmountValue,
    effectiveSelectedCoupon?.couponDeductAmount,
    previewData?.discountAmount,
    previewLoading,
    selectedAddress?.id
  ])

  /**
   * 返回配送辅助说明。
   * @returns string 配送说明文案。
   */
  const deliveryHintText = useMemo(() => {
    if (!selectedAddress?.id) {
      return '请先补充收货地址'
    }
    if (previewLoading) {
      return '正在计算配送方式与时效'
    }
    return previewData?.estimatedDeliveryTime || '配送时效以下单页最终展示为准'
  }, [previewData?.estimatedDeliveryTime, previewLoading, selectedAddress?.id])

  /**
   * 根据当前优惠券选择状态生成提交订单时使用的优惠券 ID。
   * @returns string | undefined 提交订单时使用的优惠券 ID。
   */
  const checkoutCouponId = useMemo(() => {
    if (couponSelectionMode === 'NONE') {
      return undefined
    }
    if (couponSelectionMode === 'MANUAL') {
      return selectedCouponId
    }
    return previewData?.couponId
  }, [couponSelectionMode, previewData?.couponId, selectedCouponId])

  /**
   * 返回优惠券行的展示文案。
   * @returns string 优惠券摘要文案。
   */
  const couponSummaryText = useMemo(() => {
    if (!selectedAddress?.id) {
      return '请先选择收货地址'
    }
    if (previewLoading) {
      return '计算中'
    }
    if (couponDisabledExplicitly) {
      return '不使用优惠券'
    }
    if (effectiveSelectedCoupon?.couponId) {
      return `已选 ${effectiveSelectedCoupon.couponName || '优惠券'}`
    }
    const matchedCount = couponCandidates.filter(item => item.matched).length
    if (matchedCount > 0) {
      return `${matchedCount} 张可用`
    }
    return '暂无可用'
  }, [couponCandidates, couponDisabledExplicitly, effectiveSelectedCoupon, previewLoading, selectedAddress?.id])

  /**
   * 重置优惠券选择状态，回到自动匹配模式。
   * @returns void 无返回值。
   */
  const resetCouponSelection = useCallback(() => {
    setCouponSelectionMode('AUTO')
    setSelectedCouponId(undefined)
  }, [])

  /**
   * 返回上一页。
   * @returns 无返回值。
   */
  const handleBack = () => {
    navigate(-1)
  }

  /**
   * 更新商品数量。
   * @param id 购物车项 ID。
   * @param quantity 目标数量。
   * @returns Promise<void> 无返回值。
   */
  const handleUpdateQuantity = async (id: number, quantity: number) => {
    const nextQuantity = Number(quantity)

    if (Number.isNaN(nextQuantity)) {
      return
    }

    if (nextQuantity < 1) {
      if (selectedItems.length <= 1) {
        showNotify('订单至少需要一个商品')
        return
      }

      if (isBuyNow) {
        setSelectedItems(prev => prev.filter(item => item.id !== id))
        resetCouponSelection()
        return
      }

      const removed = await removeItem(id)
      if (removed) {
        setSelectedItems(prev => prev.filter(item => item.id !== id))
        resetCouponSelection()
      }
      return
    }

    if (isBuyNow) {
      setSelectedItems(prev => prev.map(item => (item.id === id ? { ...item, quantity: nextQuantity } : item)))
      resetCouponSelection()
      return
    }

    const updated = await updateQuantity(id, nextQuantity)
    if (updated) {
      setSelectedItems(prev => prev.map(item => (item.id === id ? { ...item, quantity: nextQuantity } : item)))
      resetCouponSelection()
    }
  }

  /**
   * 选择收货地址。
   * @param address 用户选择的地址。
   * @returns 无返回值。
   */
  const handleSelectAddress = (address: UserAddressTypes.UserAddressVo) => {
    setSelectedAddress(address)
    resetCouponSelection()
    setShowAddressSelector(false)
  }

  /**
   * 格式化完整地址。
   * @param address 地址对象。
   * @returns string 完整地址字符串。
   */
  const formatFullAddress = (address: UserAddressTypes.UserAddressVo) => {
    const parts = [address.address, address.detailAddress].filter(Boolean)
    return parts.join(' ')
  }

  /**
   * 请求订单预览（金额与优惠券均由后端计算）。
   * @param options 当前优惠券请求配置。
   * @returns Promise<void> 无返回值。
   */
  const requestOrderPreview = useCallback(
    async (options?: { couponId?: string; forceDisableCoupon?: boolean }) => {
      if (!selectedAddress?.id || selectedItems.length === 0) {
        setPreviewData(null)
        return
      }

      const requestVersion = previewRequestVersionRef.current + 1
      previewRequestVersionRef.current = requestVersion
      setPreviewLoading(true)

      try {
        let previewResult: newOrderTypes.OrderPreviewVo
        const previewCouponId = options?.forceDisableCoupon ? undefined : options?.couponId
        const disableCoupon = options?.forceDisableCoupon === true ? true : undefined

        if (isBuyNow) {
          const buyNowItem = selectedItems[0]
          if (!buyNowItem) {
            showNotify('立即购买商品不存在')
            return
          }

          previewResult = await newOrderTypes.previewOrder({
            type: newOrderTypes.PreviewType.PRODUCT,
            productId: buyNowItem.productId.toString(),
            quantity: buyNowItem.quantity,
            addressId: selectedAddress.id,
            couponId: previewCouponId,
            disableCoupon
          })
        } else {
          previewResult = await newOrderTypes.previewOrder({
            type: newOrderTypes.PreviewType.CART,
            cartIds: selectedItems.map(item => item.id.toString()),
            addressId: selectedAddress.id,
            couponId: previewCouponId,
            disableCoupon
          })
        }

        if (requestVersion !== previewRequestVersionRef.current) {
          return
        }

        setPreviewData(previewResult)
      } catch (error: any) {
        if (requestVersion !== previewRequestVersionRef.current) {
          return
        }
        setPreviewData(null)
        console.error('订单预览失败:', error)
        showNotify(error?.message || '订单预览失败，请稍后重试')
      } finally {
        if (requestVersion === previewRequestVersionRef.current) {
          setPreviewLoading(false)
        }
      }
    },
    [isBuyNow, selectedAddress?.id, selectedItems]
  )

  /**
   * 在地址、商品数量、优惠券变化后拉取后端预览结果。
   * @returns 无返回值。
   */
  useEffect(() => {
    if (!selectedAddress?.id || selectedItems.length === 0) {
      setPreviewData(null)
      return
    }
    void requestOrderPreview({
      couponId: selectedCouponId,
      forceDisableCoupon: couponSelectionMode === 'NONE'
    })
  }, [couponSelectionMode, requestOrderPreview, selectedAddress?.id, selectedItems.length, selectedCouponId])

  /**
   * 选择优惠券。
   * @param couponId 目标优惠券 ID，不传代表不使用优惠券。
   * @returns 无返回值。
   */
  const handleSelectCoupon = (couponId?: string) => {
    if (couponId) {
      setCouponSelectionMode('MANUAL')
      setSelectedCouponId(couponId)
    } else {
      setCouponSelectionMode('NONE')
      setSelectedCouponId(undefined)
    }
    setShowCouponPopup(false)
  }

  /**
   * 提交订单。
   * @returns Promise<void> 无返回值。
   */
  const handleSubmit = async () => {
    if (!selectedAddress || !selectedAddress.id) {
      showNotify('请选择收货地址')
      return
    }

    if (previewLoading) {
      showNotify('订单金额正在计算中，请稍候')
      return
    }

    const deliveryType = previewData?.deliveryType as DeliveryType | undefined
    if (!deliveryType) {
      showNotify('配送方式获取失败，请刷新后重试')
      return
    }

    try {
      setSubmitting(true)

      let result

      if (isBuyNow) {
        const product = selectedItems[0]
        if (!product) {
          showNotify('立即购买商品不存在')
          return
        }

        result = await newOrderTypes.checkoutOrder({
          productId: product.productId.toString(),
          quantity: product.quantity,
          addressId: selectedAddress.id,
          deliveryType,
          remark: remark.trim() || undefined,
          couponId: checkoutCouponId,
          disableCoupon: couponDisabledExplicitly || undefined
        })
      } else {
        result = await newOrderTypes.createOrderFromCart({
          cartIds: selectedItems.map(item => item.id.toString()),
          addressId: selectedAddress.id,
          deliveryType,
          remark: remark.trim() || undefined,
          couponId: checkoutCouponId,
          disableCoupon: couponDisabledExplicitly || undefined
        })
        await fetchCartList()
        await fetchCartCount()
      }

      if (result && result.orderNo) {
        showSuccessNotify('订单提交成功')
        navigate(`/order/payment/${result.orderNo}`, {
          replace: true
        })
      }
    } catch (error: any) {
      console.error('提交订单失败:', error)
      showNotify(error.message || '提交订单失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className={styles.checkoutPage}>
        <div className={styles.checkoutHeader}>
          <div className={styles.headerLeft} onClick={handleBack}>
            <ChevronLeft size={24} />
          </div>
          <div className={styles.headerTitle}>确认订单</div>
        </div>
        {renderCheckoutPageSkeleton()}
      </div>
    )
  }

  return (
    <div className={styles.checkoutPage}>
      <div className={styles.checkoutHeader}>
        <div className={styles.headerLeft} onClick={handleBack}>
          <ChevronLeft size={24} />
        </div>
        <div className={styles.headerTitle}>确认订单</div>
      </div>

      <div className={styles.checkoutContent}>
        <div className={styles.addressCard} onClick={() => setShowAddressSelector(true)}>
          {selectedAddress ? (
            <div className={styles.addressContent}>
              <div className={styles.locationIcon}>
                <MapPin size={20} />
              </div>
              <div className={styles.addressInfo}>
                <div className={styles.userInfo}>
                  <span className={styles.userName}>{selectedAddress.receiverName}</span>
                  <span className={styles.userPhone}>{selectedAddress.receiverPhone}</span>
                </div>
                <div className={styles.userAddress}>{formatFullAddress(selectedAddress)}</div>
              </div>
              <ChevronRight size={18} className={styles.arrowIcon} />
            </div>
          ) : (
            <div className={styles.addressEmpty}>
              <div className={styles.emptyText}>
                <MapPin size={20} />
                <span>请选择收货地址</span>
              </div>
              <ChevronRight size={18} color='#ccc' />
            </div>
          )}
        </div>

        <div className={styles.goodsCard}>
          <div className={styles.cardHeader}>购物清单 ({totalQuantity})</div>
          <div className={styles.orderSnapshot}>
            <div className={styles.snapshotMain}>
              <div className={styles.snapshotTitle}>确认商品、优惠与配送信息</div>
              <div className={styles.snapshotDesc}>
                {previewLoading ? (
                  <SkeletonBlock inline className={styles.checkoutInlineSkeletonLong} />
                ) : (
                  deliveryHintText
                )}
              </div>
            </div>
            {displayedDiscountAmountValue > 0 && (
              <div className={styles.snapshotBadge}>已优惠 ¥{formatAmount(String(displayedDiscountAmountValue))}</div>
            )}
          </div>

          <div className={styles.goodsList}>
            {selectedItems.map(item => (
              <div key={item.id} className={styles.goodsItem}>
                <img src={item.image} alt={item.name} className={styles.goodsImage} />
                <div className={styles.goodsInfo}>
                  <div className={styles.goodsName}>{item.name}</div>
                  <div className={styles.goodsSpec}>{item.spec}</div>
                  <div className={styles.goodsBottom}>
                    <div className={styles.goodsPrice}>
                      <span className={styles.symbol}>¥</span>
                      <span className={styles.value}>{formatAmount(previewItemMap.get(item.productId)?.price)}</span>
                    </div>
                    <div className={styles.goodsQuantity}>
                      <InputNumber
                        value={item.quantity}
                        min={selectedItems.length <= 1 ? 1 : 0}
                        max={99}
                        onChange={value => handleUpdateQuantity(item.id, value as number)}
                      />
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className={styles.orderOptions}>
            <div className={`${styles.optionItem} ${styles.clickable}`} onClick={() => setShowCouponPopup(true)}>
              <div className={styles.optionLabelBlock}>
                <span className={styles.optionLabel}>优惠券</span>
                <span className={styles.optionHint}>{couponHintText}</span>
              </div>
              <div className={`${styles.optionContent} ${styles.highlight}`}>
                {previewLoading ? (
                  <SkeletonBlock inline className={styles.checkoutInlineSkeletonMedium} />
                ) : (
                  <span>{couponSummaryText}</span>
                )}
                <ChevronRight size={16} color='#ccc' />
              </div>
            </div>

            <div className={styles.optionItem}>
              <div className={styles.optionLabelBlock}>
                <span className={styles.optionLabel}>配送方式</span>
                <span className={styles.optionHint}>{deliveryHintText}</span>
              </div>
              <div className={`${styles.optionContent} ${styles.highlight}`}>
                {previewLoading ? (
                  <SkeletonBlock inline className={styles.checkoutInlineSkeletonShort} />
                ) : (
                  <span>{previewData?.deliveryTypeName || '请先选择收货地址'}</span>
                )}
              </div>
            </div>

            <div className={`${styles.optionItem} ${styles.remarkOption}`}>
              <div className={styles.optionLabelBlock}>
                <span className={styles.optionLabel}>订单备注</span>
                <span className={styles.optionHint}>如有特殊配送需求可在这里补充</span>
              </div>
              <div className={styles.remarkInputWrapper}>
                <TextArea
                  className={styles.remarkInput}
                  placeholder='选填，建议先与商家沟通确认'
                  value={remark}
                  onChange={setRemark}
                  maxLength={50}
                  rows={2}
                  autoSize
                  showCount
                />
              </div>
            </div>
          </div>
        </div>

        <div className={styles.priceCard}>
          <div className={styles.priceHeader}>
            <div className={styles.priceTitle}>金额明细</div>
            {displayedDiscountAmountValue > 0 && (
              <div className={styles.priceBadge}>共优惠 ¥{formatAmount(String(displayedDiscountAmountValue))}</div>
            )}
          </div>
          <div className={styles.priceItem}>
            <span className={styles.label}>商品总额</span>
            <span className={styles.value}>
              {previewLoading ? (
                <SkeletonBlock inline className={styles.checkoutPriceValueSkeleton} />
              ) : (
                `¥${formatAmount(previewData?.itemsAmount)}`
              )}
            </span>
          </div>
          <div className={styles.priceItem}>
            <span className={styles.label}>优惠金额</span>
            <span className={`${styles.value} ${styles.discount}`}>
              {previewLoading ? (
                <SkeletonBlock inline className={styles.checkoutPriceValueSkeletonShort} />
              ) : (
                `-¥${formatAmount(String(displayedDiscountAmountValue))}`
              )}
            </span>
          </div>
          <div className={`${styles.priceItem} ${styles.total}`}>
            <span className={styles.label}>实付款</span>
            <span className={styles.value}>
              {previewLoading ? (
                <SkeletonBlock inline className={styles.checkoutPriceTotalSkeleton} />
              ) : (
                `¥${displayedTotalAmountText}`
              )}
            </span>
          </div>
        </div>
      </div>

      <div className={styles.checkoutFooter}>
        <div className={styles.footerLeft}>
          <div className={styles.totalLabel}>合计:</div>
          <div className={styles.totalPrice}>
            {previewLoading ? (
              <SkeletonBlock inline className={styles.checkoutFooterTotalSkeleton} />
            ) : (
              <>
                <span className={styles.symbol}>¥</span>
                <span className={styles.value}>{displayedTotalAmountText}</span>
              </>
            )}
          </div>
        </div>
        <Button
          type='primary'
          fill='solid'
          className={styles.submitBtn}
          onClick={handleSubmit}
          loading={submitting}
          disabled={submitting || previewLoading || !previewData?.deliveryType}
        >
          {submitting ? '提交中...' : previewLoading ? '计算中' : '提交订单'}
        </Button>
      </div>

      <Popup
        visible={showCouponPopup}
        position='bottom'
        round
        style={{ height: COUPON_POPUP_HEIGHT }}
        onClose={() => setShowCouponPopup(false)}
      >
        <div className={styles.couponPopup}>
          <div className={styles.couponHeader}>
            <div className={styles.couponTitle}>选择优惠券</div>
            <div className={styles.couponClose} onClick={() => setShowCouponPopup(false)}>
              完成
            </div>
          </div>

          <div className={styles.couponBody}>
            <div
              className={`${styles.couponOptionCard} ${styles.noneSelected} ${noCouponSelected ? styles.selected : ''}`}
              onClick={() => handleSelectCoupon(undefined)}
            >
              <div className={styles.couponCardLeft}>
                <div className={styles.couponName}>不使用优惠券</div>
                <div className={styles.couponDesc}>保持当前金额</div>
              </div>
              <div className={styles.checkWrapper}>
                {noCouponSelected ? (
                  <CheckCircle2 size={20} className={styles.selectedIcon} />
                ) : (
                  <Circle size={20} className={styles.unselectedIcon} />
                )}
              </div>
            </div>

            {couponCandidates.length === 0 ? (
              <div className={styles.couponEmpty}>暂无可用优惠券</div>
            ) : (
              couponCandidates.map(coupon => {
                const couponId = coupon.couponId
                const matched = coupon.matched === true
                const selected = Boolean(couponId && couponId === effectiveSelectedCoupon?.couponId)
                const cardKey = `${coupon.couponId || ''}-${coupon.couponName || ''}-${coupon.expireTime || ''}`
                return (
                  <div
                    key={cardKey}
                    className={`${styles.couponOptionCard} ${matched ? styles.available : styles.disabled} ${
                      selected ? styles.selected : ''
                    }`}
                    onClick={() => {
                      if (matched && couponId) {
                        handleSelectCoupon(couponId)
                      }
                    }}
                  >
                    <div className={styles.cardLeft}>
                      <div className={styles.amountLine}>
                        <span className={styles.symbol}>¥</span>
                        <span className={styles.amount}>{formatAmount(coupon.availableAmount)}</span>
                      </div>
                      <div className={styles.amountHint}>可抵扣金额</div>
                    </div>

                    <div className={styles.cardRight}>
                      <div className={styles.cardHeader}>
                        <div className={styles.couponName}>{coupon.couponName || '优惠券'}</div>
                      </div>
                      <div className={styles.cardMeta}>
                        满 {formatAmount(coupon.thresholdAmount)} 元可用，预计抵扣{' '}
                        {formatAmount(coupon.couponDeductAmount)} 元
                      </div>
                      <div className={styles.timeLine}>
                        <Clock3 size={12} />
                        <span>有效期至 {coupon.expireTime || '--'}</span>
                      </div>
                      {!matched && (
                        <div className={styles.couponInvalidReason}>{coupon.unusableReason || '不可用'}</div>
                      )}
                    </div>

                    <div className={styles.checkWrapper}>
                      {selected ? (
                        <CheckCircle2 size={20} className={styles.selectedIcon} />
                      ) : (
                        <Circle size={20} className={styles.unselectedIcon} />
                      )}
                    </div>
                  </div>
                )
              })
            )}
          </div>
        </div>
      </Popup>

      <AddressSelector
        visible={showAddressSelector}
        onClose={() => setShowAddressSelector(false)}
        onSelect={handleSelectAddress}
      />
    </div>
  )
}

export default Checkout
