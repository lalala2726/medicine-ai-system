package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.service.MallProductTagTypeService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.request.MallProductTagTypeAddRequest;
import com.zhangyichuang.medicine.model.request.MallProductTagTypeListQueryRequest;
import com.zhangyichuang.medicine.model.request.MallProductTagTypeStatusUpdateRequest;
import com.zhangyichuang.medicine.model.request.MallProductTagTypeUpdateRequest;
import com.zhangyichuang.medicine.model.vo.MallProductTagTypeAdminVo;
import com.zhangyichuang.medicine.model.vo.MallProductTagTypeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品标签类型管理控制器。
 *
 * @author Chuang
 */
@RestController
@RequestMapping("/mall/product/tag-type")
@Validated
@RequiredArgsConstructor
@Tag(name = "商品标签类型管理", description = "提供商品标签类型的增删改查与状态管理")
@PreventDuplicateSubmit
public class MallProductTagTypeController extends BaseController {

    /**
     * 商品标签类型服务。
     */
    private final MallProductTagTypeService mallProductTagTypeService;

    /**
     * 查询商品标签类型列表。
     *
     * @param request 查询参数
     * @return 标签类型分页列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询商品标签类型列表")
    @PreAuthorize("hasAuthority('mall:product:tag:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> list(MallProductTagTypeListQueryRequest request) {
        Page<MallProductTagTypeAdminVo> page = mallProductTagTypeService.listTypes(request);
        return getTableData(page, page.getRecords());
    }

    /**
     * 查询启用标签类型下拉列表。
     *
     * @return 启用标签类型列表
     */
    @GetMapping("/option")
    @Operation(summary = "查询标签类型下拉列表")
    @PreAuthorize("hasAuthority('mall:product:tag:list') or hasRole('super_admin')")
    public AjaxResult<List<MallProductTagTypeVo>> option() {
        return success(mallProductTagTypeService.option());
    }

    /**
     * 查询商品标签类型详情。
     *
     * @param id 标签类型ID
     * @return 标签类型详情
     */
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "查询商品标签类型详情")
    @PreAuthorize("hasAuthority('mall:product:tag:query') or hasRole('super_admin')")
    public AjaxResult<MallProductTagTypeAdminVo> getById(@PathVariable Long id) {
        return success(mallProductTagTypeService.getTypeById(id));
    }

    /**
     * 新增商品标签类型。
     *
     * @param request 新增请求
     * @return 新增结果
     */
    @PostMapping
    @Operation(summary = "新增商品标签类型")
    @PreAuthorize("hasAuthority('mall:product:tag:add') or hasRole('super_admin')")
    public AjaxResult<Void> add(@Validated @RequestBody MallProductTagTypeAddRequest request) {
        return toAjax(mallProductTagTypeService.addType(request));
    }

    /**
     * 修改商品标签类型。
     *
     * @param request 修改请求
     * @return 修改结果
     */
    @PutMapping
    @Operation(summary = "修改商品标签类型")
    @PreAuthorize("hasAuthority('mall:product:tag:edit') or hasRole('super_admin')")
    public AjaxResult<Void> update(@Validated @RequestBody MallProductTagTypeUpdateRequest request) {
        return toAjax(mallProductTagTypeService.updateType(request));
    }

    /**
     * 修改商品标签类型状态。
     *
     * @param request 状态请求
     * @return 修改结果
     */
    @PutMapping("/status")
    @Operation(summary = "修改商品标签类型状态")
    @PreAuthorize("hasAuthority('mall:product:tag:edit') or hasRole('super_admin')")
    public AjaxResult<Void> updateStatus(@Validated @RequestBody MallProductTagTypeStatusUpdateRequest request) {
        return toAjax(mallProductTagTypeService.updateTypeStatus(request));
    }

    /**
     * 删除商品标签类型。
     *
     * @param id 标签类型ID
     * @return 删除结果
     */
    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "删除商品标签类型")
    @PreAuthorize("hasAuthority('mall:product:tag:delete') or hasRole('super_admin')")
    public AjaxResult<Void> delete(@PathVariable Long id) {
        return toAjax(mallProductTagTypeService.deleteType(id));
    }
}
