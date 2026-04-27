package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.vo.MallProductVo;
import com.zhangyichuang.medicine.admin.service.MallProductService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.request.MallProductAddRequest;
import com.zhangyichuang.medicine.model.request.MallProductListQueryRequest;
import com.zhangyichuang.medicine.model.request.MallProductUpdateRequest;
import com.zhangyichuang.medicine.model.vo.MallProductListVo;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商城商品控制器
 * <p>
 * 提供商城商品的增删改查功能，包括商品列表查询、商品详情查询、
 * 商品添加、商品修改和商品删除等功能。
 *
 * @author Chuang
 * created on 2025/10/4
 */
@RestController
@RequestMapping("/mall/product")
@RequiredArgsConstructor
@Tag(name = "商城商品接口", description = "提供商城商品的增删改查")
@PreventDuplicateSubmit
public class MallProductController extends BaseController {

    private final MallProductService mallProductService;

    /**
     * 获取商城商品列表
     *
     * @param request 查询参数
     * @return 商城商品列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取商城商品列表")
    @PreAuthorize("hasAuthority('mall:product:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listMallProduct(MallProductListQueryRequest request) {
        Page<MallProductDetailDto> page = mallProductService.listMallProductWithCategory(request);
        List<MallProductListVo> mallProductListVos = page.getRecords().stream()
                .map(product -> {
                    MallProductListVo productListVo = copyProperties(product, MallProductListVo.class);
                    List<MallProductTagVo> tags = product.getTags() == null ? List.of() : product.getTags();
                    productListVo.setTags(tags);
                    productListVo.setTagNames(tags.stream().map(MallProductTagVo::getName).toList());
                    if (product.getImages() != null && !product.getImages().isEmpty()) {
                        productListVo.setCoverImage(product.getImages().getFirst());
                    }
                    return productListVo;
                })
                .toList();
        return getTableData(page, mallProductListVos);
    }

    /**
     * 获取商城商品详情
     *
     * @param id 商品ID
     * @return 商城商品详情
     */
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "获取商城商品详情")
    @PreAuthorize("hasAuthority('mall:product:query') or hasRole('super_admin')")
    public AjaxResult<MallProductVo> getProductById(@PathVariable Long id) {
        MallProductDetailDto product = mallProductService.getMallProductById(id);
        MallProductVo productVo = copyProperties(product, MallProductVo.class);
        return success(productVo);
    }

    /**
     * 添加商城商品
     *
     * @param request 添加参数
     * @return 添加结果
     */
    @PostMapping
    @Operation(summary = "添加商城商品")
    @PreAuthorize("hasAuthority('mall:product:add') or hasRole('super_admin')")
    public AjaxResult<Void> addProduct(@Validated @RequestBody MallProductAddRequest request) {
        boolean result = mallProductService.addMallProduct(request);
        return toAjax(result);
    }

    /**
     * 修改商城商品
     *
     * @param request 修改参数
     * @return 修改结果
     */
    @PutMapping
    @Operation(summary = "修改商城商品")
    @PreAuthorize("hasAuthority('mall:product:edit') or hasRole('super_admin')")
    public AjaxResult<Void> updateProduct(@Validated @RequestBody MallProductUpdateRequest request) {
        boolean result = mallProductService.updateMallProduct(request);
        return toAjax(result);
    }

    /**
     * 删除商城商品
     *
     * @param ids 商品ID列表
     * @return 删除结果
     */
    @DeleteMapping("/{ids}")
    @Operation(summary = "删除商城商品")
    @PreAuthorize("hasAuthority('mall:product:delete') or hasRole('super_admin')")
    public AjaxResult<Void> deleteProduct(@PathVariable List<Long> ids) {
        boolean result = mallProductService.deleteMallProduct(ids);
        return toAjax(result);
    }

    /**
     * 获取售后列表
     */
    @GetMapping("/after-sale/list")
    @Operation
    @PreAuthorize("hasAuthority('mall:after_sale:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listAfterSale() {
        return success();
    }

    /**
     * 获取售后详情
     */
    @GetMapping("/after-sale/{id:\\d+}")
    @Operation(summary = "获取售后详情")
    @PreAuthorize("hasAuthority('mall:after_sale:query') or hasRole('super_admin')")
    public AjaxResult<Void> getAfterSaleById(@PathVariable Long id) {
        // TODO 获取售后详情
        return success();
    }

}
