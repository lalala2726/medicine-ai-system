package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.service.MallProductTagService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.request.MallProductTagAddRequest;
import com.zhangyichuang.medicine.model.request.MallProductTagListQueryRequest;
import com.zhangyichuang.medicine.model.request.MallProductTagStatusUpdateRequest;
import com.zhangyichuang.medicine.model.request.MallProductTagUpdateRequest;
import com.zhangyichuang.medicine.model.vo.MallProductTagAdminVo;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品标签管理控制器。
 *
 * @author Chuang
 */
@RestController
@RequestMapping("/mall/product/tag")
@Validated
@RequiredArgsConstructor
@Tag(name = "商品标签管理", description = "提供商品标签的增删改查与状态管理")
@PreventDuplicateSubmit
public class MallProductTagController extends BaseController {

    /**
     * 商品标签服务。
     */
    private final MallProductTagService mallProductTagService;

    /**
     * 查询商品标签列表。
     *
     * @param request 查询参数
     * @return 标签分页列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询商品标签列表")
    @PreAuthorize("hasAuthority('mall:product:tag:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> list(MallProductTagListQueryRequest request) {
        Page<MallProductTagAdminVo> page = mallProductTagService.listTags(request);
        return getTableData(page, page.getRecords());
    }

    /**
     * 查询商品标签下拉列表。
     *
     * @param typeCode 标签类型编码
     * @return 启用标签列表
     */
    @GetMapping("/option")
    @Operation(summary = "查询商品标签下拉列表")
    @PreAuthorize("hasAuthority('mall:product:tag:list') or hasRole('super_admin')")
    public AjaxResult<List<MallProductTagVo>> option(@RequestParam(value = "typeCode", required = false) String typeCode) {
        return success(mallProductTagService.option(typeCode));
    }

    /**
     * 查询商品标签详情。
     *
     * @param id 标签ID
     * @return 标签详情
     */
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "查询商品标签详情")
    @PreAuthorize("hasAuthority('mall:product:tag:query') or hasRole('super_admin')")
    public AjaxResult<MallProductTagAdminVo> getById(@PathVariable Long id) {
        return success(mallProductTagService.getTagById(id));
    }

    /**
     * 新增商品标签。
     *
     * @param request 新增请求
     * @return 新增结果
     */
    @PostMapping
    @Operation(summary = "新增商品标签")
    @PreAuthorize("hasAuthority('mall:product:tag:add') or hasRole('super_admin')")
    public AjaxResult<Void> add(@Validated @RequestBody MallProductTagAddRequest request) {
        return toAjax(mallProductTagService.addTag(request));
    }

    /**
     * 修改商品标签。
     *
     * @param request 修改请求
     * @return 修改结果
     */
    @PutMapping
    @Operation(summary = "修改商品标签")
    @PreAuthorize("hasAuthority('mall:product:tag:edit') or hasRole('super_admin')")
    public AjaxResult<Void> update(@Validated @RequestBody MallProductTagUpdateRequest request) {
        return toAjax(mallProductTagService.updateTag(request));
    }

    /**
     * 修改商品标签状态。
     *
     * @param request 状态请求
     * @return 修改结果
     */
    @PutMapping("/status")
    @Operation(summary = "修改商品标签状态")
    @PreAuthorize("hasAuthority('mall:product:tag:edit') or hasRole('super_admin')")
    public AjaxResult<Void> updateStatus(@Validated @RequestBody MallProductTagStatusUpdateRequest request) {
        return toAjax(mallProductTagService.updateTagStatus(request));
    }

    /**
     * 删除商品标签。
     *
     * @param id 标签ID
     * @return 删除结果
     */
    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "删除商品标签")
    @PreAuthorize("hasAuthority('mall:product:tag:delete') or hasRole('super_admin')")
    public AjaxResult<Void> delete(@PathVariable Long id) {
        return toAjax(mallProductTagService.deleteTag(id));
    }
}
