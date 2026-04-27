import { useState, useEffect, useRef, useCallback } from 'react'

interface UseInfiniteScrollOptions<T> {
  /** 获取数据的函数，返回 { rows, total } */
  fetchData: (pageNum: number, pageSize: number) => Promise<{ rows: T[]; total: number }>
  /** 每页条数，默认 10 */
  pageSize?: number
  /** 去重 key 提取函数 */
  getKey?: (item: T) => string | number | undefined
  /** 触发加载的距离阈值（距离底部多少 px 开始加载），默认 100 */
  threshold?: number
  /** 滚动容器的 ref，如果不传则监听 window */
  scrollRef?: React.RefObject<HTMLElement | null>
  /** 是否立即加载第一页，默认 true */
  immediate?: boolean
}

interface UseInfiniteScrollReturn<T> {
  /** 当前列表数据 */
  list: T[]
  /** 是否正在加载第一页 */
  loading: boolean
  /** 是否正在加载更多 */
  loadingMore: boolean
  /** 是否还有更多数据 */
  hasMore: boolean
  /** 手动刷新（重置到第一页） */
  refresh: () => Promise<void>
  /** 手动设置列表 */
  setList: React.Dispatch<React.SetStateAction<T[]>>
  /** 重置状态并重新加载 */
  reset: () => void
}

export function useInfiniteScroll<T>(options: UseInfiniteScrollOptions<T>): UseInfiniteScrollReturn<T> {
  const { fetchData, pageSize = 10, getKey, threshold = 100, scrollRef, immediate = true } = options

  const [list, setList] = useState<T[]>([])
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [hasMore, setHasMore] = useState(true)

  const pageNumRef = useRef(1)
  const isLoadingRef = useRef(false)
  const totalLoadedRef = useRef(0)
  const hasMoreRef = useRef(true)
  /** 缓存当前列表快照，避免分页合并时读取到旧状态。 */
  const listRef = useRef<T[]>([])
  const fetchDataRef = useRef(fetchData)
  const getKeyRef = useRef(getKey)
  const pageSizeRef = useRef(pageSize)

  // 保持 ref 与最新 props 同步
  fetchDataRef.current = fetchData
  getKeyRef.current = getKey
  pageSizeRef.current = pageSize

  // hasMore 同步到 ref，供 scroll handler 读取
  useEffect(() => {
    hasMoreRef.current = hasMore
  }, [hasMore])

  /** 保持列表 ref 与当前列表状态同步，便于分页合并时读取最新值。 */
  useEffect(() => {
    listRef.current = list
  }, [list])

  const loadData = useCallback(
    async (page: number, isRefresh: boolean = false) => {
      if (isLoadingRef.current) return
      isLoadingRef.current = true

      if (isRefresh || page === 1) {
        setLoading(true)
      } else {
        setLoadingMore(true)
      }

      try {
        const currentPageSize = pageSizeRef.current
        const currentGetKey = getKeyRef.current
        const result = await fetchDataRef.current(page, currentPageSize)

        if (result && result.rows) {
          let nextList: T[] = []

          if (isRefresh || page === 1) {
            nextList = result.rows
          } else {
            const currentList = listRef.current
            if (currentGetKey) {
              const existingKeys = new Set(currentList.map(item => currentGetKey(item)))
              const newItems = result.rows.filter(item => !existingKeys.has(currentGetKey(item)))
              nextList = [...currentList, ...newItems]
            } else {
              nextList = [...currentList, ...result.rows]
            }
          }

          setList(nextList)
          listRef.current = nextList
          totalLoadedRef.current = nextList.length

          // 判断是否还有更多：当前返回条数不足一页，或已加载总数 >= total
          const newHasMore = result.rows.length >= currentPageSize && totalLoadedRef.current < result.total
          setHasMore(newHasMore)
          hasMoreRef.current = newHasMore
          pageNumRef.current = page
        } else {
          if (isRefresh || page === 1) {
            setList([])
            listRef.current = []
          }
          setHasMore(false)
          hasMoreRef.current = false
        }
      } catch (error) {
        console.error('加载数据失败:', error)
      } finally {
        setLoading(false)
        setLoadingMore(false)
        isLoadingRef.current = false
      }
    },
    [] // 无外部依赖，通过 ref 读取最新值
  )

  // 滚动加载
  useEffect(() => {
    const handleScroll = () => {
      if (!hasMoreRef.current || isLoadingRef.current) return

      let isNearBottom = false
      if (scrollRef?.current) {
        const el = scrollRef.current
        isNearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < threshold
      } else {
        isNearBottom = document.documentElement.scrollHeight - window.scrollY - window.innerHeight < threshold
      }

      if (isNearBottom) {
        loadData(pageNumRef.current + 1)
      }
    }

    // scrollRef.current 在首次 render 时可能还是 null，用 rAF 延迟绑定
    let target: HTMLElement | Window | null = null
    const bind = () => {
      target = scrollRef?.current || window
      target.addEventListener('scroll', handleScroll, { passive: true })
    }
    const rafId = requestAnimationFrame(bind)
    return () => {
      cancelAnimationFrame(rafId)
      if (target) target.removeEventListener('scroll', handleScroll)
    }
  }, [loadData, threshold, scrollRef])

  // 初始加载
  useEffect(() => {
    if (immediate) {
      loadData(1, true)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const refresh = useCallback(async () => {
    pageNumRef.current = 1
    setHasMore(true)
    hasMoreRef.current = true
    totalLoadedRef.current = 0
    await loadData(1, true)
  }, [loadData])

  const reset = useCallback(() => {
    pageNumRef.current = 1
    totalLoadedRef.current = 0
    setList([])
    listRef.current = []
    setHasMore(true)
    hasMoreRef.current = true
    setLoading(false)
    setLoadingMore(false)
    isLoadingRef.current = false
    loadData(1, true)
  }, [loadData])

  return { list, loading, loadingMore, hasMore, refresh, setList, reset }
}
