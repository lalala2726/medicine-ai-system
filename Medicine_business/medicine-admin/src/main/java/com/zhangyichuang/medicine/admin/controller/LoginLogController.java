package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.admin.model.request.LoginLogQueryRequest;
import com.zhangyichuang.medicine.admin.model.vo.LoginLogListVo;
import com.zhangyichuang.medicine.admin.model.vo.LoginLogVo;
import com.zhangyichuang.medicine.admin.service.LoginLogService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.log.annotation.OperationLog;
import com.zhangyichuang.medicine.common.log.enums.OperationType;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统登录日志控制器。
 * <p>
 * 提供登录日志分页查询、详情查询与清空能力。
 * </p>
 */
@RestController
@RequestMapping("/system/login_log")
@RequiredArgsConstructor
@Tag(name = "登录日志", description = "提供登录日志查询与清空接口")
@PreventDuplicateSubmit
public class LoginLogController extends BaseController {

    private final LoginLogService loginLogService;

    /**
     * 分页查询登录日志列表。
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "登录日志列表")
    @PreAuthorize("hasAuthority('system:login_log:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> logList(LoginLogQueryRequest request) {
        var page = loginLogService.logList(request);
        var rows = copyListProperties(page, LoginLogListVo.class);
        return getTableData(page, rows);
    }

    /**
     * 根据日志ID查询详情。
     *
     * @param id 日志ID
     * @return 日志详情
     */
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "登录日志详情")
    @PreAuthorize("hasAuthority('system:login_log:query') or hasRole('super_admin')")
    public AjaxResult<LoginLogVo> getLogById(@PathVariable Long id) {
        var log = loginLogService.getLogById(id);
        var vo = copyProperties(log, LoginLogVo.class);
        return success(vo);
    }

    /**
     * 清空登录日志。
     *
     * @return 操作结果
     */
    @DeleteMapping
    @Operation(summary = "清空登录日志")
    @PreAuthorize("hasAuthority('system:login_log:delete') or hasRole('super_admin')")
    @OperationLog(module = "登录日志", action = "清空登录日志", type = OperationType.DELETE)
    public AjaxResult<Void> clearLog() {
        boolean result = loginLogService.clearLogs();
        return toAjax(result);
    }
}
