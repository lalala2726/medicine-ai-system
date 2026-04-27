import React, { useEffect, useState } from 'react'

const MobileCheck: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isMobile, setIsMobile] = useState(true)

  useEffect(() => {
    const checkMobile = () => {
      const userAgent = navigator.userAgent.toLowerCase()
      const isMobileUA = /iphone|ipad|ipod|android|blackberry|windows phone/i.test(userAgent)

      // 检测 iPadOS：通过 userAgent 是否包含 "mac" 并且支持触摸
      const isIpad = /macintosh|macintel/i.test(navigator.userAgent) && navigator.maxTouchPoints > 1

      // 增加宽度判断，通常移动端宽度小于 1200px
      const isSmallScreen = window.innerWidth <= 1200

      // 如果是移动端UA、iPad或者屏幕宽度小于等于1200px，认为是移动端环境
      setIsMobile(isMobileUA || isIpad || isSmallScreen)
    }

    checkMobile()
    window.addEventListener('resize', checkMobile)
    return () => window.removeEventListener('resize', checkMobile)
  }, [])

  if (isMobile) {
    return <>{children}</>
  }

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: '#f7f8fa',
        zIndex: 9999,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px',
        textAlign: 'center'
      }}
    >
      <div
        style={{
          width: '80px',
          height: '80px',
          background: '#fff',
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: '24px',
          boxShadow: '0 4px 16px rgba(0,0,0,0.05)'
        }}
      >
        <svg width='40' height='40' viewBox='0 0 24 24' fill='none' xmlns='http://www.w3.org/2000/svg'>
          <path
            d='M7 2C5.89543 2 5 2.89543 5 4V20C5 21.1046 5.89543 22 7 22H17C18.1046 22 19 21.1046 19 20V4C19 2.89543 18.1046 2 17 2H7Z'
            stroke='#fa2c19'
            strokeWidth='2'
          />
          <path d='M12 18H12.01' stroke='#fa2c19' strokeWidth='2' strokeLinecap='round' />
        </svg>
      </div>
      <h2 style={{ fontSize: '20px', color: '#333', marginBottom: '12px', fontWeight: 600 }}>请在手机或平板端访问</h2>
      <p style={{ fontSize: '14px', color: '#666', lineHeight: '1.6', maxWidth: '300px' }}>
        为了获得最佳体验，请使用移动设备访问本站。
      </p>
      <div
        style={{
          marginTop: '32px',
          padding: '16px',
          background: '#fff',
          borderRadius: '12px',
          fontSize: '13px',
          color: '#999',
          maxWidth: '320px',
          border: '1px dashed #ddd'
        }}
      >
        <p style={{ marginBottom: '8px' }}>开发调试提示：</p>
        <p>
          PC端请按{' '}
          <span
            style={{ color: '#333', fontWeight: 'bold', background: '#eee', padding: '2px 6px', borderRadius: '4px' }}
          >
            F12
          </span>{' '}
          打开开发者工具
        </p>
        <p style={{ marginTop: '4px' }}>
          并切换至 <span style={{ color: '#333', fontWeight: 'bold' }}>移动端调试模式</span> (Ctrl+Shift+M)
        </p>
      </div>
    </div>
  )
}

export default MobileCheck
