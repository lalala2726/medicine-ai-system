package com.zhangyichuang.medicine.client.controller;

import com.zhangyichuang.medicine.client.service.MallCategoryService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.security.annotation.Anonymous;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.vo.MallCategoryTree;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城商品分类前台接口
 *
 * @author Chuang
 */
@RestController
@RequestMapping("/mall/category")
@RequiredArgsConstructor
@Tag(name = "商城商品分类前台接口")
@Anonymous
public class MallCategoryController extends BaseController {

    private final MallCategoryService mallCategoryService;

    /**
     * 获取商品分类树（仅启用分类）
     *
     * @return 商品分类树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取商品分类树")
    public AjaxResult<List<MallCategoryTree>> categoryTree() {
        List<MallCategoryTree> tree = mallCategoryService.categoryTree();
        return success(tree);
    }

    /**
     * 获取指定父分类下的子分类树（仅启用分类）
     *
     * @param parentId 父分类ID
     * @return 子分类树
     */
    @GetMapping("/children")
    @Operation(summary = "获取指定父分类下的子分类树")
    public AjaxResult<List<MallCategoryTree>> categoryChildren(@RequestParam("parentId") Long parentId) {
        List<MallCategoryTree> children = mallCategoryService.categoryChildren(parentId);
        return success(children);
    }

    /**
     * 获取指定父分类下的同级分类（不包含子级）
     *
     * @param parentId 父分类ID
     * @return 同级分类列表
     */
    @GetMapping("/siblings")
    @Operation(summary = "获取指定父分类下的同级分类（不包含子级）")
    public AjaxResult<List<MallCategoryTree>> categorySiblings(@RequestParam("parentId") Long parentId) {
        List<MallCategoryTree> siblings = mallCategoryService.categorySiblings(parentId);
        return success(siblings);
    }
}
