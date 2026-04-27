import React, { useEffect, useMemo, useState } from 'react'
import { rawRequest } from '@/request/rawRequestClient'
import { resolveAssetUrl } from '@/utils/asset'
import { getBaseURL } from '@/utils/env'

/**
 * 需要走鉴权拉取的资源前缀。
 */
const AUTH_ASSET_PREFIX = `${getBaseURL()}/`

/**
 * 鉴权图片组件属性。
 */
interface AuthImageProps extends Omit<React.ImgHTMLAttributes<HTMLImageElement>, 'src'> {
  /** 原始图片地址。 */
  src?: string
}

/**
 * 渲染需要登录态访问的图片资源。
 *
 * @param props 图片属性
 * @returns 鉴权后的图片元素
 */
const AuthImage: React.FC<AuthImageProps> = ({ src, alt, ...restProps }) => {
  const resolvedSrc = useMemo(() => resolveAssetUrl(src), [src])
  const [displaySrc, setDisplaySrc] = useState<string | undefined>(() => {
    if (!resolvedSrc || !resolvedSrc.startsWith(AUTH_ASSET_PREFIX)) {
      return resolvedSrc
    }

    return undefined
  })

  useEffect(() => {
    if (!resolvedSrc) {
      setDisplaySrc(undefined)
      return
    }

    if (!resolvedSrc.startsWith(AUTH_ASSET_PREFIX)) {
      setDisplaySrc(resolvedSrc)
      return
    }

    const abortController = new AbortController()
    let objectUrl = ''

    const loadProtectedImage = async () => {
      try {
        const response = await rawRequest({
          url: resolvedSrc,
          signal: abortController.signal
        })
        const contentType = response.headers.get('Content-Type') || ''

        if (!contentType.startsWith('image/')) {
          setDisplaySrc(undefined)
          return
        }

        const imageBlob = await response.blob()
        objectUrl = URL.createObjectURL(imageBlob)
        setDisplaySrc(objectUrl)
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        console.error('加载鉴权图片失败:', error)
        setDisplaySrc(undefined)
      }
    }

    void loadProtectedImage()

    return () => {
      abortController.abort()

      if (objectUrl) {
        URL.revokeObjectURL(objectUrl)
      }
    }
  }, [resolvedSrc])

  if (!displaySrc) {
    return null
  }

  return <img src={displaySrc} alt={alt} {...restProps} />
}

export default AuthImage
