import { useCallback, useState } from 'react'

/** Assistant 页面里所有弹层选择器的可见性与初始筛选状态。 */
export interface AssistantSelectorState {
  orderSelectorVisible: boolean
  orderSelectorInitialStatus?: string
  afterSaleSelectorVisible: boolean
  afterSaleSelectorInitialStatus?: string
  patientSelectorVisible: boolean
  openOrderSelector: (status?: string) => void
  closeOrderSelector: () => void
  openAfterSaleSelector: (status?: string) => void
  closeAfterSaleSelector: () => void
  openPatientSelector: () => void
  closePatientSelector: () => void
}

/**
 * 管理 Assistant 页面中订单/售后/就诊人选择器的显示状态。
 * 页面 controller 只依赖这个 hook，不直接维护分散的 useState。
 */
export function useAssistantSelectorState(): AssistantSelectorState {
  /** 订单选择器是否显示。 */
  const [orderSelectorVisible, setOrderSelectorVisible] = useState(false)
  /** 订单选择器打开时的初始状态筛选。 */
  const [orderSelectorInitialStatus, setOrderSelectorInitialStatus] = useState<string | undefined>(undefined)
  /** 售后选择器是否显示。 */
  const [afterSaleSelectorVisible, setAfterSaleSelectorVisible] = useState(false)
  /** 售后选择器打开时的初始状态筛选。 */
  const [afterSaleSelectorInitialStatus, setAfterSaleSelectorInitialStatus] = useState<string | undefined>(undefined)
  /** 就诊人选择器是否显示。 */
  const [patientSelectorVisible, setPatientSelectorVisible] = useState(false)

  /** 打开订单选择器，并可附带初始状态筛选。 */
  const openOrderSelector = useCallback((status?: string) => {
    setOrderSelectorInitialStatus(status)
    setOrderSelectorVisible(true)
  }, [])

  /** 关闭订单选择器。 */
  const closeOrderSelector = useCallback(() => {
    setOrderSelectorVisible(false)
  }, [])

  /** 打开售后选择器，并可附带初始状态筛选。 */
  const openAfterSaleSelector = useCallback((status?: string) => {
    setAfterSaleSelectorInitialStatus(status)
    setAfterSaleSelectorVisible(true)
  }, [])

  /** 关闭售后选择器。 */
  const closeAfterSaleSelector = useCallback(() => {
    setAfterSaleSelectorVisible(false)
  }, [])

  /** 打开就诊人选择器。 */
  const openPatientSelector = useCallback(() => {
    setPatientSelectorVisible(true)
  }, [])

  /** 关闭就诊人选择器。 */
  const closePatientSelector = useCallback(() => {
    setPatientSelectorVisible(false)
  }, [])

  return {
    orderSelectorVisible,
    orderSelectorInitialStatus,
    afterSaleSelectorVisible,
    afterSaleSelectorInitialStatus,
    patientSelectorVisible,
    openOrderSelector,
    closeOrderSelector,
    openAfterSaleSelector,
    closeAfterSaleSelector,
    openPatientSelector,
    closePatientSelector
  }
}
