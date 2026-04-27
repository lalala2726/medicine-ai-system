import React, { useState, useRef, useEffect, useCallback } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ChevronDown, ChevronUp } from 'lucide-react'
import { NavBar, Loading } from '@nutui/nutui-react'
import type { CategoryTypes } from '@/api/category'
import AuthImage from '@/components/AuthImage'
import { useCategoryStore } from '@/stores/categoryStore'
import { getCategoryIconText } from '@/utils/category'
import styles from './index.module.less'

const Category: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const [activeCategory1, setActiveCategory1] = useState<string | number>('')
  const [activeCategory2, setActiveCategory2] = useState<string | number>('')
  const [isExpanded, setIsExpanded] = useState<boolean>(false)
  const { categories: categoryData, loading, fetchCategoryTree } = useCategoryStore()

  const contentRef = useRef<HTMLDivElement>(null)
  const scrollContainerRef = useRef<HTMLDivElement>(null)
  const stateActiveId = (location.state as { activeId?: string | number } | null)?.activeId

  const loadCategories = useCallback(async () => {
    try {
      await fetchCategoryTree()
    } catch (error) {
      console.error('Failed to load categories:', error)
    }
  }, [fetchCategoryTree])

  useEffect(() => {
    void loadCategories()
  }, [loadCategories])

  useEffect(() => {
    if (categoryData.length === 0) return

    if (stateActiveId && categoryData.some(item => String(item.id) === String(stateActiveId))) {
      setActiveCategory1(stateActiveId)
      return
    }

    setActiveCategory1(prevActive => {
      if (prevActive && categoryData.some(item => String(item.id) === String(prevActive))) {
        return prevActive
      }
      return categoryData[0].id || ''
    })
  }, [categoryData, stateActiveId])

  const currentCategoryData = React.useMemo(
    () => categoryData.find(c => String(c.id) === String(activeCategory1)),
    [activeCategory1, categoryData]
  )

  const level2List = React.useMemo(() => currentCategoryData?.children || [], [currentCategoryData])

  useEffect(() => {
    if (level2List.length > 0) {
      setActiveCategory2(level2List[0].id || '')
      if (contentRef.current) {
        contentRef.current.scrollTop = 0
      }
    } else {
      setActiveCategory2('')
    }
    setIsExpanded(false)
  }, [level2List])

  const scrollToSection = (id: string | number) => {
    setActiveCategory2(id)
    setIsExpanded(false)

    // 滚动横向分类导航栏，让选中项可见
    if (scrollContainerRef.current) {
      const activeTab = scrollContainerRef.current.querySelector(`[data-category-id="${id}"]`) as HTMLElement
      if (activeTab) {
        const container = scrollContainerRef.current
        const tabLeft = activeTab.offsetLeft
        const tabWidth = activeTab.offsetWidth
        const containerWidth = container.offsetWidth
        const scrollLeft = container.scrollLeft

        if (tabLeft + tabWidth > scrollLeft + containerWidth) {
          container.scrollTo({
            left: tabLeft + tabWidth - containerWidth + 20,
            behavior: 'smooth'
          })
        } else if (tabLeft < scrollLeft) {
          container.scrollTo({
            left: tabLeft - 20,
            behavior: 'smooth'
          })
        }
      }
    }

    const element = document.getElementById(`section-${id}`)
    if (element && contentRef.current) {
      const offsetTop = element.offsetTop
      contentRef.current.scrollTo({
        top: offsetTop,
        behavior: 'smooth'
      })
    }
  }

  // 跳转到分类结果页
  const handleItemClick = (item: CategoryTypes.MallCategoryTree, parentId: string | number) => {
    navigate('/category-result', {
      state: {
        categoryId: item.id,
        parentId: parentId,
        name: item.name
      }
    })
  }

  if (loading && categoryData.length === 0) {
    return (
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: 'var(--app-viewport-height)',
          background: 'var(--background-light)'
        }}
      >
        <Loading>加载中...</Loading>
      </div>
    )
  }

  return (
    <div className={styles.pageWrapper}>
      <div className={styles.topBar}>
        <NavBar back={<></>}>
          <div className='title'>药品分类</div>
        </NavBar>
      </div>

      <div className={styles.categoryContainer}>
        <div className={styles.sidebar}>
          {categoryData.map(item => (
            <div
              key={item.id}
              className={`${styles.sidebarItem} ${String(activeCategory1) === String(item.id) ? styles.active : ''}`}
              onClick={() => setActiveCategory1(item.id || '')}
            >
              {item.name}
            </div>
          ))}
        </div>

        <div className={styles.mainContent}>
          {level2List.length > 0 && (
            <div className={styles.level2Header}>
              <div className={styles.scrollContainer} ref={scrollContainerRef}>
                {level2List.map(sub => (
                  <div
                    key={sub.id}
                    data-category-id={sub.id}
                    className={`${styles.level2Tab} ${String(activeCategory2) === String(sub.id) ? styles.active : ''}`}
                    onClick={() => scrollToSection(sub.id || '')}
                  >
                    {sub.name}
                  </div>
                ))}
              </div>

              <div className={styles.expandBtn} onClick={() => setIsExpanded(!isExpanded)}>
                {isExpanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
              </div>
            </div>
          )}

          <div className={styles.contentBody} ref={contentRef}>
            {level2List.length > 0
              ? level2List.map(sub => (
                  <div key={sub.id} id={`section-${sub.id}`}>
                    <div className={styles.sectionTitle} onClick={() => handleItemClick(sub, activeCategory1)}>
                      {sub.name}
                    </div>
                    <div className={styles.grid}>
                      {sub.children && sub.children.length > 0 ? (
                        sub.children.map(item => (
                          <div key={item.id} className={styles.gridItem} onClick={() => handleItemClick(item, sub.id!)}>
                            <div className={styles.imageWrapper}>
                              {item.cover ? (
                                <AuthImage src={item.cover} alt={item.name} />
                              ) : (
                                <div className={styles.iconFallback}>
                                  <span className={styles.iconFallbackText}>{getCategoryIconText(item.name)}</span>
                                </div>
                              )}
                            </div>
                            <div className={styles.itemName}>{item.name}</div>
                          </div>
                        ))
                      ) : (
                        <div className={styles.gridItem} onClick={() => handleItemClick(sub, activeCategory1)}>
                          <div className={styles.imageWrapper}>
                            {sub.cover ? (
                              <AuthImage src={sub.cover} alt={sub.name} />
                            ) : (
                              <div className={styles.iconFallback}>
                                <span className={styles.iconFallbackText}>{getCategoryIconText(sub.name)}</span>
                              </div>
                            )}
                          </div>
                          <div className={styles.itemName}>{sub.name}</div>
                        </div>
                      )}
                    </div>
                  </div>
                ))
              : currentCategoryData && (
                  <div className={styles.grid} style={{ padding: '16px' }}>
                    <div className={styles.gridItem} onClick={() => handleItemClick(currentCategoryData, 0)}>
                      <div className={styles.imageWrapper}>
                        {currentCategoryData.cover ? (
                          <AuthImage src={currentCategoryData.cover} alt={currentCategoryData.name} />
                        ) : (
                          <div className={styles.iconFallback}>
                            <span className={styles.iconFallbackText}>
                              {getCategoryIconText(currentCategoryData.name)}
                            </span>
                          </div>
                        )}
                      </div>
                      <div className={styles.itemName}>{currentCategoryData.name}</div>
                    </div>
                  </div>
                )}
          </div>
        </div>

        {isExpanded && level2List.length > 0 && (
          <>
            <div className={styles.overlay} onClick={() => setIsExpanded(false)}></div>
            <div className={styles.dropdownMenu}>
              <div className={styles.dropdownHeader}>
                <span>{currentCategoryData?.name}</span>
                <div className={styles.closeBtn} onClick={() => setIsExpanded(false)}>
                  <ChevronUp size={20} />
                </div>
              </div>
              <div className={styles.dropdownGrid}>
                {level2List.map(sub => (
                  <div
                    key={sub.id}
                    className={`${styles.dropdownItem} ${String(activeCategory2) === String(sub.id) ? styles.active : ''}`}
                    onClick={() => scrollToSection(sub.id || '')}
                  >
                    {sub.name}
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

export default Category
