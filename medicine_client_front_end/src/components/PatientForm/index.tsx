import React, { useEffect, useState } from 'react'
import { Popup, Input, Button, Radio, Switch, TextArea, DatePicker, Picker } from '@nutui/nutui-react'
import { ChevronRight } from 'lucide-react'
import { addPatientProfile, updatePatientProfile, type patientProfileTypes } from '@/api/patientProfile'
import type { PatientWithAge } from '@/types/patient'
import { showSuccessNotify, showErrorNotify } from '@/utils/notify'
import styles from './index.module.less'

interface PatientFormProps {
  visible: boolean
  editData?: PatientWithAge | null
  onClose: () => void
  onSuccess: () => void
}

// 关系选项
const RELATIONSHIP_OPTIONS = [
  { label: '本人', value: '本人' },
  { label: '伴侣', value: '伴侣' },
  { label: '父母', value: '父母' },
  { label: '子女', value: '子女' },
  { label: '朋友', value: '朋友' },
  { label: '其他', value: '其他' }
]

// Initial state
const initialFormState = {
  name: '',
  gender: '1',
  birthDate: '',
  relationship: '',
  pastMedicalHistory: '',
  allergy: '',
  chronicDisease: '',
  longTermMedications: '',
  isDefault: false
}

const PatientForm: React.FC<PatientFormProps> = ({ visible, editData, onClose, onSuccess }) => {
  const [submitting, setSubmitting] = useState(false)
  const [showDatePicker, setShowDatePicker] = useState(false)
  const [showRelationPicker, setShowRelationPicker] = useState(false)

  const [formData, setFormData] = useState(initialFormState)

  // Initialize form data when visible or editData changes
  useEffect(() => {
    if (visible) {
      if (editData) {
        setFormData({
          name: editData.name || '',
          gender: (editData.gender || 1).toString(),
          birthDate: editData.birthDate || '',
          relationship: editData.relationship || '',
          pastMedicalHistory: editData.pastMedicalHistory || '',
          allergy: editData.allergy || '',
          chronicDisease: editData.chronicDisease || '',
          longTermMedications: editData.longTermMedications || '',
          isDefault: Boolean(editData.isDefault)
        })
      } else {
        setFormData(initialFormState)
      }
    }
  }, [visible, editData])

  const handleConfirmDate = (_selectedOptions: any, selectedValue: any[]) => {
    if (selectedValue && selectedValue.length === 3) {
      const [year, month, day] = selectedValue
      const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
      setFormData(prev => ({ ...prev, birthDate: dateStr }))
    }
    setShowDatePicker(false)
  }

  const handleConfirmRelation = (_selectedOptions: any, selectedValue: any[]) => {
    if (selectedValue && selectedValue.length > 0) {
      setFormData(prev => ({ ...prev, relationship: selectedValue[0] as string }))
    }
    setShowRelationPicker(false)
  }

  const handleInputChange = (key: string, value: any) => {
    setFormData(prev => ({ ...prev, [key]: value }))
  }

  const validateForm = () => {
    if (!formData.name.trim()) {
      showErrorNotify('请输入姓名')
      return false
    }
    if (!formData.birthDate) {
      showErrorNotify('请选择出生日期')
      return false
    }
    if (!formData.relationship.trim()) {
      showErrorNotify('请选择与本人关系')
      return false
    }
    return true
  }

  const handleSubmit = async () => {
    if (!validateForm()) return

    setSubmitting(true)
    try {
      /** 新增就诊人时提交给后端的请求参数。 */
      const commonData: patientProfileTypes.PatientProfileAddRequest = {
        name: formData.name.trim(),
        gender: Number(formData.gender),
        birthDate: formData.birthDate,
        allergy: formData.allergy || '',
        pastMedicalHistory: formData.pastMedicalHistory || '',
        chronicDisease: formData.chronicDisease || '',
        longTermMedications: formData.longTermMedications || '',
        relationship: formData.relationship || '',
        isDefault: formData.isDefault ? 1 : 0
      }

      if (editData?.id) {
        /** 编辑就诊人时提交给后端的请求参数。 */
        const updateRequest: patientProfileTypes.PatientProfileUpdateRequest = {
          id: editData.id,
          ...commonData
        }
        await updatePatientProfile(updateRequest)
        showSuccessNotify('修改成功')
      } else {
        await addPatientProfile(commonData)
        showSuccessNotify('添加成功')
      }

      onSuccess()
      onClose()
    } catch (apiError) {
      console.error('提交失败:', apiError)
      showErrorNotify('提交失败，请重试')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Popup visible={visible} position='bottom' round onClose={onClose} style={{ height: '70vh' }}>
      <div className={styles.patientFormPopup}>
        {/* Navbar */}
        <div className={styles.navbar}>
          <div className={styles.navTitle}>{editData ? '编辑就诊人' : '添加就诊人'}</div>
        </div>

        {/* Content */}
        <div className={styles.contentWrapper}>
          {/* Basic Info Section */}
          <div className={styles.sectionTitle}>基本信息</div>
          <div className={styles.card}>
            {/* Name */}
            <div className={styles.formItem}>
              <div className={styles.label}>姓名</div>
              <div className={styles.inputWrapper}>
                <Input
                  className={styles.customInput}
                  value={formData.name}
                  onChange={val => handleInputChange('name', val)}
                  placeholder='请输入真实姓名'
                  align='right'
                />
              </div>
            </div>

            {/* Gender */}
            <div className={styles.formItem}>
              <div className={styles.label}>性别</div>
              <div className={styles.inputWrapper}>
                <Radio.Group
                  value={formData.gender}
                  onChange={val => handleInputChange('gender', val)}
                  direction='horizontal'
                  className={styles.radioGroup}
                >
                  <Radio value='1'>男</Radio>
                  <Radio value='2'>女</Radio>
                </Radio.Group>
              </div>
            </div>

            {/* Birthday */}
            <div className={styles.formItem} onClick={() => setShowDatePicker(true)}>
              <div className={styles.label}>出生日期</div>
              <div className={styles.inputWrapper}>
                {formData.birthDate ? (
                  <span className={styles.valueText}>{formData.birthDate}</span>
                ) : (
                  <span className={styles.placeholder}>请选择</span>
                )}
                <ChevronRight size={16} color='#ccc' style={{ marginLeft: 4 }} />
              </div>
            </div>

            {/* Relationship */}
            <div className={styles.formItem} onClick={() => setShowRelationPicker(true)}>
              <div className={styles.label}>与本人关系</div>
              <div className={styles.inputWrapper}>
                {formData.relationship ? (
                  <span className={styles.valueText}>{formData.relationship}</span>
                ) : (
                  <span className={styles.placeholder}>请选择</span>
                )}
                <ChevronRight size={16} color='#ccc' style={{ marginLeft: 4 }} />
              </div>
            </div>
          </div>

          {/* Health Info Section */}
          <div className={styles.sectionTitle}>健康档案 (选填)</div>
          <div className={styles.card}>
            {/* Past History */}
            <div className={`${styles.formItem} ${styles.verticalItem}`}>
              <div className={styles.label}>既往病史</div>
              <div className={styles.inputWrapper}>
                <TextArea
                  className={styles.customTextArea}
                  value={formData.pastMedicalHistory}
                  onChange={val => handleInputChange('pastMedicalHistory', val)}
                  placeholder="请输入既往病史，无则填'无'"
                  rows={2}
                  maxLength={100}
                />
              </div>
            </div>

            {/* Allergy */}
            <div className={`${styles.formItem} ${styles.verticalItem}`}>
              <div className={styles.label}>过敏史</div>
              <div className={styles.inputWrapper}>
                <TextArea
                  className={styles.customTextArea}
                  value={formData.allergy}
                  onChange={val => handleInputChange('allergy', val)}
                  placeholder="请输入过敏史，无则填'无'"
                  rows={2}
                  maxLength={100}
                />
              </div>
            </div>

            {/* Chronic */}
            <div className={`${styles.formItem} ${styles.verticalItem}`}>
              <div className={styles.label}>慢性病</div>
              <div className={styles.inputWrapper}>
                <TextArea
                  className={styles.customTextArea}
                  value={formData.chronicDisease}
                  onChange={val => handleInputChange('chronicDisease', val)}
                  placeholder="请输入慢性病，无则填'无'"
                  rows={2}
                  maxLength={100}
                />
              </div>
            </div>
          </div>

          {/* Settings Section */}
          <div className={styles.card}>
            <div className={styles.formItem}>
              <div className={styles.label}>设为默认</div>
              <div className={styles.inputWrapper}>
                <Switch checked={formData.isDefault} onChange={val => handleInputChange('isDefault', val)} />
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className={styles.footer}>
          <Button block type='primary' className={styles.submitBtn} loading={submitting} onClick={handleSubmit}>
            保存就诊人
          </Button>
        </div>

        {/* Date Picker */}
        <DatePicker
          visible={showDatePicker}
          title='选择出生日期'
          type='date'
          startDate={new Date(1900, 0, 1)}
          endDate={new Date()}
          value={formData.birthDate ? new Date(formData.birthDate) : new Date()}
          onConfirm={handleConfirmDate}
          onClose={() => setShowDatePicker(false)}
        />

        {/* Relation Picker */}
        <Picker
          visible={showRelationPicker}
          title='选择与本人关系'
          options={[RELATIONSHIP_OPTIONS]}
          value={[formData.relationship]}
          onConfirm={handleConfirmRelation}
          onClose={() => setShowRelationPicker(false)}
        />
      </div>
    </Popup>
  )
}

export default PatientForm
