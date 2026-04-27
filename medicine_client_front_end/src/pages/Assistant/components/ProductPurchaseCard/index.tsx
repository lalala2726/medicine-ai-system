import React, { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Package, ChevronRight } from 'lucide-react'
import { AssistantCardShell } from '../MessagePrimitives'
import type { PurchaseProductDisplayItem } from '../../modules/messages/chatTypes'
import styles from './index.module.less'

/** 商品购买卡默认标题。 */
const DEFAULT_PRODUCT_PURCHASE_CARD_TITLE = '请确认要购买的商品'
/** 商品购买卡头部说明文案。 */
const PRODUCT_PURCHASE_CARD_HEADER_HINT = '请核对商品、数量与金额信息。'

/** 商品购买卡固定按钮文案。 */
const FIXED_PRODUCT_PURCHASE_BUTTON_TEXT = '立即购买'

interface ProductPurchaseCardProps {
  /** 卡片标题。 */
  title?: string
  /** 购买商品列表。 */
  products: PurchaseProductDisplayItem[]
  /** 后端计算后的总价展示文案。 */
  totalPrice: string
  /** "立即购买"按钮点击回调。 */
  onPurchase?: (products: PurchaseProductDisplayItem[]) => void
}

/**
 * 商品购买确认卡片。
 * 只负责展示后端下发的商品、数量和总价，不参与购买流程跳转。
 * 点击单个商品可跳转至商品详情页。
 */
const ProductPurchaseCard: React.FC<ProductPurchaseCardProps> = ({
  title = DEFAULT_PRODUCT_PURCHASE_CARD_TITLE,
  products,
  totalPrice,
  onPurchase
}) => {
  const navigate = useNavigate()

  const handleProductClick = useCallback(
    (productId: string) => {
      navigate(`/product/${productId}`)
    },
    [navigate]
  )

  if (products.length === 0 || !totalPrice.trim()) {
    return null
  }

  return (
    <AssistantCardShell className={styles.assistantProductPurchaseCard}>
      <div className={styles.cardHeader}>
        <div className={styles.headerLeft}>
          <Package size={16} className={styles.headerIcon} />
          <div className={styles.headerMain}>
            <span className={styles.headerTitle}>{title}</span>
            <span className={styles.headerHint}>{PRODUCT_PURCHASE_CARD_HEADER_HINT}</span>
          </div>
        </div>
      </div>

      <div className={styles.content}>
        <div className={styles.productList}>
          {products.map(product => (
            <div
              key={product.id}
              className={styles.productItem}
              onClick={() => handleProductClick(product.id)}
              role='button'
              tabIndex={0}
            >
              <div className={styles.productImage}>
                <img src={product.image} alt={product.name} />
              </div>

              <div className={styles.productInfo}>
                <div className={styles.productName}>{product.name}</div>
                <div className={styles.productMeta}>
                  <div className={styles.productPrice}>
                    <span className={styles.priceSymbol}>¥</span>
                    <span className={styles.priceValue}>{product.price}</span>
                  </div>
                  <div className={styles.productQuantity}>x{product.quantity}</div>
                </div>
              </div>

              <div className={styles.productArrow}>
                <ChevronRight size={14} />
              </div>
            </div>
          ))}
        </div>

        <div className={styles.footerSection}>
          <div className={styles.summarySection}>
            <span className={styles.totalLabel}>共 {products.reduce((sum, p) => sum + p.quantity, 0)} 件，合计</span>
            <div className={styles.totalPrice}>
              <span className={styles.priceSymbol}>¥</span>
              <span className={styles.totalPriceValue}>{totalPrice}</span>
            </div>
          </div>

          <div className={styles.actionSection}>
            <button type='button' className={styles.purchaseButton} onClick={() => onPurchase?.(products)}>
              {FIXED_PRODUCT_PURCHASE_BUTTON_TEXT}
            </button>
          </div>
        </div>
      </div>
    </AssistantCardShell>
  )
}

export default ProductPurchaseCard
