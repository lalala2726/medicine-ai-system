import type { CategoryTypes } from '@/api/category'
import { resolveAssetUrl } from '@/utils/asset'

/**
 * 未配置排序值时使用的最小排序权重。
 */
const EMPTY_CATEGORY_SORT_VALUE = Number.NEGATIVE_INFINITY

/**
 * 获取分类排序值。
 *
 * @param category 分类数据
 * @returns 可用于比较的排序值
 */
const getCategorySortValue = (category: CategoryTypes.MallCategoryTree): number => {
  return typeof category.sort === 'number' ? category.sort : EMPTY_CATEGORY_SORT_VALUE
}

/**
 * 递归按排序值倒序整理分类列表。
 *
 * @param categories 原始分类列表
 * @returns 已按排序值倒序整理的新分类列表
 */
export const sortCategoriesBySortDesc = (
  categories: CategoryTypes.MallCategoryTree[]
): CategoryTypes.MallCategoryTree[] => {
  return categories
    .map((category, index) => ({ category, index }))
    .sort((left, right) => {
      const leftSortValue = getCategorySortValue(left.category)
      const rightSortValue = getCategorySortValue(right.category)

      if (leftSortValue !== rightSortValue) {
        return rightSortValue - leftSortValue
      }

      return left.index - right.index
    })
    .map(({ category }) => ({
      ...category,
      cover: resolveAssetUrl(category.cover),
      children: Array.isArray(category.children) ? sortCategoriesBySortDesc(category.children) : category.children
    }))
}

/**
 * 获取分类 icon 需要展示的首字。
 *
 * @param categoryName 分类名称
 * @returns 去空白后的首个字符
 */
export const getCategoryIconText = (categoryName?: string): string => {
  return categoryName?.replace(/\s+/g, '').charAt(0) || ''
}
