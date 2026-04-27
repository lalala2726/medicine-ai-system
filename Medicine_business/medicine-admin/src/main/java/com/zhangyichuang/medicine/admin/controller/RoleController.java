package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.admin.model.request.RoleAddRequest;
import com.zhangyichuang.medicine.admin.model.request.RoleListRequest;
import com.zhangyichuang.medicine.admin.model.request.RolePermissionUpdateRequest;
import com.zhangyichuang.medicine.admin.model.request.RoleUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.RoleListVo;
import com.zhangyichuang.medicine.admin.model.vo.RolePermissionVo;
import com.zhangyichuang.medicine.admin.model.vo.RoleVo;
import com.zhangyichuang.medicine.admin.service.PermissionService;
import com.zhangyichuang.medicine.admin.service.RoleService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.Option;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
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
 * 角色管理控制器。
 * <p>
 * 提供角色的分页查询、详情查询、增删改及角色授权能力。
 * </p>
 */
@RestController
@RequestMapping("/system/role")
@Tag(name = "角色管理", description = "角色管理")
@RequiredArgsConstructor
@PreventDuplicateSubmit
public class RoleController extends BaseController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    /**
     * 分页查询角色列表。
     *
     * @param request 查询条件与分页参数
     * @return 角色分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "角色列表")
    @PreAuthorize("hasAuthority('system:role:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> roleList(RoleListRequest request) {
        var rolePage = roleService.roleList(request);
        var vo = copyListProperties(rolePage, RoleListVo.class);
        return getTableData(rolePage, vo);
    }

    /**
     * 查询角色下拉选项。
     *
     * @return 角色选项列表
     */
    @GetMapping("/option")
    @Operation(summary = "角色选项")
    @PreAuthorize("hasAuthority('system:role:list') or hasRole('super_admin')")
    public AjaxResult<List<Option<Long>>> roleOption() {
        return success(roleService.roleOption());
    }

    /**
     * 查询角色详情。
     *
     * @param id 角色ID
     * @return 角色详情
     */
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "角色详情")
    @PreAuthorize("hasAuthority('system:role:query') or hasRole('super_admin')")
    public AjaxResult<RoleVo> getRoleById(@PathVariable Long id) {
        var role = roleService.getRoleById(id);
        var roleVo = copyProperties(role, RoleVo.class);
        return success(roleVo);
    }

    /**
     * 新增角色。
     *
     * @param request 角色新增参数
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "添加角色")
    @PreAuthorize("hasAuthority('system:role:add') or hasRole('super_admin')")
    @OperationLog(module = "角色管理", action = "新增角色", type = OperationType.ADD)
    public AjaxResult<Void> postRole(@Validated @RequestBody RoleAddRequest request) {
        return toAjax(roleService.addRole(request));
    }

    /**
     * 修改角色。
     *
     * @param request 角色修改参数
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改角色")
    @PreAuthorize("hasAuthority('system:role:update') or hasRole('super_admin')")
    @OperationLog(module = "角色管理", action = "修改角色", type = OperationType.UPDATE)
    public AjaxResult<Void> putRole(@Validated @RequestBody RoleUpdateRequest request) {
        return toAjax(roleService.updateRoleById(request));
    }

    /**
     * 批量删除角色。
     *
     * @param ids 角色ID集合
     * @return 操作结果
     */
    @DeleteMapping("/{ids:\\d+(?:,\\d+)*}")
    @Operation(summary = "删除角色")
    @PreAuthorize("hasAuthority('system:role:delete') or hasRole('super_admin')")
    @OperationLog(module = "角色管理", action = "删除角色", type = OperationType.DELETE)
    public AjaxResult<Void> deleteRole(@PathVariable List<Long> ids) {
        return toAjax(roleService.deleteRoleByIds(ids));
    }

    /**
     * 查询角色已授权权限。
     *
     * @param id 角色ID
     * @return 角色权限信息（已选权限 + 可选权限树）
     */
    @GetMapping("/permission/{id}")
    @Operation(summary = "获取角色权限")
    @PreAuthorize("hasAuthority('system:role:query') or hasRole('super_admin')")
    public AjaxResult<RolePermissionVo> getRolePermission(@PathVariable Long id) {
        List<Long> rolePermission = roleService.getRolePermission(id);
        List<Option<Long>> options = permissionService.permissionOption();
        return success(new RolePermissionVo(options, rolePermission));
    }

    /**
     * 更新角色授权。
     *
     * @param request 授权参数
     * @return 操作结果
     */
    @PutMapping("/permission")
    @Operation(summary = "更新角色权限")
    @PreAuthorize("hasAuthority('system:role:update') or hasRole('super_admin')")
    @OperationLog(module = "角色管理", action = "更新角色权限", type = OperationType.UPDATE)
    public AjaxResult<Void> updateRolePermission(@Validated @RequestBody RolePermissionUpdateRequest request) {
        return toAjax(roleService.updateRolePermission(request));
    }
}
