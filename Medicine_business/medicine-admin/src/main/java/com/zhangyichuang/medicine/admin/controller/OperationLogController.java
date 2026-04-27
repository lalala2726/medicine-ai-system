package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.admin.model.request.OperationLogQueryRequest;
import com.zhangyichuang.medicine.admin.model.vo.OperationLogListVo;
import com.zhangyichuang.medicine.admin.model.vo.OperationLogVo;
import com.zhangyichuang.medicine.admin.service.OperationLogService;
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
 * 系统操作日志控制器。
 * <p>
 * 提供操作日志分页查询、详情查询与清空能力。
 * </p>
 */
@RestController
@RequestMapping("/system/operation_log")
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "提供操作日志查询与清空接口")
@PreventDuplicateSubmit
public class OperationLogController extends BaseController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志列表。
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "操作日志列表")
    @PreAuthorize("hasAuthority('system:operation-log:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> logList(OperationLogQueryRequest request) {
        var page = operationLogService.logList(request);
        var rows = copyListProperties(page, OperationLogListVo.class);
        return getTableData(page, rows);
    }

    /**
     * 根据日志ID查询详情。
     *
     * @param id 日志ID
     * @return 日志详情
     */
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "操作日志详情")
    @PreAuthorize("hasAuthority('system:operation-log:query') or hasRole('super_admin')")
    public AjaxResult<OperationLogVo> getLogById(@PathVariable Long id) {
        var log = operationLogService.getLogById(id);
        var vo = copyProperties(log, OperationLogVo.class);
        return success(vo);
    }

    /**
     * 清空操作日志。
     *
     * @return 操作结果
     */
    @DeleteMapping
    @Operation(summary = "清空操作日志")
    @PreAuthorize("hasAuthority('system:operation-log:delete') or hasRole('super_admin')")
    @OperationLog(module = "操作日志", action = "清空操作日志", type = OperationType.DELETE)
    public AjaxResult<Void> clearLog() {
        boolean result = operationLogService.clearLogs();
        return toAjax(result);
    }
}
