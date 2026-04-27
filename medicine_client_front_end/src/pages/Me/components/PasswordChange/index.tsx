import React, { useState } from 'react'
import { Button, Input } from '@nutui/nutui-react'
import { ChevronLeft, Eye, EyeOff, Lock } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { changePassword } from '@/api/auth'
import type { CaptchaVerificationResult } from '@/api/captcha'
import type { ChangePasswordParams } from '@/types/api'
import { useAuthStore } from '@/store/auth'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import SliderCaptchaModal from '@/pages/Login/components/SliderCaptchaModal'
import styles from './index.module.less'

/**
 * 记住账号的本地缓存 key。
 */
const REMEMBERED_ACCOUNT_KEY = 'rememberedAccount'

/**
 * 记住密码的本地缓存 key。
 */
const REMEMBERED_PASSWORD_KEY = 'rememberedPassword'

/**
 * 登录方式的本地缓存 key。
 */
const LOGIN_TYPE_KEY = 'loginType'

/**
 * 密码字段键。
 */
type PasswordFieldKey = 'oldPassword' | 'newPassword' | 'confirmPassword'

/**
 * 修改密码表单数据。
 */
interface PasswordChangeFormData {
  /** 原密码 */
  oldPassword: string
  /** 新密码 */
  newPassword: string
  /** 确认新密码 */
  confirmPassword: string
}

/**
 * 密码明文显示状态。
 */
type PasswordVisibleState = Record<PasswordFieldKey, boolean>

/**
 * 客户端修改密码页面。
 *
 * @returns 页面节点
 */
