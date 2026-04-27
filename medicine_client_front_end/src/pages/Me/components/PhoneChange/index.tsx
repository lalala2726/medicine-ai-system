import React, { useState } from 'react'
import { Button, Input } from '@nutui/nutui-react'
import { ChevronLeft, Smartphone } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { changePhone, sendPhoneVerificationCode } from '@/api/auth'
import type { CaptchaVerificationResult } from '@/api/captcha'
import { useUserStore } from '@/stores/userStore'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import SliderCaptchaModal from '@/pages/Login/components/SliderCaptchaModal'
import styles from './index.module.less'

/**
 * 手机号格式正则。
 */
const PHONE_NUMBER_PATTERN = /^1[3-9]\d{9}$/

/**
 * 手机号修改表单数据。
 */
interface PhoneChangeFormData {
  /** 新手机号 */
  phoneNumber: string
  /** 手机验证码 */
  verificationCode: string
}

/**
 * 客户端修改手机号页面。
 *
 * @returns 页面节点
 */
const PhoneChange: React.FC = () => {
  const navigate = useNavigate()
  const { user, profile, updateUser, updateFromProfile } = useUserStore()
  const [captchaOpen, setCaptchaOpen] = useState(false)
  const [sendingCode, setSendingCode] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [pendingPhoneNumber, setPendingPhoneNumber] = useState<string | null>(null)
  const [formData, setFormData] = useState<PhoneChangeFormData>({
    phoneNumber: '',
    verificationCode: ''
  })

  /**
   * 返回上一页。
   *
   * @returns 无返回值
   */
  const handleBack = (): void => {
    navigate(-1)
  }

  /**
   * 更新表单字段值。
   *
   * @param field 字段键
   * @param value 字段值
   * @returns 无返回值
   */
  const handleInputChange = (field: keyof PhoneChangeFormData, value: string): void => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }))
  }

  /**
   * 生成脱敏手机号文案。
   *
   * @param phoneNumber 完整手机号
   * @returns 脱敏后的手机号
   */
  const maskPhoneNumber = (phoneNumber: string): string => {
    return phoneNumber.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
  }

  /**
   * 校验发送验证码时的手机号。
   *
   * @returns 合法手机号，不合法时返回 null
   */
  const validatePhoneNumberForSend = (): string | null => {
    const normalizedPhoneNumber = formData.phoneNumber.trim()
    if (!normalizedPhoneNumber) {
      showNotify('请输入新手机号')
      return null
    }
    if (!PHONE_NUMBER_PATTERN.test(normalizedPhoneNumber)) {
      showNotify('请输入正确的手机号')
      return null
    }
    if (normalizedPhoneNumber === (profile.phoneNumber || '').trim()) {
      showNotify('新手机号不能与当前手机号相同')
      return null
    }
    return normalizedPhoneNumber
  }

  /**
   * 校验手机号修改表单。
   *
   * @returns 合法参数，不合法时返回 null
   */
  const validateSubmitParams = (): PhoneChangeFormData | null => {
    const normalizedPhoneNumber = validatePhoneNumberForSend()
    if (!normalizedPhoneNumber) {
      return null
    }
    const verificationCode = formData.verificationCode.trim()
    if (!verificationCode) {
      showNotify('请输入验证码')
      return null
    }
    return {
      phoneNumber: normalizedPhoneNumber,
      verificationCode
    }
  }

  /**
   * 打开发送验证码的滑动验证弹层。
   *
   * @returns 无返回值
   */
  const handleOpenCaptcha = (): void => {
    const normalizedPhoneNumber = validatePhoneNumberForSend()
    if (!normalizedPhoneNumber) {
      return
    }
    setPendingPhoneNumber(normalizedPhoneNumber)
    setCaptchaOpen(true)
  }

  /**
   * 取消发送验证码的滑动验证。
   *
   * @returns 无返回值
   */
  const handleCaptchaCancel = (): void => {
    setCaptchaOpen(false)
    setPendingPhoneNumber(null)
  }

  /**
   * 滑动验证通过后发送手机验证码。
   *
   * @param captchaVerificationResult 滑动验证码校验结果
   * @returns 无返回值
   */
  const handleCaptchaVerified = async (captchaVerificationResult: CaptchaVerificationResult): Promise<void> => {
    if (!pendingPhoneNumber) {
      setCaptchaOpen(false)
      return
    }

    try {
      setSendingCode(true)
      await sendPhoneVerificationCode({
        phoneNumber: pendingPhoneNumber,
        captchaVerificationId: captchaVerificationResult.id
      })
      showSuccessNotify('验证码已发送，请查看后端日志')
    } catch (error) {
      console.error('发送手机号验证码失败:', error)
    } finally {
      setSendingCode(false)
      setCaptchaOpen(false)
      setPendingPhoneNumber(null)
    }
  }

  /**
   * 提交修改手机号请求。
   *
   * @returns 无返回值
   */
  const handleSubmit = async (): Promise<void> => {
    const submitParams = validateSubmitParams()
    if (!submitParams) {
      return
    }

    try {
      setSubmitting(true)
      await changePhone(submitParams)
      updateUser({
        phone: maskPhoneNumber(submitParams.phoneNumber)
      })
      updateFromProfile({
        phoneNumber: submitParams.phoneNumber
      })
      showSuccessNotify('手机号修改成功')
      navigate(-1)
    } catch (error) {
      console.error('修改手机号失败:', error)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={styles.phoneChangePage}>
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={handleBack}>
          <ChevronLeft size={24} />
        </div>
        <div className={styles.navTitle}>修改手机号</div>
        <div className={styles.navRight} />
      </div>

      <div className={styles.pageContent}>
        <div className={styles.heroSection}>
          <div className={styles.heroTitle}>更新绑定手机号</div>
          <div className={styles.heroSubtitle}>发送验证码前需要先完成滑动验证，验证码会打印在后端日志中。</div>
        </div>

        <div className={styles.formCard}>
          <div className={styles.currentPhoneCard}>
            <div className={styles.currentPhoneLabel}>当前绑定手机号</div>
            <div className={styles.currentPhoneValue}>{user.phone || '未绑定手机号'}</div>
          </div>

          <div className={styles.fieldItem}>
            <div className={styles.fieldLabel}>新手机号</div>
            <div className={styles.control}>
              <div className={styles.controlIcon}>
                <Smartphone size={18} />
              </div>
              <Input
                className={styles.nutInput}
                type='tel'
                placeholder='请输入新的手机号'
                value={formData.phoneNumber}
                onChange={value => handleInputChange('phoneNumber', value)}
              />
            </div>
          </div>

          <div className={styles.fieldItem}>
            <div className={styles.fieldLabel}>验证码</div>
            <div className={styles.codeRow}>
              <div className={styles.codeInputWrapper}>
                <Input
                  className={styles.nutInput}
                  type='number'
                  placeholder='请输入验证码'
                  value={formData.verificationCode}
                  onChange={value => handleInputChange('verificationCode', value)}
                />
              </div>
              <button
                className={styles.sendCodeButton}
                onClick={handleOpenCaptcha}
                type='button'
                disabled={sendingCode}
              >
                {sendingCode ? '发送中...' : '发送验证码'}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className={styles.footer}>
        <Button block type='primary' className={styles.submitButton} loading={submitting} onClick={handleSubmit}>
          确认修改
        </Button>
      </div>

      <SliderCaptchaModal
        actionLabel='发送验证码'
        onCancel={handleCaptchaCancel}
        onVerified={handleCaptchaVerified}
        open={captchaOpen}
      />
    </div>
  )
}

export default PhoneChange
