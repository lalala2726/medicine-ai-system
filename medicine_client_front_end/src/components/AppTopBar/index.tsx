import { NavBar } from '@nutui/nutui-react'
import { ArrowLeft } from '@nutui/icons-react'
import { useNavigate } from 'react-router-dom'
import type { ReactNode } from 'react'

interface AppTopBarProps {
  title?: string | ReactNode
  subtitle?: string
  showBack?: boolean
  backText?: string
  right?: ReactNode
  left?: ReactNode
  onBackClick?: () => void
  onTitleClick?: () => void
}

const AppTopBar = ({
  title = '页面标题',
  subtitle,
  showBack = true,
  right,
  left,
  onBackClick,
  onTitleClick
}: AppTopBarProps) => {
  const navigate = useNavigate()

  const styles = {
    flexCenter: {
      display: 'flex',
      alignItems: 'center'
    },
    title: {
      fontSize: '18px',
      fontWeight: 'bold',
      lineHeight: '26px'
    },
    description: {
      fontSize: '12px',
      fontWeight: 400,
      color: 'rgba(0,0,0, 0.5)',
      lineHeight: '16px'
    }
  }

  const handleBackClick = () => {
    if (onBackClick) {
      onBackClick()
    } else {
      navigate(-1)
    }
  }

  // 渲染标题内容
  const renderTitle = () => {
    if (subtitle) {
      return (
        <div style={{ ...styles.flexCenter, flexDirection: 'column' }}>
          <span style={styles.title} onClick={onTitleClick}>
            {title}
          </span>
          <span style={styles.description}>{subtitle}</span>
        </div>
      )
    }

    if (onTitleClick && typeof title === 'string') {
      return <span onClick={onTitleClick}>{title}</span>
    }

    return title
  }

  return (
    <NavBar
      title={renderTitle()}
      back={
        showBack ? (
          <>
            <ArrowLeft />
          </>
        ) : undefined
      }
      left={left}
      right={right}
      onBackClick={handleBackClick}
    />
  )
}

export default AppTopBar
