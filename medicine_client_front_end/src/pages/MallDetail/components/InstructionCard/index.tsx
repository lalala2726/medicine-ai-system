import React from 'react'
import { ChevronRight } from 'lucide-react'
import type { ProductTypes } from '@/api/product'
import styles from './index.module.less'

interface InstructionCardProps {
  medicineDetail: ProductTypes.DrugDetailDto | null | undefined
  images: string[]
  onViewDetail: () => void
}

const InstructionCard: React.FC<InstructionCardProps> = ({ medicineDetail, images, onViewDetail }) => {
  const displayFields = [
    { label: '药品通用名', value: medicineDetail?.commonName },
    { label: '品牌', value: medicineDetail?.brand },
    { label: '成分', value: medicineDetail?.composition },
    { label: '性状', value: medicineDetail?.characteristics }
  ].filter(field => field.value)

  return (
    <div className={styles.instructionCard}>
      <div className={styles.instructionHeader}>
        <h3 className={styles.instructionTitle}>说明书</h3>
        <div className={styles.instructionAction} onClick={onViewDetail}>
          <span>查看详情</span>
          <ChevronRight size={14} />
        </div>
      </div>

      <div className={styles.instructionContent}>
        {displayFields.slice(0, 2).map((field, index) => (
          <div key={index} className={styles.instructionRow}>
            <span className={styles.instructionLabel}>{field.label}</span>
            <span className={styles.instructionValue}>{field.value}</span>
          </div>
        ))}
      </div>

      {images.length > 0 && (
        <div className={styles.instructionImages}>
          {images.map((image, index) => (
            <div key={index} className={styles.instructionImageItem}>
              <img src={image} alt={`说明书图片${index + 1}`} />
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default InstructionCard
