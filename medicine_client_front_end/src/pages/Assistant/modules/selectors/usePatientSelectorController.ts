import { useCallback, useEffect, useState } from 'react'
import { listPatientProfiles, type patientProfileTypes } from '@/api/patientProfile'

/** PatientSelector 业务控制器配置。 */
interface UsePatientSelectorControllerOptions {
  visible: boolean
}

/** PatientSelector 组件消费的控制器结果。 */
export interface PatientSelectorControllerResult {
  loading: boolean
  patients: patientProfileTypes.PatientProfileListVo[]
  formVisible: boolean
  refreshPatients: () => Promise<void>
  openCreateForm: () => void
  closeCreateForm: () => void
}

/**
 * 管理就诊人选择器的数据加载与新增表单状态。
 *
 * @param options - 控制器配置项
 * @returns 供组件使用的就诊人选择器状态与事件
 */
export function usePatientSelectorController({
  visible
}: UsePatientSelectorControllerOptions): PatientSelectorControllerResult {
  /** 当前是否正在加载就诊人列表。 */
  const [loading, setLoading] = useState(false)
  /** 当前已加载的就诊人列表。 */
  const [patients, setPatients] = useState<patientProfileTypes.PatientProfileListVo[]>([])
  /** 当前新增就诊人表单是否展示。 */
  const [formVisible, setFormVisible] = useState(false)

  /**
   * 拉取最新就诊人列表。
   *
   * @returns 无返回值
   */
  const refreshPatients = useCallback(async () => {
    setLoading(true)

    try {
      const response = await listPatientProfiles()
      setPatients(response)
    } catch (error) {
      console.error('加载就诊人列表失败:', error)
      setPatients([])
    } finally {
      setLoading(false)
    }
  }, [])

  /**
   * 打开新增就诊人表单。
   *
   * @returns 无返回值
   */
  const openCreateForm = useCallback(() => {
    setFormVisible(true)
  }, [])

  /**
   * 关闭新增就诊人表单。
   *
   * @returns 无返回值
   */
  const closeCreateForm = useCallback(() => {
    setFormVisible(false)
  }, [])

  /**
   * 选择器弹层打开时拉取最新数据。
   *
   * @returns 无返回值
   */
  useEffect(() => {
    if (!visible) {
      return
    }

    void refreshPatients()
  }, [refreshPatients, visible])

  return {
    loading,
    patients,
    formVisible,
    refreshPatients,
    openCreateForm,
    closeCreateForm
  }
}
