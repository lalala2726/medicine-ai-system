import React, { useEffect, useMemo, useState } from 'react'
import { useLocation } from 'react-router-dom'
import AppTopBar from '@/components/AppTopBar'
import MarkdownRenderer from '@/components/MarkdownRenderer'
import { getAgreementConfig } from '@/api/agreement'
import { showNotify } from '@/utils/notify'
import styles from './index.module.less'

/** 协议页面类型。 */
type AgreementPageType = 'software' | 'privacy'

/** 协议页面标题映射。 */
const AGREEMENT_PAGE_TITLE_MAP: Record<AgreementPageType, string> = {
  software: '服务协议',
  privacy: '隐私政策'
}

/** 协议页 Markdown 关闭代码块、表格的复制下载工具。 */
const AGREEMENT_MARKDOWN_CONTROLS = {
  code: false,
  table: false
}

/**
 * 协议配置页面。
 *
 * @returns 页面节点
 */
const AgreementPage: React.FC = () => {
  const location = useLocation()
  const [loading, setLoading] = useState(false)
  const [markdownContent, setMarkdownContent] = useState('')

  /**
   * 根据当前路由推导协议页面类型。
   */
  const pageType = useMemo<AgreementPageType>(() => {
    return location.pathname.endsWith('/privacy') ? 'privacy' : 'software'
  }, [location.pathname])

  /**
   * 当前协议页面标题。
   */
  const pageTitle = useMemo(() => {
    return AGREEMENT_PAGE_TITLE_MAP[pageType]
  }, [pageType])

  useEffect(() => {
    /**
     * 加载协议配置并提取当前页面需要展示的 Markdown 内容。
     *
     * @returns 无返回值
     */
    const loadAgreementConfig = async () => {
      setLoading(true)
      try {
        const config = await getAgreementConfig()
        if (pageType === 'software') {
          setMarkdownContent(config.softwareAgreementMarkdown)
          return
        }
        setMarkdownContent(config.privacyAgreementMarkdown)
      } catch (error) {
        console.error('加载协议配置失败:', error)
        showNotify('加载协议失败，请稍后重试')
      } finally {
        setLoading(false)
      }
    }

    void loadAgreementConfig()
  }, [pageType])

  return (
    <div className={styles.page}>
      <AppTopBar title={pageTitle} />
      <main className={styles.content}>
        {loading ? (
          <div className={styles.loading}>加载中...</div>
        ) : (
          <MarkdownRenderer
            className={styles.markdown}
            content={markdownContent}
            controls={AGREEMENT_MARKDOWN_CONTROLS}
            lineNumbers={false}
          />
        )}
      </main>
    </div>
  )
}

export default AgreementPage
