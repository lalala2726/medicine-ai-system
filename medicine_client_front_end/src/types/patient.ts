import type { patientProfileTypes } from '@/api/patientProfile'

// 扩展 PatientProfileListVo 以添加计算属性
export interface PatientWithAge extends Omit<patientProfileTypes.PatientProfileListVo, 'isDefault'> {
  age?: number
  isDefault: boolean
}
