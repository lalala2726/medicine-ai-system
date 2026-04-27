import React from 'react'
import { Swipe, Checkbox, InputNumber } from '@nutui/nutui-react'
import { Trash2 } from 'lucide-react'
import type { CartItem } from '@/stores/cartStore'
import styles from './index.module.less'

interface CartItemCardProps {
  item: CartItem
  editMode?: boolean
  onToggleSelect: (_id: number) => void
  onUpdateQuantity: (_id: number, _quantity: number) => void
  onDelete: (_item: CartItem) => void
  onProductClick: (_id: number) => void
  onDeleteWithConfirm?: (_item: CartItem) => void
}

const CartItemCard: React.FC<CartItemCardProps> = ({
  item,
  editMode = false,
  onToggleSelect,
  onUpdateQuantity,
  onDelete,
  onProductClick,
  onDeleteWithConfirm
}) => {
  // 滑动右侧操作按钮 - 只显示删除
  const rightAction = (
    <div className={styles.deleteButton} onClick={() => onDelete(item)}>
      <div className={styles.deleteIcon}>
        <Trash2 size={20} color='#fff' />
      </div>
      <div className={styles.deleteText}>删除</div>
    </div>
  )

  return (
    <div className={styles.cartItemWrapper}>
      <Swipe rightAction={rightAction}>
        <div className={styles.cartItem}>
          <div className={styles.itemCheckbox}>
            <Checkbox checked={item.selected} onChange={() => onToggleSelect(item.id)} />
          </div>

          <div className={styles.itemImage} onClick={() => onProductClick(item.productId)}>
            <img src={item.image} alt={item.name} />
          </div>

          <div className={styles.itemInfo}>
            <div className={styles.itemName} onClick={() => onProductClick(item.productId)}>
              {item.name}
            </div>
            <div className={styles.itemSpec}>{item.spec}</div>
            <div className={styles.itemBottom}>
              <div className={styles.itemPrice}>
                <span className={styles.priceSymbol}>¥</span>
                <span className={styles.priceValue}>{item.price.toFixed(2)}</span>
              </div>
              {!editMode && (
                <div>
                  <InputNumber
                    value={item.quantity}
                    min={1}
                    max={99}
                    onChange={value => onUpdateQuantity(item.id, value as number)}
                  />
                </div>
              )}
            </div>
          </div>

          {editMode && onDeleteWithConfirm && (
            <div className={styles.itemDelete} onClick={() => onDeleteWithConfirm(item)}>
              <Trash2 size={20} />
            </div>
          )}
        </div>
      </Swipe>
    </div>
  )
}

export default CartItemCard
