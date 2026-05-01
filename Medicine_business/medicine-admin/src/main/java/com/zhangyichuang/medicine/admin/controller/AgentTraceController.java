package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.AgentTraceRunListRequest;
import com.zhangyichuang.medicine.admin.model.vo.AgentTraceDetailVo;
import com.zhangyichuang.medicine.admin.model.vo.AgentTraceRunListVo;
import com.zhangyichuang.medicine.admin.service.AgentTraceService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.log.annotation.OperationLog;
import com.zhangyichuang.medicine.common.log.enums.OperationType;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Agent Trace 管理控制器。
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/agent/trace")
@Tag(name = "Agent Trace管理", description = "管理端智能体运行跟踪查询接口")
public class AgentTraceController extends BaseController {

    /**
     * Agent Trace 管理服务。
     */
    private final AgentTraceService agentTraceService;

    /**
     * 分页查询 Agent Trace 运行列表。
     *
     * @param request 查询参数。
     * @return Trace 运行分页列表。
     */
    @GetMapping("/list")
    @Operation(summary = "Agent Trace运行列表")
    @PreAuthorize("hasAuthority('system:agent_trace:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listRuns(AgentTraceRunListRequest request) {
        Page<AgentTraceRunListVo> page = agentTraceService.listRuns(request);
        return getTableData(page, page.getRecords());
    }

    /**
     * 查询 Agent Trace 详情。
     *
     * @param traceId Trace 唯一标识。
     * @return Trace 详情。
     */
    @GetMapping("/{traceId}")
    @Operation(summary = "Agent Trace详情")
    @PreAuthorize("hasAuthority('system:agent_trace:query') or hasRole('super_admin')")
    public AjaxResult<AgentTraceDetailVo> getTraceDetail(@PathVariable String traceId) {
        return success(agentTraceService.getTraceDetail(traceId));
    }

    /**
     * 删除 Agent Trace。
     *
     * @param traceId Trace 唯一标识。
     * @return 操作结果。
     */
    @DeleteMapping("/{traceId}")
    @Operation(summary = "删除Agent Trace")
    @PreAuthorize("hasAuthority('system:agent_trace:delete') or hasRole('super_admin')")
    @OperationLog(module = "Agent Trace管理", action = "删除Agent Trace", type = OperationType.DELETE)
    public AjaxResult<Void> deleteTrace(@PathVariable String traceId) {
        return toAjax(agentTraceService.deleteTrace(traceId));
    }
}
