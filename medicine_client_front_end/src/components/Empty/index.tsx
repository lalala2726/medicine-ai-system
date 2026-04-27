import React from 'react'
import { Inbox } from 'lucide-react'
import styles from './index.module.less'

export interface EmptyProps {
  image?: React.ReactNode | string
  imageSize?: number | string
  description?: React.ReactNode
  children?: React.ReactNode
  className?: string
  style?: React.CSSProperties
}

const Empty: React.FC<EmptyProps> = ({
  image,
  imageSize = 80,
  description = '暂无数据',
  children,
  className = '',
  style
}) => {
  const renderImage = () => {
    if (!image) {
      return (
        <div className={styles.defaultImage} style={{ width: imageSize, height: imageSize }}>
          <Inbox size={Number(imageSize) * 0.5} strokeWidth={1.5} />
        </div>
      )
    }

    if (typeof image === 'string') {
      return <img src={image} alt='empty' style={{ width: imageSize, height: imageSize }} />
    }

    return image
  }

  return (
    <div className={`${styles.empty} ${className}`} style={style}>
      <div className={styles.imageContainer}>{renderImage()}</div>
      {description && <div className={styles.description}>{description}</div>}
      {children && <div className={styles.footer}>{children}</div>}
    </div>
  )
}

export default Empty
