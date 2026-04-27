import React from 'react'
import { ShoppingBag } from 'lucide-react'
import { AssistantBubble, AssistantCardShell } from '../MessagePrimitives'
import type { ProductDisplayItem } from '../../modules/messages/chatTypes'
import styles from './index.module.less'

export type { ProductDisplayItem }

/** 商品推荐卡标签文案。 */
const PRODUCT_TAG_TEXT = '商品推荐'

/** 商品卡布局语义。 */
type ProductCardTone = 'assistant' | 'user'

interface ProductCardProps {
  /** @deprecated 不再展示标题，保留字段避免外部调用报错。 */
  title?: string
  /** 商品列表 */
  products: ProductDisplayItem[]
  /** 卡片布局语义。 */
  tone?: ProductCardTone
  /** 点击商品回调 */
  onProductClick?: (product: ProductDisplayItem) => void
}

/**
 * 商品推荐卡组件。
 *
 * @param props - 组件属性
 * @returns 商品推荐卡节点
 */
const ProductCard: React.FC<ProductCardProps> = ({ products = [], tone = 'assistant', onProductClick }) => {
  if (products.length === 0) {
    return null
  }

  /**
   * 触发商品点击回调。
   *
   * @param product - 当前点击的商品信息
   * @returns 无返回值
   */
  const handleProductClick = (product: ProductDisplayItem) => {
    onProductClick?.(product)
  }

  const cardClassName = [styles.productCard, tone === 'user' ? styles.userProductCard : styles.assistantProductCard]
    .filter(Boolean)
    .join(' ')

  const cardContent = (
    <>
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <ShoppingBag size={14} className={styles.headerIcon} />
          <span className={styles.tagText}>{PRODUCT_TAG_TEXT}</span>
        </div>
        <span className={styles.headerCount}>共{products.length}件</span>
      </div>

      <div className={styles.productList}>
        {products.map(product => (
          <button
            key={product.id}
            type='button'
            className={styles.productItem}
            onClick={() => handleProductClick(product)}
            aria-label={`查看商品${product.name}`}
          >
            <img src={product.image} alt={product.name} className={styles.productImage} />
            <span className={styles.productInfo}>
              <span className={styles.productName}>{product.name}</span>
              {product.tags && product.tags.length > 0 && (
                <span className={styles.productTags}>
                  {product.tags.map((tag, index) => (
                    <span key={index} className={styles.productTag}>
                      {tag}
                    </span>
                  ))}
                </span>
              )}
              <span className={styles.productPrice}>
                <span className={styles.priceSymbol}>¥</span>
                <span className={styles.priceValue}>{product.price}</span>
              </span>
            </span>
          </button>
        ))}
      </div>
    </>
  )

  if (tone === 'user') {
    return (
      <AssistantBubble tone='user' className={styles.userBubble}>
        <div className={cardClassName}>{cardContent}</div>
      </AssistantBubble>
    )
  }

  return (
    <AssistantCardShell className={styles.assistantCardShell}>
      <div className={cardClassName}>{cardContent}</div>
    </AssistantCardShell>
  )
}

export default ProductCard
