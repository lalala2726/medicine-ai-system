/**
 * 极简现代登录页面
 */

import React, { useState, useEffect } from 'react'
import { Input, Button, Checkbox } from '@nutui/nutui-react'
import { User, Smartphone, Eye, EyeOff } from 'lucide-react'
import styles from './index.module.less'
import { login } from '@/api/auth.ts'
import type { CaptchaVerificationResult } from '@/api/captcha'
import { useAuthStore } from '@/store/auth.ts'
import { useNavigate } from 'react-router-dom'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import type { LoginParams } from '@/types/api'
import SliderCaptchaModal from './components/SliderCaptchaModal'

/**
 * 登录页滚动锁定样式快照。
 */
interface LoginScrollLockSnapshot {
  /** html 原始 overflow */
  htmlOverflow: string
  /** body 原始 overflow */
  bodyOverflow: string
  /** body 原始 overscroll-behavior */
  bodyOverscrollBehavior: string
}

/**
 * 锁定登录页滚动，确保页面在登录阶段保持固定。
 *
 * @returns 解锁滚动函数
 */
function lockLoginPageScroll(): () => void {
  const htmlElement = document.documentElement
  const bodyElement = document.body
  const snapshot: LoginScrollLockSnapshot = {
    htmlOverflow: htmlElement.style.overflow,
    bodyOverflow: bodyElement.style.overflow,
    bodyOverscrollBehavior: bodyElement.style.overscrollBehavior
  }
  htmlElement.style.overflow = 'hidden'
  bodyElement.style.overflow = 'hidden'
  bodyElement.style.overscrollBehavior = 'none'
  return () => {
    htmlElement.style.overflow = snapshot.htmlOverflow
    bodyElement.style.overflow = snapshot.bodyOverflow
    bodyElement.style.overscrollBehavior = snapshot.bodyOverscrollBehavior
  }
}

