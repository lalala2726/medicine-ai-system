import React, { useState, useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft, Search, X, Trash2 } from 'lucide-react'
import { suggest } from '@/api/product'
import { useDebounceValue } from '@/hooks/useDebounce'
import { useSearchStore } from '@/stores/searchStore'
import styles from './index.module.less'

// 热门搜索
const HOT_SEARCHES = ['感冒灵', '布洛芬', '阿莫西林', '维生素C', '头孢', '止咳糖浆', '胃药', '消炎药']

const SearchPage: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()

  const { keyword: storedKeyword, setSearchState } = useSearchStore()

  const [inputValue, setInputValue] = useState(() => {
    const state = location.state as { fromHomeSearch?: boolean } | null
    if (state?.fromHomeSearch) {
      return ''
    }
    return storedKeyword
  })

  useEffect(() => {
    setSearchState({ keyword: inputValue })
  }, [inputValue, setSearchState])

  const [suggestions, setSuggestions] = useState<string[]>([])
  const [suggestLoading, setSuggestLoading] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  const [searchHistory, setSearchHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('search_history')
    return saved ? JSON.parse(saved) : []
  })

  const debouncedInput = useDebounceValue(inputValue, 300)

  useEffect(() => {
    const state = location.state as { fromHomeSearch?: boolean; keyword?: string } | null
    const fromHomeSearch = Boolean(state?.fromHomeSearch)

    const focusDelay = fromHomeSearch ? 100 : 300
    const focusTimer = setTimeout(() => {
      inputRef.current?.focus()
    }, focusDelay)

    return () => clearTimeout(focusTimer)
  }, [location.state])

  useEffect(() => {
    if (!debouncedInput) {
      setSuggestions([])
      return
    }

    const fetchSuggestions = async () => {
      setSuggestLoading(true)
      try {
        const data = await suggest(debouncedInput)
        setSuggestions(data || [])
      } catch (error) {
        console.error('获取搜索建议失败:', error)
        setSuggestions([])
      } finally {
        setSuggestLoading(false)
      }
    }

    fetchSuggestions()
  }, [debouncedInput])

  const addToHistory = (keyword: string) => {
    setSearchHistory(prev => {
      const newHistory = [keyword, ...prev.filter(item => item !== keyword)].slice(0, 10)
      localStorage.setItem('search_history', JSON.stringify(newHistory))
      return newHistory
    })
  }

  const clearHistory = () => {
    setSearchHistory([])
    localStorage.removeItem('search_history')
  }

  const handleBack = () => navigate(-1)

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value
    setInputValue(value)
    if (!value) setSuggestions([])
  }

  const handleClearInput = () => {
    setInputValue('')
    setSuggestions([])
    inputRef.current?.focus()
  }

  const doSearch = (keyword: string) => {
    if (!keyword.trim()) return
    addToHistory(keyword.trim())
    navigate(`/search-result?keyword=${encodeURIComponent(keyword.trim())}`)
  }

  const handleSubmit = () => {
    if (inputValue.trim()) doSearch(inputValue.trim())
  }

  const handleTagClick = (keyword: string) => doSearch(keyword)
  const handleSuggestionClick = (suggestion: string) => doSearch(suggestion)

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') handleSubmit()
  }

  return (
    <div className={styles.searchPage}>
      <div className={styles.backdrop} />

      <div className={styles.header}>
        <div className={styles.backBtn} onClick={handleBack}>
          <ArrowLeft size={24} />
        </div>
        <div className={styles.inputContainer}>
          <div className={styles.inputWrapper}>
            <Search size={18} className={styles.inputIcon} />
            <input
              ref={inputRef}
              type='text'
              className={styles.input}
              placeholder='搜索药品名称'
              value={inputValue}
              onChange={handleInputChange}
              onKeyDown={handleKeyDown}
            />
            {inputValue && (
              <div className={styles.clearBtn} onClick={handleClearInput}>
                <X size={16} />
              </div>
            )}
          </div>
        </div>
        <span className={styles.searchAction} onClick={handleSubmit}>
          搜索
        </span>
      </div>

      {suggestions.length === 0 && (
        <div className={styles.historySection}>
          {searchHistory.length > 0 && (
            <div className={styles.historyBlock}>
              <div className={styles.historyHeader}>
                <span className={styles.historyTitle}>搜索历史</span>
                <div className={styles.historyDelete} onClick={clearHistory}>
                  <Trash2 size={16} />
                </div>
              </div>
              <div className={styles.historyTags}>
                {searchHistory.map((item, index) => (
                  <span key={index} className={styles.historyTag} onClick={() => handleTagClick(item)}>
                    <span className={styles.historyTagText}>{item}</span>
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className={styles.historyBlock}>
            <div className={styles.historyHeader}>
              <span className={styles.historyTitle}>热门搜索</span>
            </div>
            <div className={styles.historyTags}>
              {HOT_SEARCHES.map((item, index) => (
                <span key={index} className={styles.historyTag} onClick={() => handleTagClick(item)}>
                  <span className={styles.historyTagText}>{item}</span>
                </span>
              ))}
            </div>
          </div>
        </div>
      )}

      {suggestions.length > 0 && (
        <div className={styles.suggestList}>
          {suggestLoading ? (
            <div className={styles.suggestLoading}>
              <div className={styles.spinner} />
              <span>搜索中...</span>
            </div>
          ) : (
            suggestions.map((item, index) => (
              <div key={index} className={styles.suggestItem} onClick={() => handleSuggestionClick(item)}>
                <Search size={16} className={styles.suggestIcon} />
                <span className={styles.suggestText}>{item}</span>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}

export default SearchPage
