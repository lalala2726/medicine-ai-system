import React from 'react'
import { Button } from '@nutui/nutui-react'
import { useNavigate } from 'react-router-dom'
import styles from './index.module.less'
import Empty from '@/components/Empty'

const NotFound: React.FC = () => {
  const navigate = useNavigate()

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <Empty description='您访问的页面不存在'>
          <p className={styles.description}>链接可能已失效，或者这个入口已经下线了。</p>
          <Button type='primary' className={styles.button} onClick={() => navigate('/', { replace: true })}>
            返回首页
          </Button>
        </Empty>
      </div>
    </div>
  )
}

export default NotFound