const Login: React.FC = () => {
  const [showPassword, setShowPassword] = useState(false)
  const navigate = useNavigate()
  const [agreeTerms, setAgreeTerms] = useState(true)
  const [isAccountLogin, setIsAccountLogin] = useState(true)
  const [loading, setLoading] = useState(false)
  const [captchaOpen, setCaptchaOpen] = useState(false)
  const [pendingLoginParams, setPendingLoginParams] = useState<LoginParams | null>(null)

  const [formData, setFormData] = useState({
    username: '',
    phone: '',
    password: ''
  })

  const setTokens = useAuthStore(state => state.setTokens)
  const accessToken = useAuthStore(state => state.accessToken)

  useEffect(() => {
    if (accessToken) {
      navigate('/', { replace: true })
    }
  }, [accessToken, navigate])

  useEffect(() => {
    return lockLoginPageScroll()
  }, [])

  /**
   * 切换登录方式。
   *
   * @returns void
   */
  const toggleLoginType = (): void => {
    setIsAccountLogin(prev => !prev)
    setFormData(prev => ({
      ...prev,
      password: ''
    }))
  }

  /**
   * 更新表单字段值。
   *
   * @param field 字段名
   * @param value 字段值
   * @returns void
   */
  const handleInputChange = (field: string, value: string): void => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }

  /**
   * 根据当前登录方式构建登录参数。
   *
   * @returns 登录参数
   */
  const buildLoginParams = (): LoginParams => {
    if (isAccountLogin) {
      return {
        username: formData.username.trim(),
        password: formData.password
      }
    }
    return {
      username: formData.phone.trim(),
      password: formData.password
    }
  }

  /**
   * 执行登录请求。
   *
   * @param loginParams 登录参数
   * @param captchaVerificationResult 验证码校验结果
   * @returns Promise<void>
   */
  const submitLogin = async (
    loginParams: LoginParams,
    captchaVerificationResult: CaptchaVerificationResult
  ): Promise<void> => {
    setLoading(true)
    try {
      const res = await login({
        ...loginParams,
        captchaVerificationId: captchaVerificationResult.id
      })
      if (res.status === 200 && res.data.code === 200 && res.data.data?.accessToken) {
        setTokens(res.data.data.accessToken, res.data.data.refreshToken)
        showSuccessNotify('欢迎回来')
        navigate('/', { replace: true })
      } else {
        showNotify(res.data?.message || '登录失败')
      }
    } catch (error) {
      console.log(error)
      showNotify('网络错误，请稍后重试')
    } finally {
      setLoading(false)
      setCaptchaOpen(false)
      setPendingLoginParams(null)
    }
  }

  /**
   * 点击登录按钮后触发验证码弹层。
   *
   * @returns void
   */
  const handleLogin = (): void => {
    if (!agreeTerms) {
      showNotify('请先阅读并同意协议')
      return
    }
    const loginParams = buildLoginParams()
    if (!loginParams.username) {
      showNotify(isAccountLogin ? '请输入账号' : '请输入手机号')
      return
    }
    if (!loginParams.password) {
      showNotify('请输入密码')
      return
    }
    setPendingLoginParams(loginParams)
    setCaptchaOpen(true)
  }

  /**
   * 取消验证码弹层并清理待登录参数。
   *
   * @returns void
   */
  const handleCaptchaCancel = (): void => {
    setCaptchaOpen(false)
    setPendingLoginParams(null)
  }

  /**
   * 验证码校验成功后继续登录。
   *
   * @param captchaVerificationResult 验证码校验结果
   * @returns Promise<void>
   */
  const handleCaptchaVerified = async (captchaVerificationResult: CaptchaVerificationResult): Promise<void> => {
    if (!pendingLoginParams) {
      setCaptchaOpen(false)
      return
    }
    await submitLogin(pendingLoginParams, captchaVerificationResult)
  }

  /**
   * 打开服务协议页面。
   *
   * @returns void
   */
  const handleOpenSoftwareAgreement = (): void => {
    navigate('/agreement/software')
  }

  /**
   * 打开隐私政策页面。
   *
   * @returns void
   */
  const handleOpenPrivacyAgreement = (): void => {
    navigate('/agreement/privacy')
  }

  return (
    <div className={styles.loginPage}>
      <div className={styles.content}>
        {/* 顶部 Logo */}
        <div className={styles.brand}>
          <span className={styles.appName}>药智通</span>
        </div>

        {/* 标题 */}
        <header className={styles.header}>
          <h1 className={styles.title}>欢迎回来</h1>
          <p className={styles.subtitle}>
            {isAccountLogin ? '通过账号密码访问您的健康空间' : '通过手机号码快速开启会话'}
          </p>
        </header>

        {/* 登录方式 Tab */}
        <div className={styles.loginTabs}>
          <div
            className={`${styles.tab} ${isAccountLogin ? styles.tabActive : ''}`}
            onClick={() => !isAccountLogin && toggleLoginType()}
          >
            账号登录
          </div>
          <div
            className={`${styles.tab} ${!isAccountLogin ? styles.tabActive : ''}`}
            onClick={() => isAccountLogin && toggleLoginType()}
          >
            手机号登录
          </div>
        </div>

        {/* 表单 */}
        <form autoComplete='off' onSubmit={event => event.preventDefault()}>
          <div className={styles.inputGroups}>
            <div className={styles.inputField}>
              <label className={styles.label}>{isAccountLogin ? '账号' : '手机号'}</label>
              <div className={styles.control}>
                <span className={styles.icon}>{isAccountLogin ? <User size={17} /> : <Smartphone size={17} />}</span>
                <Input
                  className={styles.nutInput}
                  type={isAccountLogin ? 'text' : 'tel'}
                  name={isAccountLogin ? 'manual-account' : 'manual-phone'}
                  placeholder={isAccountLogin ? '请输入账号' : '请输入手机号'}
                  value={isAccountLogin ? formData.username : formData.phone}
                  onChange={v => handleInputChange(isAccountLogin ? 'username' : 'phone', v)}
                />
              </div>
            </div>

            <div className={styles.inputField}>
              <label className={styles.label}>密码</label>
              <div className={styles.control}>
                <span className={styles.icon}>
                  <Eye size={17} style={{ opacity: 0.4 }} />
                </span>
                <Input
                  className={styles.nutInput}
                  name='manual-password'
                  type={showPassword ? 'text' : 'password'}
                  placeholder='请输入密码'
                  value={formData.password}
                  onChange={v => handleInputChange('password', v)}
                />
                <div onClick={() => setShowPassword(!showPassword)} className={styles.eyeBtn}>
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </div>
              </div>
            </div>
          </div>

          <div className={styles.forgotRow}>
            <span className={styles.forgot}>忘记密码？</span>
          </div>

          <Button className={styles.submitBtn} type='primary' block loading={loading} onClick={handleLogin}>
            登 录
          </Button>
        </form>
      </div>

      {/* 底部：协议 + ICP */}
      <footer className={styles.footer}>
        <div className={styles.agreementRow}>
          <Checkbox checked={agreeTerms} onChange={setAgreeTerms} className={styles.checkbox} />
          <p className={styles.agreementText}>
            登录即代表同意{' '}
            <span className={styles.link} onClick={handleOpenSoftwareAgreement}>
              服务协议
            </span>{' '}
            与{' '}
            <span className={styles.link} onClick={handleOpenPrivacyAgreement}>
              隐私政策
            </span>
          </p>
        </div>
        <div className={styles.icpRow}>
          <a className={styles.icpLink} href='https://beian.miit.gov.cn/' target='_blank' rel='noopener noreferrer'>
            陕ICP备2023007009号-2
          </a>
        </div>
      </footer>

      <SliderCaptchaModal onCancel={handleCaptchaCancel} onVerified={handleCaptchaVerified} open={captchaOpen} />
    </div>
  )
}

export default Login
