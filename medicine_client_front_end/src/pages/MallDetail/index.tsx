import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Swiper, Skeleton, Button, Badge, ImagePreview } from '@nutui/nutui-react'
import { ChevronLeft, ShoppingCart, MessageSquare } from 'lucide-react'
import { detail } from '@/api/product'
import type { ProductTypes } from '@/api/product'
import { useCartStore } from '@/stores/cartStore'
import { useAuth } from '@/hooks/useAuth'
import type { AssistantConsultRouteState } from '@/pages/Assistant/modules/shared/consultProductDraft'
import InstructionCard from './components/InstructionCard'
import InstructionPopup from './components/InstructionPopup'
import { showNotify } from '@/utils/notify'
import { getDrugCategoryMeta } from '@/constants/drugCategory'
import styles from './index.module.less'

const MallDetail: React.FC = () => {
  const navigate = useNavigate()
  const params = useParams<{ id: string }>()
  const { addItem, cartCount, fetchCartCount } = useCartStore()
  const { isLoggedIn, requireLogin } = useAuth()

  const [loading, setLoading] = useState(true)
  const [product, setProduct] = useState<ProductTypes.MallProductVo | null>(null)
  const [currentImageIndex, setCurrentImageIndex] = useState(0)
  const [showInstructionPopup, setShowInstructionPopup] = useState(false)
  const [showImagePreview, setShowImagePreview] = useState(false)

  // 获取商品详情
  useEffect(() => {
    const fetchProductDetail = async () => {
      if (!params.id) {
        showNotify('商品ID不存在')
        navigate(-1)
        return
      }

      try {
        setLoading(true)
        const data = await detail(params.id)
        setProduct(data)
      } catch (error) {
        console.error('获取商品详情失败:', error)
        showNotify('获取商品详情失败')
      } finally {
        setLoading(false)
      }
    }

    fetchProductDetail()
  }, [params.id, navigate])

  // 每次进入商品详情页时获取最新的购物车数量（仅在登录状态下）
  useEffect(() => {
    if (isLoggedIn) {
      fetchCartCount()
    }
  }, [isLoggedIn, fetchCartCount])

  const handleBack = () => {
    navigate(-1)
  }

  const handleAddToCart = async () => {
    if (!product) return
    if (!requireLogin()) return

    await addItem({ productId: Number(product.id) || 0 })
  }

  const handleBuyNow = () => {
    if (!product) return
    if (!requireLogin()) return

    const buyNowData = {
      isBuyNow: true,
      product: {
        id: Number(product.id) || 0,
        productId: Number(product.id) || 0,
        name: product.name || '未知商品',
        price: Number(product.price) || 0,
        image: product.images?.[0] || '/images/1.jpg',
        spec: product.unit || '件',
        quantity: 1,
        stock: product.stock
      }
    }

    navigate('/checkout', { state: buyNowData })
  }

  /**
   * 将当前商品作为咨询草稿带入 Assistant 页面。
   *
   * @returns 无返回值
   */
  const handleConsult = () => {
    if (!product) return
    if (!requireLogin()) return

    const consultRouteState: AssistantConsultRouteState = {
      consultProductDraft: {
        productId: Number(product.id) || 0,
        name: product.name || '',
        image: images[0] || '',
        price: product.price || '0',
        unit: product.unit || '件'
      }
    }

    navigate('/assistant', { state: consultRouteState })
  }

  const handleImageChange = (index: number) => {
    setCurrentImageIndex(index)
  }

  const handleImageClick = () => {
    setShowImagePreview(true)
  }

  // 加载状态
  if (loading) {
    return (
      <div className={styles.mallDetail}>
        <div className={styles.mallDetail__header}>
          <div className={styles.headerBack} onClick={handleBack}>
            <ChevronLeft size={24} />
          </div>
          <div className={styles.headerTitle}>商品详情</div>
          <div className={styles.headerActions}></div>
        </div>
        <div className={styles.mallDetail__loading}>
          <Skeleton rows={10} animated />
        </div>
      </div>
    )
  }

  // 无数据
  if (!product) {
    return (
      <div className={styles.mallDetail}>
        <div className={styles.mallDetail__header}>
          <div className={styles.headerBack} onClick={handleBack}>
            <ChevronLeft size={24} />
          </div>
          <div className={styles.headerTitle}>商品详情</div>
          <div className={styles.headerActions}></div>
        </div>
        <div className={styles.mallDetail__empty}>
          <p>该商品已下架或不存在</p>
        </div>
      </div>
    )
  }

  const images = product.images && product.images.length > 0 ? product.images : ['/images/1.jpg']
  const drugDetail = product.drugDetail
  const drugCategoryMeta = getDrugCategoryMeta(drugDetail?.drugCategory)
  const drugTagClassName =
    drugDetail?.drugCategory === 0
      ? styles.otcGreen
      : drugDetail?.drugCategory === 1
        ? styles.rx
        : drugDetail?.drugCategory === 2
          ? styles.otcRed
          : ''

  return (
    <div className={styles.mallDetail}>
      {/* 顶部导航栏 */}
      <div className={styles.mallDetail__header}>
        <div className={styles.headerBack} onClick={handleBack}>
          <ChevronLeft size={24} />
        </div>
        <div className={styles.headerTitle}>商品详情</div>
        <div className={styles.headerActions}></div>
      </div>

      {/* 轮播图 */}
      <div className={styles.mallDetail__swiper}>
        <Swiper autoPlay loop indicator onChange={handleImageChange}>
          {images.map((item, index) => (
            <Swiper.Item key={index}>
              <img src={item} alt={`product-${index}`} draggable={false} onClick={handleImageClick} />
            </Swiper.Item>
          ))}
        </Swiper>
        <div className={styles.swiperIndicator}>
          {currentImageIndex + 1} / {images.length}
        </div>
      </div>

      {/* 图片预览 */}
      <ImagePreview
        visible={showImagePreview}
        images={images.map(src => ({ src }))}
        defaultValue={currentImageIndex}
        onClose={() => setShowImagePreview(false)}
      />

      {/* 商品信息 */}
      <div className={styles.mallDetail__content}>
        <div className={styles.productInfo}>
          <div className={styles.productHeader}>
            <div className={styles.productPrice}>
              <span className={styles.priceSymbol}>¥</span>
              <span className={styles.priceValue}>{product.price || '0'}</span>
              <span className={styles.priceUnit}>/{product.unit || '件'}</span>
            </div>
            <div className={styles.productSales}>
              <span>已售 {product.sales || '0'}</span>
            </div>
          </div>
          <div className={styles.productName}>{product.name || '未知商品'}</div>

          <div className={styles.productTagWrapper}>
            {drugCategoryMeta && (
              <>
                <span className={`${styles.productTag} ${drugTagClassName}`}>{drugCategoryMeta.shortLabel}</span>
                <span className={styles.productSubtitle}>{drugCategoryMeta.description}</span>
              </>
            )}
          </div>

          {product.categoryNames && product.categoryNames.length > 0 && (
            <div className={styles.productTagWrapper}>
              {product.categoryNames.map(categoryName => (
                <span key={categoryName} className={styles.productTag}>
                  {categoryName}
                </span>
              ))}
            </div>
          )}

          {product.stock !== undefined && (
            <div className={styles.productStock}>
              库存状态
              <span className={product.stock > 0 ? styles.inStock : styles.outStock}>
                {product.stock > 0 ? `充足 (${product.stock})` : '缺货'}
              </span>
            </div>
          )}
        </div>
      </div>

      {/* 说明书卡片 */}
      {drugDetail && (
        <InstructionCard medicineDetail={drugDetail} images={[]} onViewDetail={() => setShowInstructionPopup(true)} />
      )}

      {/* 商品详情图片 */}
      <div className={styles.detailImagesCard}>
        <div className={styles.cardTitle}>商品详情</div>
        <div className={styles.imageContent}>
          {images.map((item, index) => (
            <img key={index} src={item} alt={`detail-${index}`} />
          ))}
        </div>
      </div>

      {/* 说明书详情弹窗 */}
      <InstructionPopup
        visible={showInstructionPopup}
        medicineDetail={drugDetail}
        onClose={() => setShowInstructionPopup(false)}
      />

      {/* 底部操作栏 */}
      <div className={styles.mallDetail__footer}>
        <div className={styles.footerLeft}>
          <div className={styles.footerIcon} onClick={() => navigate('/cart')}>
            {cartCount > 0 ? (
              <Badge value={cartCount} max={99}>
                <ShoppingCart size={22} />
              </Badge>
            ) : (
              <ShoppingCart size={22} />
            )}
            <span>购物车</span>
          </div>
          <div className={styles.footerIcon} onClick={handleConsult}>
            <MessageSquare size={22} />
            <span>咨询</span>
          </div>
        </div>
        <div className={styles.footerRight}>
          <Button className={styles.btnAddCart} onClick={handleAddToCart} disabled={product.stock === 0}>
            加入购物车
          </Button>
          <Button className={styles.btnBuyNow} onClick={handleBuyNow} disabled={product.stock === 0}>
            立即购买
          </Button>
        </div>
      </div>
    </div>
  )
}

export default MallDetail
