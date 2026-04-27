package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.admin.service.MallProductUnitService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.request.MallProductUnitAddRequest;
import com.zhangyichuang.medicine.model.vo.MallProductUnitVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品单位控制器。
 *
 * @author Chuang
 */
@RestController
@RequestMapping("/mall/product/unit")
@Validated
@RequiredArgsConstructor
@Tag(name = "商品单位管理", description = "提供商品单位下拉选项的查询、新增与删除能力")
@PreventDuplicateSubmit
public class MallProductUnitController extends BaseController {

    /**
     * 商品单位服务。
     */
    private final MallProductUnitService mallProductUnitService;

    /**
     * 查询商品单位下拉选项。
     *
     * @return 商品单位下拉选项列表
     */
    @GetMapping("/option")
    @Operation(summary = "查询商品单位下拉选项")
    @PreAuthorize(
            "hasAuthority('mall:product:list') or hasAuthority('mall:product:query') "
                    + "or hasAuthority('mall:product:add') or hasAuthority('mall:product:edit') "
                    + "or hasRole('super_admin')"
    )
    public AjaxResult<List<MallProductUnitVo>> option() {
        return success(mallProductUnitService.option());
    }

    /**
     * 新增商品单位。
     *
     * @param request 商品单位新增请求
     * @return 新增后的商品单位
     */
    @PostMapping
    @Operation(summary = "新增商品单位")
    @PreAuthorize("hasAuthority('mall:product:add') or hasAuthority('mall:product:edit') or hasRole('super_admin')")
    public AjaxResult<MallProductUnitVo> add(@Validated @RequestBody MallProductUnitAddRequest request) {
        return success(mallProductUnitService.addUnit(request));
    }

    /**
     * 删除商品单位。
     *
     * @param id 商品单位ID
     * @return 删除结果
     */
    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "删除商品单位")
    @PreAuthorize("hasAuthority('mall:product:add') or hasAuthority('mall:product:edit') or hasRole('super_admin')")
    public AjaxResult<Void> delete(@PathVariable Long id) {
        return toAjax(mallProductUnitService.deleteUnit(id));
    }
}
