import styles from './index.module.less'

/** 选择器 Tab 项配置。 */
export interface SelectorTabItem {
  /** 当前 Tab 的唯一标识。 */
  key: string | number
  /** 当前 Tab 的展示文案。 */
  title: string
}

/** 自研选择器 Tab 组件属性。 */
interface SelectorTabsProps {
  /** 当前激活的 Tab 值。 */
  value: string | number
  /** 需要展示的全部 Tab 项。 */
  tabs: readonly SelectorTabItem[]
  /** 切换 Tab 时的回调。 */
  onChange: (value: string | number) => void
  /** 组件根节点的额外样式类名。 */
  className?: string
  /** Tab 列表的无障碍标签。 */
  ariaLabel?: string
}

/**
 * 拼接组件需要的 className 字符串。
 *
 * @param classNames - 需要参与拼接的 className 集合
 * @returns 已过滤空值后的 className 字符串
 */
const joinClassNames = (classNames: Array<string | undefined | false | null>): string => {
  return classNames.filter(Boolean).join(' ')
}

/**
 * 弹层顶部的状态筛选 Tab 组件。
 * 使用按钮组实现同类 NutUI Tabs 的视觉效果，并避免第三方组件手势干扰。
 *
 * @param props - 组件属性
 * @returns 选择器 Tab 组件节点
 */
const SelectorTabs = ({ value, tabs, onChange, className, ariaLabel = '状态筛选' }: SelectorTabsProps) => {
  return (
    <div className={joinClassNames([styles.container, className])}>
      <div className={styles.tabList} role='tablist' aria-label={ariaLabel}>
        {tabs.map(tab => {
          /** 当前 Tab 是否处于激活态。 */
          const isActive = tab.key === value

          return (
            <button
              key={tab.key}
              type='button'
              role='tab'
              aria-selected={isActive}
              className={joinClassNames([styles.tabButton, isActive && styles.tabButtonActive])}
              onClick={() => onChange(tab.key)}
            >
              <span className={styles.tabLabel}>{tab.title}</span>
            </button>
          )
        })}
      </div>
    </div>
  )
}

export default SelectorTabs
