package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.admin.model.request.AgentPromptKeyUpsertRequest;
import com.zhangyichuang.medicine.admin.model.request.AgentPromptRollbackRequest;
import com.zhangyichuang.medicine.admin.model.request.AgentPromptSyncRequest;
import com.zhangyichuang.medicine.admin.model.request.AgentPromptUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.AgentPromptConfigVo;
import com.zhangyichuang.medicine.admin.model.vo.AgentPromptHistoryVo;
import com.zhangyichuang.medicine.admin.model.vo.AgentPromptKeyOptionVo;
import com.zhangyichuang.medicine.admin.service.AgentPromptConfigService;
import com.zhangyichuang.medicine.admin.service.AgentPromptSyncTaskService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 提示词配置管理控制器。
 */
@RestController
@RequestMapping("/agent/config")
@Validated
@Tag(name = "Agent提示词配置管理", description = "管理端 Agent 提示词配置接口")
@RequiredArgsConstructor
@PreventDuplicateSubmit
public class AgentPromptConfigController extends BaseController {

    /**
     * Agent 提示词配置服务。
     */
    private final AgentPromptConfigService agentPromptConfigService;

    /**
     * Agent 提示词同步任务服务。
     */
    private final AgentPromptSyncTaskService agentPromptSyncTaskService;

    /**
     * 查询提示词键选项列表。
     *
     * @return 提示词键选项列表
     */
    @GetMapping("/prompt/keys")
    @Operation(summary = "提示词键选项列表")
    @PreAuthorize("hasAuthority('system:agent_prompt:list') or hasRole('super_admin')")
    public AjaxResult<List<AgentPromptKeyOptionVo>> listPromptKeys() {
        return success(agentPromptConfigService.listPromptKeys());
    }

    /**
     * 新增或更新提示词业务键。
     *
     * @param request 业务键新增/更新请求
     * @return 操作结果
     */
    @PutMapping("/prompt/key")
    @Operation(summary = "新增或更新提示词业务键")
    @PreAuthorize("hasAuthority('system:agent_prompt:update') or hasRole('super_admin')")
    public AjaxResult<Void> savePromptKey(@Valid @RequestBody AgentPromptKeyUpsertRequest request) {
        return toAjax(agentPromptConfigService.savePromptKey(request));
    }

    /**
     * 查询指定提示词当前配置。
     *
     * @param promptKey 提示词业务键
     * @return 提示词当前配置
     */
    @GetMapping("/prompt")
    @Operation(summary = "提示词配置详情")
    @PreAuthorize("hasAuthority('system:agent_prompt:query') or hasRole('super_admin')")
    public AjaxResult<AgentPromptConfigVo> getPromptConfig(@RequestParam("promptKey") String promptKey) {
        return success(agentPromptConfigService.getPromptConfig(promptKey));
    }

    /**
     * 保存提示词配置。
     *
     * @param request 提示词保存请求
     * @return 操作结果
     */
    @PutMapping("/prompt")
    @Operation(summary = "保存提示词配置")
    @PreAuthorize("hasAuthority('system:agent_prompt:update') or hasRole('super_admin')")
    public AjaxResult<Void> savePromptConfig(@Valid @RequestBody AgentPromptUpdateRequest request) {
        return toAjax(agentPromptConfigService.savePromptConfig(request));
    }

    /**
     * 查询指定提示词历史版本列表。
     *
     * @param promptKey 提示词业务键
     * @param limit     返回条数上限
     * @return 历史版本列表
     */
    @GetMapping("/prompt/history")
    @Operation(summary = "提示词历史版本")
    @PreAuthorize("hasAuthority('system:agent_prompt:query') or hasRole('super_admin')")
    public AjaxResult<List<AgentPromptHistoryVo>> listPromptHistory(
            @RequestParam("promptKey") String promptKey,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return success(agentPromptConfigService.listPromptHistory(promptKey, limit));
    }

    /**
     * 将提示词回滚到指定历史版本。
     *
     * @param request 回滚请求
     * @return 操作结果
     */
    @PostMapping("/prompt/rollback")
    @Operation(summary = "回滚提示词历史版本")
    @PreAuthorize("hasAuthority('system:agent_prompt:rollback') or hasRole('super_admin')")
    public AjaxResult<Void> rollbackPromptConfig(@Valid @RequestBody AgentPromptRollbackRequest request) {
        return toAjax(agentPromptConfigService.rollbackPromptConfig(request));
    }

    /**
     * 提交全量提示词同步任务。
     *
     * @return 操作结果
     */
    @PostMapping("/prompt/sync/all")
    @Operation(summary = "提交全量提示词同步任务")
    @PreAuthorize("hasAuthority('system:agent_prompt:sync') or hasRole('super_admin')")
    public AjaxResult<Void> submitSyncAllPromptConfigs() {
        return toAjax(agentPromptSyncTaskService.submitSyncAllTask());
    }

    /**
     * 提交单条提示词同步任务。
     *
     * @param request 同步请求
     * @return 操作结果
     */
    @PostMapping("/prompt/sync")
    @Operation(summary = "提交单条提示词同步任务")
    @PreAuthorize("hasAuthority('system:agent_prompt:sync') or hasRole('super_admin')")
    public AjaxResult<Void> submitSyncPromptConfig(@Valid @RequestBody AgentPromptSyncRequest request) {
        return toAjax(agentPromptSyncTaskService.submitSyncSingleTask(request.getPromptKey()));
    }

    /**
     * 删除指定提示词当前配置与历史版本。
     *
     * @param promptKey             提示词业务键
     * @param captchaVerificationId 验证码校验凭证
     * @return 操作结果
     */
    @DeleteMapping("/prompt")
    @Operation(summary = "删除提示词配置与历史")
    @PreAuthorize("hasAuthority('system:agent_prompt:delete') or hasRole('super_admin')")
    public AjaxResult<Void> deletePromptConfig(
            @RequestParam("promptKey") @NotBlank(message = "提示词键不能为空") String promptKey,
            @RequestParam("captchaVerificationId") @NotBlank(message = "验证码校验凭证不能为空") String captchaVerificationId
    ) {
        return toAjax(agentPromptConfigService.deletePromptConfig(promptKey, captchaVerificationId));
    }
}
