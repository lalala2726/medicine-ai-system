import requestClient from '@/request/requestClient'

export namespace patientProfileTypes {
  export interface PatientProfileListVo {
    /** 主键ID */
    id?: string
    /** 就诊人姓名 */
    name?: string
    /** 性别：1男 2女 */
    gender?: number
    /** 出生日期 */
    birthDate?: string
    /** 过敏史 */
    allergy?: string
    /** 既往病史 */
    pastMedicalHistory?: string
    /** 慢性病信息 */
    chronicDisease?: string
    /** 长期用药 */
    longTermMedications?: string
    /** 与账户关系 */
    relationship?: string
    /** 是否默认就诊人：1是 0否 */
    isDefault?: number
  }

  export interface PatientProfileAddRequest {
    /** 就诊人姓名 */
    name: string
    /** 性别 */
    gender: number
    /** 出生日期 */
    birthDate: string
    /** 过敏史 */
    allergy?: string
    /** 既往病史 */
    pastMedicalHistory?: string
    /** 慢性病信息 */
    chronicDisease?: string
    /** 长期用药 */
    longTermMedications?: string
    /** 与账户关系 */
    relationship?: string
    /** 是否默认就诊人 */
    isDefault?: number
  }

  export interface PatientProfileUpdateRequest {
    /** ID */
    id: string
    /** 就诊人姓名 */
    name: string
    /** 性别 */
    gender: number
    /** 出生日期 */
    birthDate: string
    /** 过敏史 */
    allergy?: string
    /** 既往病史 */
    pastMedicalHistory?: string
    /** 慢性病信息 */
    chronicDisease?: string
    /** 长期用药 */
    longTermMedications?: string
    /** 与账户关系 */
    relationship?: string
    /** 是否默认就诊人 */
    isDefault?: number
  }

  export interface PatientProfileVo {
    /** 主键ID */
    id?: string
    /** 所属用户ID */
    userId?: string
    /** 就诊人姓名 */
    name?: string
    /** 性别：1男 2女 */
    gender?: number
    /** 出生日期 */
    birthDate?: string
    /** 过敏史 */
    allergy?: string
    /** 既往病史 */
    pastMedicalHistory?: string
    /** 慢性病信息 */
    chronicDisease?: string
    /** 长期用药 */
    longTermMedications?: string
    /** 与账户关系 */
    relationship?: string
    /** 是否默认就诊人：1是 0否 */
    isDefault?: number
  }
}

/**
 * 查询就诊人信息列表
 */
export const listPatientProfiles = () => {
  return requestClient.get<patientProfileTypes.PatientProfileListVo[]>('/patient_profile/list')
}

/**
 * 查询就诊人详情
 * @param id 就诊人ID
 */
export const getPatientProfile = (id: string) => {
  return requestClient.get<patientProfileTypes.PatientProfileVo>(`/patient_profile/${id}`)
}

/**
 * 新增就诊人信息
 * @param data 就诊人信息
 */
export const addPatientProfile = (data: patientProfileTypes.PatientProfileAddRequest) => {
  return requestClient.post<void>('/patient_profile', data)
}

/**
 * 修改就诊人信息
 * @param data 就诊人信息
 */
export const updatePatientProfile = (data: patientProfileTypes.PatientProfileUpdateRequest) => {
  return requestClient.put<void>('/patient_profile', data)
}

/**
 * 删除就诊人信息
 * @param id 就诊人ID
 */
export const deletePatientProfile = (id: string) => {
  return requestClient.delete<void>(`/patient_profile/${id}`)
}

/**
 * 设置默认就诊人
 * @param id 就诊人ID
 */
export const setDefaultPatientProfile = (id: string) => {
  return requestClient.put<void>(`/patient_profile/default/${id}`)
}
