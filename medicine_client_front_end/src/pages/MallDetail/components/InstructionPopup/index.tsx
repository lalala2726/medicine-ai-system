import React from 'react'
import { Popup, Ellipsis, Divider } from '@nutui/nutui-react'
import type { ProductTypes } from '@/api/product'
import { getDrugCategoryMeta } from '@/constants/drugCategory'
import styles from './index.module.less'

interface InstructionPopupProps {
  visible: boolean
  medicineDetail: ProductTypes.DrugDetailDto | null | undefined
  onClose: () => void
}

interface InstructionField {
  label: string
  value: string | boolean | undefined
  isLongText?: boolean
  rows?: number
}

const InstructionPopup: React.FC<InstructionPopupProps> = ({ visible, medicineDetail, onClose }) => {
  const drugCategoryMeta =
    medicineDetail?.drugCategory !== undefined ? getDrugCategoryMeta(medicineDetail.drugCategory) : undefined

  const instructionFields: InstructionField[] = [
    { label: '药品通用名', value: medicineDetail?.commonName },
    { label: '品牌', value: medicineDetail?.brand },
    { label: '成分', value: medicineDetail?.composition, isLongText: true, rows: 2 },
    { label: '性状', value: medicineDetail?.characteristics },
    { label: '功能主治', value: medicineDetail?.efficacy, isLongText: true, rows: 3 },
    { label: '用法用量', value: medicineDetail?.usageMethod, isLongText: true, rows: 2 },
    { label: '不良反应', value: medicineDetail?.adverseReactions, isLongText: true, rows: 2 },
    { label: '注意事项', value: medicineDetail?.precautions, isLongText: true, rows: 3 },
    { label: '禁忌', value: medicineDetail?.taboo, isLongText: true, rows: 2 },
    { label: '包装规格', value: medicineDetail?.packaging },
    { label: '有效期', value: medicineDetail?.validityPeriod },
    { label: '贮藏条件', value: medicineDetail?.storageConditions },
    { label: '产地类型', value: medicineDetail?.originType },
    { label: '批准文号', value: medicineDetail?.approvalNumber },
    { label: '生产单位', value: medicineDetail?.productionUnit },
    { label: '执行标准', value: medicineDetail?.executiveStandard },
    {
      label: '药品分类',
      value: drugCategoryMeta ? `${drugCategoryMeta.shortLabel} / ${drugCategoryMeta.name}` : undefined
    },
    {
      label: '是否外用药',
      value:
        medicineDetail?.isOutpatientMedicine !== undefined
          ? medicineDetail.isOutpatientMedicine
            ? '是'
            : '否'
          : undefined
    },
    { label: '药品说明书', value: medicineDetail?.instruction, isLongText: true, rows: 5 }
  ]

  const validFields = instructionFields.filter(field => {
    return !(field.value === undefined || field.value === null || field.value === '')
  })

  const renderField = (field: InstructionField, index: number) => {
    const isLastField = index === validFields.length - 1
    const valueStr = String(field.value)

    return (
      <React.Fragment key={index}>
        <div className={styles.popupRow}>
          <span className={styles.popupLabel}>{field.label}</span>
          {field.isLongText ? (
            <div className={styles.popupValue}>
              <Ellipsis
                content={valueStr}
                direction='end'
                rows={field.rows || 2}
                expandText='展开'
                collapseText='收起'
              />
            </div>
          ) : (
            <span className={styles.popupValue}>{valueStr}</span>
          )}
        </div>
        {!isLastField && <Divider />}
      </React.Fragment>
    )
  }

  return (
    <Popup
      visible={visible}
      position='bottom'
      round
      closeable
      destroyOnClose
      title='药品说明书'
      style={{ height: '80vh' }}
      onClose={onClose}
    >
      <div className={styles.instructionPopup}>
        <div className={styles.popupContent}>
          {medicineDetail && validFields.length > 0 ? (
            <>
              {validFields.map((field, index) => renderField(field, index))}
              {medicineDetail.warmTips && (
                <>
                  <Divider />
                  <div className={`${styles.popupRow} ${styles.popupRowHighlight}`}>
                    <span className={styles.popupLabel}>温馨提示</span>
                    <div className={styles.popupValue}>
                      <Ellipsis
                        content={medicineDetail.warmTips}
                        direction='end'
                        rows={3}
                        expandText='展开'
                        collapseText='收起'
                      />
                    </div>
                  </div>
                </>
              )}
            </>
          ) : (
            <div className={styles.emptyTip}>暂无说明书信息</div>
          )}
        </div>
      </div>
    </Popup>
  )
}

export default InstructionPopup
