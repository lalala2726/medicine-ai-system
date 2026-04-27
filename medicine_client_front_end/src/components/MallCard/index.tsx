import React from 'react'
import './index.less'

interface MallCardProps {
  src?: string
  title?: string
  sale?: string
  price?: string
  imageHeight?: number
  onClick?: () => void
  onAddCart?: (e: React.MouseEvent) => void
}

const MallCard: React.FC<MallCardProps> = ({
  src = 'https://bkimg.cdn.bcebos.com/pic/b3b7d0a20cf431adcbef4a1d566fbbaf2edda3cc32c4?x-bce-process=image/format,f_auto/quality,Q_70/resize,m_lfit,limit_1,w_536',
  title = '999感冒灵',
  sale = '销售 0 件',
  price = '9.9',
  imageHeight,
  onClick
}) => {
  // 处理价格显示，移除可能存在的￥符号
  const displayPrice = price?.replace(/^￥|^¥/, '') || '0.00'

  return (
    <div className='mall-card' onClick={onClick}>
      <div className='mall-card__image' style={imageHeight ? { height: `${imageHeight}px` } : undefined}>
        <img src={src} alt={title} loading='lazy' />
      </div>
      <div className='mall-card__content'>
        <h3 className='mall-card__title'>{title}</h3>
        <p className='mall-card__sale'>{sale}</p>
        <div className='mall-card__footer'>
          <div className='mall-card__price'>
            <span className='mall-card__price-symbol'>¥</span>
            <span className='mall-card__price-value'>{displayPrice}</span>
          </div>
        </div>
      </div>
    </div>
  )
}

export default MallCard
