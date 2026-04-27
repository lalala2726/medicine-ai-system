package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.admin.model.request.PermissionAddRequest;
import com.zhangyichuang.medicine.admin.model.request.PermissionUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.PermissionTreeVo;
import com.zhangyichuang.medicine.admin.model.vo.PermissionVo;
import com.zhangyichuang.medicine.admin.service.PermissionService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.Option;
import com.zhangyichuang.medicine.common.log.annotation.OperationLog;
import com.zhangyichuang.medicine.common.log.enums.OperationType;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器。
 * <p>
 * 提供权限树查询、详情查询、增删改与权限选项查询能力。
 * </p>
 */
@RestController
@RequestMapping("/system/permission")
@Tag(name = "权限管理", description = "权限管理")
@RequiredArgsConstructor
@PreventDuplicateSubmit
public class PermissionController extends BaseController {

    private final PermissionService permissionService;

    /**
     * 查询权限树。
     *
     * @return 权限树列表
     */
    @GetMapping("/list")
    @Operation(summary = "权限列表")
    @PreAuthorize("hasAuthority('system:permission:list') or hasRole('super_admin')")
    public AjaxResult<List<PermissionTreeVo>> permissionTree() {
        return success(permissionService.permissionTree());
    }

    /**
     * 查询权限详情。
     *
     * @param id 权限ID
     * @return 权限详情
     */
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "权限详情")
    @PreAuthorize("hasAuthority('system:permission:query') or hasRole('super_admin')")
    public AjaxResult<PermissionVo> getPermissionById(@PathVariable Long id) {
        var permission = permissionService.getPermissionById(id);
        var permissionVo = copyProperties(permission, PermissionVo.class);
        return success(permissionVo);
    }

    /**
     * 新增权限。
     *
     * @param request 权限新增参数
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "添加权限")
    @PreAuthorize("hasAuthority('system:permission:add') or hasRole('super_admin')")
    @OperationLog(module = "权限管理", action = "新增权限", type = OperationType.ADD)
    public AjaxResult<Void> addPermission(@Validated @RequestBody PermissionAddRequest request) {
        return toAjax(permissionService.addPermission(request));
    }

    /**
     * 查询权限下拉选项。
     *
     * @return 权限选项树
     */
    @GetMapping("/option")
    @Operation(summary = "权限选项")
    @PreAuthorize("hasAuthority('system:permission:list') or hasRole('super_admin')")
    public AjaxResult<List<Option<Long>>> permissionOption() {
        return success(permissionService.permissionOption());
    }

    /**
     * 修改权限。
     *
     * @param request 权限修改参数
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改权限")
    @PreAuthorize("hasAuthority('system:permission:update') or hasRole('super_admin')")
    @OperationLog(module = "权限管理", action = "修改权限", type = OperationType.UPDATE)
    public AjaxResult<Void> updatePermission(@Validated @RequestBody PermissionUpdateRequest request) {
        return toAjax(permissionService.updatePermissionById(request));
    }

    /**
     * 批量删除权限。
     *
     * @param ids 权限ID集合
     * @return 操作结果
     */
    @DeleteMapping("/{ids:\\d+(?:,\\d+)*}")
    @Operation(summary = "删除权限")
    @PreAuthorize("hasAuthority('system:permission:delete') or hasRole('super_admin')")
    @OperationLog(module = "权限管理", action = "删除权限", type = OperationType.DELETE)
    public AjaxResult<Void> deletePermission(@PathVariable List<Long> ids) {
        return toAjax(permissionService.deletePermissionByIds(ids));
    }
}