const PasswordChange: React.FC = () => {
  const navigate = useNavigate()
  const clearTokens = useAuthStore(state => state.clearTokens)
  const [submitting, setSubmitting] = useState(false)
  const [captchaOpen, setCaptchaOpen] = useState(false)
  const [pendingChangePasswordParams, setPendingChangePasswordParams] = useState<ChangePasswordParams | null>(null)
  const [formData, setFormData] = useState<PasswordChangeFormData>({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
  const [passwordVisibleState, setPasswordVisibleState] = useState<PasswordVisibleState>({
    oldPassword: false,
    newPassword: false,
    confirmPassword: false
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
  const handleInputChange = (field: PasswordFieldKey, value: string): void => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }))
  }

  /**
   * 切换密码显隐状态。
   *
   * @param field 字段键
   * @returns 无返回值
   */
  const handleTogglePasswordVisible = (field: PasswordFieldKey): void => {
    setPasswordVisibleState(prev => ({
      ...prev,
      [field]: !prev[field]
    }))
  }

  /**
   * 清理记住登录信息的本地缓存。
   *
   * @returns 无返回值
   */
  const clearRememberedLoginState = (): void => {
    localStorage.removeItem(REMEMBERED_ACCOUNT_KEY)
    localStorage.removeItem(REMEMBERED_PASSWORD_KEY)
    localStorage.removeItem(LOGIN_TYPE_KEY)
  }

  /**
   * 校验表单并生成待提交参数。
   *
   * @returns 合法的修改密码参数，不合法时返回 null
   */
  const validateAndBuildParams = (): ChangePasswordParams | null => {
    const oldPassword = formData.oldPassword.trim()
    const newPassword = formData.newPassword.trim()
    const confirmPassword = formData.confirmPassword.trim()

    if (!oldPassword) {
      showNotify('请输入原密码')
      return null
    }
    if (!newPassword) {
      showNotify('请输入新密码')
      return null
    }
    if (!confirmPassword) {
      showNotify('请再次输入新密码')
      return null
    }
    if (oldPassword === newPassword) {
      showNotify('新密码不能与原密码相同')
      return null
    }
    if (newPassword !== confirmPassword) {
      showNotify('两次输入的新密码不一致')
      return null
    }

    return {
      oldPassword,
      newPassword,
      captchaVerificationId: ''
    }
  }

  /**
   * 点击确认修改后打开验证码弹层。
   *
   * @returns 无返回值
   */
  const handleSubmit = (): void => {
    const changePasswordParams = validateAndBuildParams()
    if (!changePasswordParams) {
      return
    }
    setPendingChangePasswordParams(changePasswordParams)
    setCaptchaOpen(true)
  }

  /**
   * 取消验证码校验。
   *
   * @returns 无返回值
   */
  const handleCaptchaCancel = (): void => {
    setCaptchaOpen(false)
    setPendingChangePasswordParams(null)
  }

  /**
   * 验证码通过后执行密码修改。
   *
   * @param captchaVerificationResult 验证码校验结果
   * @returns 无返回值
   */
  const handleCaptchaVerified = async (captchaVerificationResult: CaptchaVerificationResult): Promise<void> => {
    if (!pendingChangePasswordParams) {
      setCaptchaOpen(false)
      return
    }

    try {
      setSubmitting(true)
      await changePassword({
        ...pendingChangePasswordParams,
        captchaVerificationId: captchaVerificationResult.id
      })
      showSuccessNotify('密码修改成功，请重新登录')
      clearTokens()
      clearRememberedLoginState()
      navigate('/login', { replace: true })
    } catch (error) {
      console.error('修改密码失败:', error)
    } finally {
      setSubmitting(false)
      setCaptchaOpen(false)
      setPendingChangePasswordParams(null)
    }
  }

  return (
    <div className={styles.passwordChangePage}>
      <div className={styles.navbar}>
        <div className={styles.navLeft} onClick={handleBack}>
          <ChevronLeft size={24} />
        </div>
        <div className={styles.navTitle}>修改密码</div>
        <div className={styles.navRight} />
      </div>

      <div className={styles.pageContent}>
        <div className={styles.heroSection}>
          <div className={styles.heroTitle}>保护您的账号安全</div>
          <div className={styles.heroSubtitle}>请输入当前密码并设置新的登录密码，提交前需完成滑动验证。</div>
        </div>

        <div className={styles.formCard}>
          <div className={styles.fieldItem}>
            <div className={styles.fieldLabel}>原密码</div>
            <div className={styles.control}>
              <div className={styles.controlIcon}>
                <Lock size={18} />
              </div>
              <Input
                className={styles.nutInput}
                type={passwordVisibleState.oldPassword ? 'text' : 'password'}
                placeholder='请输入原密码'
                value={formData.oldPassword}
                onChange={value => handleInputChange('oldPassword', value)}
              />
              <div className={styles.eyeButton} onClick={() => handleTogglePasswordVisible('oldPassword')}>
                {passwordVisibleState.oldPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </div>
            </div>
          </div>

          <div className={styles.fieldItem}>
            <div className={styles.fieldLabel}>新密码</div>
            <div className={styles.control}>
              <div className={styles.controlIcon}>
                <Lock size={18} />
              </div>
              <Input
                className={styles.nutInput}
                type={passwordVisibleState.newPassword ? 'text' : 'password'}
                placeholder='请输入新密码'
                value={formData.newPassword}
                onChange={value => handleInputChange('newPassword', value)}
              />
              <div className={styles.eyeButton} onClick={() => handleTogglePasswordVisible('newPassword')}>
                {passwordVisibleState.newPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </div>
            </div>
          </div>

          <div className={styles.fieldItem}>
            <div className={styles.fieldLabel}>确认新密码</div>
            <div className={styles.control}>
              <div className={styles.controlIcon}>
                <Lock size={18} />
              </div>
              <Input
                className={styles.nutInput}
                type={passwordVisibleState.confirmPassword ? 'text' : 'password'}
                placeholder='请再次输入新密码'
                value={formData.confirmPassword}
                onChange={value => handleInputChange('confirmPassword', value)}
              />
              <div className={styles.eyeButton} onClick={() => handleTogglePasswordVisible('confirmPassword')}>
                {passwordVisibleState.confirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </div>
            </div>
          </div>
        </div>

        <div className={styles.tipCard}>
          <div className={styles.tipTitle}>修改说明</div>
          <div className={styles.tipItem}>请输入当前账号正在使用的密码完成身份确认。</div>
          <div className={styles.tipItem}>两次输入的新密码必须保持一致，且不能与原密码相同。</div>
          <div className={styles.tipItem}>修改成功后当前设备将退出登录，请使用新密码重新登录。</div>
        </div>
      </div>

      <div className={styles.footer}>
        <Button block type='primary' className={styles.submitButton} loading={submitting} onClick={handleSubmit}>
          确认修改
        </Button>
      </div>

      <SliderCaptchaModal
        actionLabel='修改密码'
        onCancel={handleCaptchaCancel}
        onVerified={handleCaptchaVerified}
        open={captchaOpen}
      />
    </div>
  )
}

export default PasswordChange
