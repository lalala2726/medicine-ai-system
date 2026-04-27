import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Dialog, Skeleton } from '@nutui/nutui-react'
import { ChevronLeft, Edit3, Trash2, Plus } from 'lucide-react'
import {
  listPatientProfiles,
  deletePatientProfile,
  setDefaultPatientProfile,
  type patientProfileTypes
} from '@/api/patientProfile'
import type { PatientWithAge } from '@/types/patient'
import { showSuccessNotify, showErrorNotify } from '@/utils/notify'
import PatientForm from '@/components/PatientForm'
import styles from './index.module.less'
import Empty from '@/components/Empty'

// 辅助函数：从出生日期计算年龄
const calculateAge = (birthDate?: string): number => {
  if (!birthDate) return 0

  const birth = new Date(birthDate)
  const today = new Date()

  let age = today.getFullYear() - birth.getFullYear()
  const monthDiff = today.getMonth() - birth.getMonth()

  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age--
  }

  return age
}

// 转换后端数据为前端显示格式
const transformPatientData = (patient: patientProfileTypes.PatientProfileListVo): PatientWithAge => {
  return {
    ...patient,
    age: calculateAge(patient.birthDate),
    isDefault: patient.isDefault === 1
  }
}

const PatientList: React.FC = () => {
  const navigate = useNavigate()
  const [patients, setPatients] = useState<PatientWithAge[]>([])
  const [loading, setLoading] = useState(false)

  const [showForm, setShowForm] = useState(false)
  const [editingPatient, setEditingPatient] = useState<PatientWithAge | null>(null)

  // 获取就诊人列表
  const fetchPatients = useCallback(async () => {
    setLoading(true)
    try {
      const response = await listPatientProfiles()
      const transformedPatients = response.map(transformPatientData)
      setPatients(transformedPatients)
    } catch (error) {
      console.error('获取就诊人列表失败:', error)
      showErrorNotify('获取就诊人列表失败')
    } finally {
      setLoading(false)
    }
  }, [])

  // 页面加载时获取数据
  useEffect(() => {
    fetchPatients()
  }, [fetchPatients])

  const handleBack = () => {
    navigate(-1)
  }

  const handleAdd = () => {
    setEditingPatient(null)
    setShowForm(true)
  }

  const handleEdit = (patient: PatientWithAge) => {
    setEditingPatient(patient)
    setShowForm(true)
  }

  const handleDelete = (id: string, name: string, e: React.MouseEvent) => {
    e.stopPropagation()
    Dialog.confirm({
      title: '删除就诊人',
      content: `确定要删除就诊人"${name}"吗？`,
      confirmText: '确定',
      cancelText: '取消',
      onConfirm: async () => {
        try {
          await deletePatientProfile(id)
          showSuccessNotify('删除成功')
          fetchPatients()
        } catch (error) {
          console.error('删除就诊人失败:', error)
          showErrorNotify('删除失败')
        }
      }
    })
  }

  const handleSetDefault = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation()
    try {
      await setDefaultPatientProfile(id)
      showSuccessNotify('设置成功')
      fetchPatients()
    } catch (error) {
      console.error('设置默认就诊人失败:', error)
      showErrorNotify('设置失败')
    }
  }

  return (
    <div className={styles.patientListPage}>
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={handleBack}>
          <ChevronLeft size={24} />
        </div>
        <div className={styles.navTitle}>就诊人管理</div>
        <div className={styles.navRight} />
      </div>

      <div className={styles.content}>
        {loading && patients.length === 0 ? (
          <div className={styles.loadingContainer}>
            <div className={styles.skeleton}>
              <Skeleton rows={3} animated />
            </div>
            <div className={styles.skeleton}>
              <Skeleton rows={3} animated />
            </div>
          </div>
        ) : patients.length === 0 ? (
          <div className={styles.emptyContainer}>
            <Empty description='暂无就诊人信息' />
            <Button
              type='primary'
              className={styles.addBtn}
              onClick={handleAdd}
              style={{ marginTop: 24, width: 180, position: 'static' }}
            >
              添加就诊人
            </Button>
          </div>
        ) : (
          patients.map(patient => (
            <div
              key={patient.id}
              className={`${styles.patientCard} ${patient.isDefault ? styles.active : ''}`}
              onClick={() => handleEdit(patient)}
            >
              <div className={styles.cardHeader}>
                <div className={styles.headerLeft}>
                  <span className={styles.name}>{patient.name}</span>
                  <span className={styles.relationTag}>{patient.relationship || '亲友'}</span>
                  {patient.isDefault && <span className={styles.defaultTag}>默认</span>}
                </div>
                <div className={styles.headerRight}>
                  {!patient.isDefault && (
                    <div className={styles.setDefaultBtn} onClick={e => handleSetDefault(patient.id!, e)}>
                      设为默认
                    </div>
                  )}
                </div>
              </div>

              <div className={styles.cardBody}>
                <div className={styles.infoRow}>
                  <span className={styles.infoValue}>{patient.gender === 1 ? '男' : '女'}</span>
                  <span className={styles.divider}>|</span>
                  <span className={styles.infoValue}>{patient.age} 岁</span>
                </div>
                {patient.pastMedicalHistory && patient.pastMedicalHistory !== '无' && (
                  <div className={styles.infoRow}>
                    <span className={styles.infoLabel}>既往病史：</span>
                    <span className={styles.infoValue}>{patient.pastMedicalHistory}</span>
                  </div>
                )}
              </div>

              <div className={styles.cardFooter}>
                <div
                  className={styles.actionBtn}
                  onClick={e => {
                    e.stopPropagation()
                    handleEdit(patient)
                  }}
                >
                  <Edit3 size={16} />
                  <span>编辑</span>
                </div>
                <div className={styles.actionBtn} onClick={e => handleDelete(patient.id!, patient.name!, e)}>
                  <Trash2 size={16} />
                  <span>删除</span>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {patients.length > 0 && (
        <div className={styles.footer}>
          <Button block type='primary' className={styles.addBtn} onClick={handleAdd}>
            <Plus size={20} style={{ marginRight: 8 }} />
            添加就诊人
          </Button>
        </div>
      )}

      <PatientForm
        visible={showForm}
        editData={editingPatient}
        onClose={() => {
          setShowForm(false)
          setEditingPatient(null)
        }}
        onSuccess={() => {
          fetchPatients()
        }}
      />
    </div>
  )
}

export default PatientList
