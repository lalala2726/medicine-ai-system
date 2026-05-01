package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.MallCategoryMapper;
import com.zhangyichuang.medicine.admin.service.MallCategoryService;
import com.zhangyichuang.medicine.common.core.base.Option;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import com.zhangyichuang.medicine.model.entity.MallCategory;
import com.zhangyichuang.medicine.model.request.MallCategoryAddRequest;
import com.zhangyichuang.medicine.model.request.MallCategoryUpdateRequest;
import com.zhangyichuang.medicine.model.vo.MallCategoryTree;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 商城商品分类服务实现类
 * <p>
 * 实现商城商品分类的业务逻辑处理，包括分类的增删改查、
 * 分类树构建、分类选项获取等功能。
 *
 * @author Chuang
 * created on 2025/10/4
 */
@Service
public class MallCategoryServiceImpl extends ServiceImpl<MallCategoryMapper, MallCategory>
        implements MallCategoryService {

    @Override
    public List<MallCategoryTree> categoryTree() {
        List<MallCategory> categories = list();
        if (categories.isEmpty()) {
            return List.of();
        }
        return buildTree(categories, 0L);
    }

    @Override
    public List<Option<Long>> option() {
        LambdaQueryWrapper<MallCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MallCategory::getStatus, 0)
                .orderByAsc(MallCategory::getSort);

        List<MallCategory> categories = list(queryWrapper);
        return categories.stream()
                .map(category -> new Option<>(category.getId(), category.getName()))
                .toList();
    }

    @Override
    public MallCategory getCategoryById(Long id) {
        if (id == null) {
            throw new ServiceException("分类ID不能为空");
        }

        MallCategory category = getById(id);
        if (category == null) {
            throw new ServiceException("分类不存在");
        }

        return category;
    }

    @Override
    public boolean addCategory(MallCategoryAddRequest request) {
        // 检查分类名称是否已存在
        LambdaQueryWrapper<MallCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MallCategory::getName, request.getName());
        if (count(queryWrapper) > 0) {
            throw new ServiceException("分类名称已存在");
        }

        Long parentId = request.getParentId();
        if (parentId != null && parentId != 0L) {
            int depth = 1;
            MallCategory parent = getById(parentId);
            if (parent == null) {
                throw new ServiceException("父分类不存在");
            }
            depth++;
            while (parent.getParentId() != null && parent.getParentId() != 0L) {
                parent = getById(parent.getParentId());
                if (parent == null) {
                    throw new ServiceException("父分类不存在");
                }
                depth++;
                if (depth > 3) {
                    throw new ServiceException("分类层级不能超过3级");
                }
            }
        }

        MallCategory category = new MallCategory();
        BeanUtils.copyProperties(request, category);
        category.setStatus(0); // 默认启用
        category.setCreateTime(new Date());
        category.setCreateBy(SecurityUtils.getUsername());

        return save(category);
    }

    @Override
    @CacheEvict(cacheNames = RedisConstants.MallProduct.CACHE_NAME, allEntries = true)
    public boolean updateCategory(MallCategoryUpdateRequest request) {
        // 检查分类是否存在
        MallCategory existingCategory = getById(request.getId());
        if (existingCategory == null) {
            throw new ServiceException("分类不存在");
        }

        // 如果修改了父分类，检查是否将自己设置为父分类
        if (request.getParentId() != null && request.getParentId().equals(request.getId())) {
            throw new ServiceException("不能将自己设置为父分类");
        }

        // 检查分类名称是否已存在（排除自己）
        LambdaQueryWrapper<MallCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MallCategory::getName, request.getName())
                .ne(MallCategory::getId, request.getId());
        if (count(queryWrapper) > 0) {
            throw new ServiceException("分类名称已存在");
        }

        BeanUtils.copyProperties(request, existingCategory);
        existingCategory.setUpdateTime(new Date());
        existingCategory.setUpdateBy(SecurityUtils.getUsername());

        return updateById(existingCategory);
    }

    @Override
    @CacheEvict(cacheNames = RedisConstants.MallProduct.CACHE_NAME, allEntries = true)
    public boolean deleteCategory(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的分类");
        }

        for (Long id : ids) {
            // 检查分类是否存在
            MallCategory category = getById(id);
            if (category == null) {
                throw new ServiceException("分类不存在: " + id);
            }

            // 检查是否有子分类
            LambdaQueryWrapper<MallCategory> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MallCategory::getParentId, id);
            long childCount = count(queryWrapper);
            if (childCount > 0) {
                throw new ServiceException("分类【" + category.getName() + "】存在子分类，无法删除");
            }
        }

        return removeByIds(ids);
    }

    /**
     * 检查商品分类是否存在
     *
     * @param categoryId 商品分类ID
     * @return 是否存在
     */
    @Override
    public boolean isProductCategoryExist(Long categoryId) {
        LambdaQueryWrapper<MallCategory> eq = new LambdaQueryWrapper<MallCategory>()
                .eq(MallCategory::getId, categoryId);
        return count(eq) > 0;
    }

    /**
     * 递归构建树形结构
     *
     * @param categories 所有分类列表
     * @param parentId   父分类ID
     * @return 树形结构
     */
    private List<MallCategoryTree> buildTree(List<MallCategory> categories, Long parentId) {
        return categories.stream()
                .filter(category -> Objects.equals(category.getParentId(), parentId))
                .sorted((c1, c2) -> Integer.compare(c2.getSort(), c1.getSort()))
                .map(category -> {
                    MallCategoryTree tree = new MallCategoryTree();
                    tree.setId(category.getId());
                    tree.setName(category.getName());
                    tree.setParentId(category.getParentId());
                    tree.setSort(category.getSort());
                    tree.setStatus(category.getStatus());
                    if (category.getDescription() != null && !category.getDescription().isBlank()) {
                        tree.setDescription(category.getDescription());
                    }
                    if (category.getCover() != null && !category.getCover().isBlank()) {
                        tree.setCover(category.getCover());
                    }
                    List<MallCategoryTree> mallCategoryTrees = buildTree(categories, category.getId());
                    if (!mallCategoryTrees.isEmpty()) {
                        tree.setChildren(mallCategoryTrees);
                    }
                    return tree;
                }).toList();
    }
}
