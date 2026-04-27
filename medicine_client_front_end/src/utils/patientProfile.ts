/**
 * 根据出生日期计算就诊人年龄。
 *
 * @param birthDate - 出生日期，格式为 `YYYY-MM-DD`
 * @returns 计算后的年龄；无法解析时返回 0
 */
export const calculatePatientAge = (birthDate?: string) => {
  if (!birthDate) {
    return 0
  }

  const birth = new Date(birthDate)

  if (Number.isNaN(birth.getTime())) {
    return 0
  }

  const today = new Date()
  let age = today.getFullYear() - birth.getFullYear()
  const monthDiff = today.getMonth() - birth.getMonth()

  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age -= 1
  }

  return Math.max(age, 0)
}

/**
 * 将就诊人性别编码转换为展示文案。
 *
 * @param gender - 性别编码，`1` 表示男，`2` 表示女
 * @returns 对应的中文展示文案
 */
export const resolvePatientGenderText = (gender?: number) => {
  return gender === 1 ? '男' : gender === 2 ? '女' : '未知'
}
