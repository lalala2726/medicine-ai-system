import React, { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@nutui/nutui-react'
import { ChevronLeft } from 'lucide-react'
import type { CaptchaVerificationResult } from '@/api/captcha'
import { redeemActivationCode, type CouponTypes } from '@/api/coupon'
import { showNotify, showSuccessNotify } from '@/utils/notify'
import SliderCaptchaModal from '@/pages/Login/components/SliderCaptchaModal'
import styles from './index.module.less'

/**
 * 格式化金额展示。
 * @param amount 金额文本。
 * @returns 格式化后的金额文本。
 */
function formatAmount(amount?: string): string {
  if (!amount) {
    return '0.00'
  }
  return Number(amount).toFixed(2)
}

/**
 * 激活码激活页面。
 */
const CouponActivationPage: React.FC = () => {
  const navigate = useNavigate()
  const [code, setCode] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [captchaOpen, setCaptchaOpen] = useState(false)
  const [pendingRedeemCode, setPendingRedeemCode] = useState<string | null>(null)
  const [result, setResult] = useState<CouponTypes.ActivationCodeRedeemVo | null>(null)

  /**
   * 返回上一页。
   * @returns 无返回值。
   */
  const handleBack = useCallback(() => {
    navigate(-1)
  }, [navigate])

  /**
   * 跳转回优惠券列表。
   * @returns 无返回值。
   */
  const handleGoCouponList = useCallback(() => {
    navigate('/coupon', { replace: true })
  }, [navigate])

  /**
   * 提交激活码兑换请求。
   *
   * @param normalizedCode 规范化后的激活码
   * @param captchaVerificationResult 验证码校验结果
   * @returns 无返回值
   */
  const submitRedeem = useCallback(
    async (normalizedCode: string, captchaVerificationResult: CaptchaVerificationResult): Promise<void> => {
      try {
        setSubmitting(true)
        const redeemResult = await redeemActivationCode({
          code: normalizedCode,
          captchaVerificationId: captchaVerificationResult.id
        })
        setResult(redeemResult)
        setCode('')
        setPendingRedeemCode(null)
        showSuccessNotify('兑换成功')
      } catch (error: any) {
        console.error('兑换失败:', error)
        showNotify(error?.message || '兑换失败')
      } finally {
        setSubmitting(false)
        setCaptchaOpen(false)
      }
    },
    []
  )

  /**
   * 点击兑换按钮后先打开验证码弹层。
   *
   * @returns 无返回值
   */
  const handleRedeem = useCallback((): void => {
    const normalizedCode = code.trim()
    if (!normalizedCode) {
      showNotify('请输入兑换码')
      return
    }
    setPendingRedeemCode(normalizedCode)
    setCaptchaOpen(true)
  }, [code])

  /**
   * 取消验证码校验。
   *
   * @returns 无返回值
   */
  const handleCaptchaCancel = useCallback((): void => {
    setCaptchaOpen(false)
    setPendingRedeemCode(null)
  }, [])

  /**
   * 验证码校验成功后继续兑换。
   *
   * @param captchaVerificationResult 验证码校验结果
   * @returns 无返回值
   */
  const handleCaptchaVerified = useCallback(
    async (captchaVerificationResult: CaptchaVerificationResult): Promise<void> => {
      if (!pendingRedeemCode) {
        setCaptchaOpen(false)
        return
      }
      await submitRedeem(pendingRedeemCode, captchaVerificationResult)
    },
    [pendingRedeemCode, submitRedeem]
  )

  return (
    <div className={styles.activationPage}>
      <div className={styles.pageHeader}>
        <div className={styles.headerLeft} onClick={handleBack}>
          <ChevronLeft size={24} />
        </div>
        <div className={styles.headerTitle}>兑换优惠券</div>
        <div className={styles.headerRight} />
      </div>

      <div className={styles.pageContent}>
        {!result ? (
          <>
            <div className={styles.heroSection}>
              <h2 className={styles.heroTitle}>兑换中心</h2>
              <p className={styles.heroSubtitle}>输入兑换码，轻松领取专享优惠</p>
            </div>

            <div className={styles.formSection}>
              <div className={styles.inputWrapper}>
                <input
                  type='text'
                  value={code}
                  placeholder='请输入或粘贴兑换码'
                  className={styles.codeInput}
                  onChange={event => setCode(event.target.value.toUpperCase())}
                />
              </div>
              <div className={styles.buttonWrapper}>
                <Button block type='primary' className={styles.submitBtn} loading={submitting} onClick={handleRedeem}>
                  {submitting ? '兑换中...' : '立即兑换'}
                </Button>
              </div>

              <div className={styles.rulesSection}>
                <div className={styles.ruleTitle}>兑换说明</div>
                <div className={styles.ruleList}>
                  <div className={styles.ruleItem}>兑换成功后系统会立即为您发放对应优惠券；</div>
                  <div className={styles.ruleItem}>每个兑换码仅限使用一次；</div>
                  <div className={styles.ruleItem}>请在兑换码有效期内使用，过期作废；</div>
                  <div className={styles.ruleItem}>同一共享码每个账号仅限兑换一次。</div>
                </div>
              </div>
            </div>
          </>
        ) : (
          <div className={styles.resultSection}>
            <div className={styles.successIcon}>
              <div className={styles.checkMark}>✓</div>
            </div>
            <div className={styles.successTitle}>兑换成功</div>
            <div className={styles.successSubtitle}>优惠券已发放至您的账户</div>

            <div className={styles.infoList}>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>优惠券名称</span>
                <span className={styles.infoValue}>{result.couponName || '-'}</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>可用金额</span>
                <span className={styles.infoValueAmount}>
                  ¥{formatAmount(result.availableAmount || result.totalAmount)}
                </span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>使用门槛</span>
                <span className={styles.infoValue}>满 ¥{formatAmount(result.thresholdAmount)} 可用</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>有效期至</span>
                <span className={styles.infoValue}>{result.expireTime || '-'}</span>
              </div>
            </div>

            <div className={styles.actionButtons}>
              <Button block type='primary' className={styles.submitBtn} onClick={handleGoCouponList}>
                立即查看
              </Button>
              <div className={styles.secondaryBtn} onClick={() => setResult(null)}>
                继续兑换
              </div>
            </div>
          </div>
        )}
      </div>
      <SliderCaptchaModal
        actionLabel='激活'
        onCancel={handleCaptchaCancel}
        onVerified={handleCaptchaVerified}
        open={captchaOpen}
      />
    </div>
  )
}

export default CouponActivationPage
