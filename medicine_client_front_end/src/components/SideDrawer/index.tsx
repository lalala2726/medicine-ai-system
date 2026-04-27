import type { PropsWithChildren } from 'react'

import styles from './index.module.less'

interface SideDrawerProps extends PropsWithChildren {
  open: boolean
  title?: string
}

const SideDrawer = ({ open, title, children }: SideDrawerProps) => {
  return (
    <aside
      className={`${styles.drawer} ${open ? styles.open : ''}`}
      role='dialog'
      aria-modal='true'
      aria-hidden={!open}
    >
      {title && (
        <div className={styles.header}>
          <h2 className={styles.title}>{title}</h2>
        </div>
      )}
      <div className={styles.content}>{children}</div>
    </aside>
  )
}

export default SideDrawer
