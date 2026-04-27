import React from 'react'
import { Popup, Button, Loading } from '@nutui/nutui-react'
import { Close } from '@nutui/icons-react'
import type { patientProfileTypes } from '@/api/patientProfile'
import Empty from '@/components/Empty'
import PatientForm from '@/components/PatientForm'
import { calculatePatientAge, resolvePatientGenderText } from '@/utils/patientProfile'
import { usePatientSelectorController } from '../../modules/selectors/usePatientSelectorController'
import styles from './index.module.less'

/** PatientSelector 组件属性。 */
interface PatientSelectorProps {
  /** 是否显示。 */
  visible: boolean
  /** 关闭回调。 */
  onClose: () => void
  /** 选中就诊人回调。 */
  onSelect: (patient: patientProfileTypes.PatientProfileListVo) => void
}

/**
 * 判断当前就诊人是否满足发送所需的关键字段。
 *
 * @param patient - 当前就诊人信息
 * @returns 是否允许发送该就诊人卡
 */
const canSendPatientCard = (patient: patientProfileTypes.PatientProfileListVo) => {
  return Boolean(
    patient.id && patient.name?.trim() && patient.birthDate?.trim() && patient.relationship?.trim() && patient.gender
  )
}

/**
 * 就诊人选择弹层。
 *
 * @param props - 组件属性
 * @returns 就诊人选择器节点
 */
const PatientSelector: React.FC<PatientSelectorProps> = ({ visible, onClose, onSelect }) => {
  const { loading, patients, formVisible, refreshPatients, openCreateForm, closeCreateForm } =
    usePatientSelectorController({
      visible
    })

  return (
    <>
      <Popup visible={visible} position='bottom' round onClose={onClose} style={{ height: '80vh' }}>
        <div className={styles.container} data-assistant-drawer-swipe-lock='true'>
          <div className={styles.header}>
            <span className={styles.title}>选择就诊人</span>
            <Close className={styles.close} onClick={onClose} />
          </div>

          <div className={styles.list}>
            {patients.map(patient => {
              /** 当前就诊人年龄。 */
              const age = calculatePatientAge(patient.birthDate)
              /** 当前就诊人性别展示文案。 */
              const genderText = resolvePatientGenderText(patient.gender)

              return (
                <div key={patient.id} className={styles.card}>
                  <div className={styles.cardBody}>
                    <div className={styles.patientInfo}>
                      <div className={styles.patientHeader}>
                        <span className={styles.name}>{patient.name}</span>
                        <span className={styles.gender}>{genderText}</span>
                      </div>
                      <div className={styles.tagRow}>
                        <span className={styles.relationship}>{patient.relationship || '亲友'}</span>
                        {patient.isDefault === 1 ? <span className={styles.defaultTag}>默认</span> : null}
                      </div>
                    </div>
                    <div className={styles.action}>
                      <Button
                        type='primary'
                        size='small'
                        className={styles.sendBtn}
                        style={{ color: '#fff' }}
                        disabled={!canSendPatientCard(patient)}
                        onClick={e => {
                          e.stopPropagation()
                          onSelect(patient)
                        }}
                      >
                        发送
                      </Button>
                    </div>
                  </div>

                  <div className={styles.cardFooter}>
                    <div className={styles.metaItem}>
                      <span className={styles.label}>年龄:</span>
                      <span className={styles.value}>{age > 0 ? `${age}岁` : '未知'}</span>
                    </div>
                    <div className={styles.metaItem}>
                      <span className={styles.label}>出生日期:</span>
                      <span className={styles.value}>{patient.birthDate || '未填写'}</span>
                    </div>
                  </div>
                </div>
              )
            })}

            {loading ? (
              <div className={styles.loading}>
                <Loading />
              </div>
            ) : null}

            {!loading && patients.length === 0 ? (
              <div className={styles.empty}>
                <Empty description='暂无就诊人信息' />
                <Button type='primary' className={styles.emptyAction} onClick={openCreateForm}>
                  新增就诊人
                </Button>
              </div>
            ) : null}
          </div>

          {patients.length > 0 ? (
            <div className={styles.footer}>
              <Button block type='primary' className={styles.addBtn} onClick={openCreateForm}>
                新增就诊人
              </Button>
            </div>
          ) : null}
        </div>
      </Popup>

      <PatientForm
        visible={formVisible}
        onClose={closeCreateForm}
        onSuccess={() => {
          void refreshPatients()
        }}
      />
    </>
  )
}

export default PatientSelector
