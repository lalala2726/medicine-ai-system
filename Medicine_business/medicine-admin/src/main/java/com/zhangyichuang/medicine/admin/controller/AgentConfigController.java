package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.*;
import com.zhangyichuang.medicine.admin.service.AgentConfigService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 配置管理控制器。
 */
@RestController
@RequestMapping("/agent/config")
@Validated
@Tag(name = "Agent配置管理", description = "管理端 Agent 配置接口")
@RequiredArgsConstructor
@PreventDuplicateSubmit
public class AgentConfigController extends BaseController {

    /**
     * 模型选项查询权限表达式，允许系统模型配置三个二级页共用模型下拉数据。
     */
    private static final String MODEL_OPTION_PERMISSION =
            "hasAuthority('system:agent_config:admin:query') "
                    + "or hasAuthority('system:agent_config:client:query') "
                    + "or hasAuthority('system:agent_config:common:query') "
                    + "or hasRole('super_admin')";

    private final AgentConfigService agentConfigService;

    /**
     * 查询知识库 Agent 配置。
     *
     * @return 知识库 Agent 配置
     */
    @GetMapping("/knowledge-base")
    @Operation(summary = "知识库Agent配置详情")
    @PreAuthorize("hasAuthority('system:agent_config:admin:query') or hasRole('super_admin')")
    public AjaxResult<KnowledgeBaseAgentConfigVo> getKnowledgeBaseConfig() {
        return success(agentConfigService.getKnowledgeBaseConfig());
    }

    /**
     * 保存知识库 Agent 配置。
     *
     * @param request 知识库 Agent 配置请求
     * @return 操作结果
     */
    @PutMapping("/knowledge-base")
    @Operation(summary = "保存知识库Agent配置")
    @PreAuthorize("hasAuthority('system:agent_config:admin:update') or hasRole('super_admin')")
    public AjaxResult<Void> saveKnowledgeBaseConfig(@Valid @RequestBody KnowledgeBaseAgentConfigRequest request) {
        return toAjax(agentConfigService.saveKnowledgeBaseConfig(request));
    }

    /**
     * 查询知识库选项。
     *
     * @return 知识库选项列表
     */
    @GetMapping("/knowledge-base/option")
    @Operation(summary = "知识库选项")
    @PreAuthorize("hasAuthority('system:agent_config:admin:query') or hasRole('super_admin')")
    public AjaxResult<List<KnowledgeBaseOptionVo>> listKnowledgeBaseOptions() {
        return success(agentConfigService.listKnowledgeBaseOptions());
    }

    /**
     * 查询客户端知识库 Agent 配置。
     *
     * @return 客户端知识库 Agent 配置
     */
    @GetMapping("/client-knowledge-base")
    @Operation(summary = "客户端知识库Agent配置详情")
    @PreAuthorize("hasAuthority('system:agent_config:client:query') or hasRole('super_admin')")
    public AjaxResult<KnowledgeBaseAgentConfigVo> getClientKnowledgeBaseConfig() {
        return success(agentConfigService.getClientKnowledgeBaseConfig());
    }

    /**
     * 保存客户端知识库 Agent 配置。
     *
     * @param request 客户端知识库 Agent 配置请求
     * @return 操作结果
     */
    @PutMapping("/client-knowledge-base")
    @Operation(summary = "保存客户端知识库Agent配置")
    @PreAuthorize("hasAuthority('system:agent_config:client:update') or hasRole('super_admin')")
    public AjaxResult<Void> saveClientKnowledgeBaseConfig(
            @Valid @RequestBody KnowledgeBaseAgentConfigRequest request) {
        return toAjax(agentConfigService.saveClientKnowledgeBaseConfig(request));
    }

    /**
     * 查询客户端知识库选项。
     *
     * @return 客户端知识库选项列表
     */
    @GetMapping("/client-knowledge-base/option")
    @Operation(summary = "客户端知识库选项")
    @PreAuthorize("hasAuthority('system:agent_config:client:query') or hasRole('super_admin')")
    public AjaxResult<List<KnowledgeBaseOptionVo>> listClientKnowledgeBaseOptions() {
        return success(agentConfigService.listClientKnowledgeBaseOptions());
    }

    /**
     * 查询管理端助手 Agent 配置。
     *
     * @return 管理端助手 Agent 配置
     */
    @GetMapping("/admin-assistant")
    @Operation(summary = "管理端助手Agent配置详情")
    @PreAuthorize("hasAuthority('system:agent_config:admin:query') or hasRole('super_admin')")
    public AjaxResult<AdminAssistantAgentConfigVo> getAdminAssistantConfig() {
        return success(agentConfigService.getAdminAssistantConfig());
    }

    /**
     * 查询客户端助手 Agent 配置。
     *
     * @return 客户端助手 Agent 配置
     */
    @GetMapping("/client-assistant")
    @Operation(summary = "客户端助手Agent配置详情")
    @PreAuthorize("hasAuthority('system:agent_config:client:query') or hasRole('super_admin')")
    public AjaxResult<ClientAssistantAgentConfigVo> getClientAssistantConfig() {
        return success(agentConfigService.getClientAssistantConfig());
    }

    /**
     * 保存管理端助手 Agent 配置。
     *
     * @param request 管理端助手 Agent 配置请求
     * @return 操作结果
     */
    @PutMapping("/admin-assistant")
    @Operation(summary = "保存管理端助手Agent配置")
    @PreAuthorize("hasAuthority('system:agent_config:admin:update') or hasRole('super_admin')")
    public AjaxResult<Void> saveAdminAssistantConfig(@Valid @RequestBody AdminAssistantAgentConfigRequest request) {
        return toAjax(agentConfigService.saveAdminAssistantConfig(request));
    }

    /**
     * 保存客户端助手 Agent 配置。
     *
     * @param request 客户端助手 Agent 配置请求
     * @return 操作结果
     */
    @PutMapping("/client-assistant")
    @Operation(summary = "保存客户端助手Agent配置")
    @PreAuthorize("hasAuthority('system:agent_config:client:update') or hasRole('super_admin')")
    public AjaxResult<Void> saveClientAssistantConfig(@Valid @RequestBody ClientAssistantAgentConfigRequest request) {
        return toAjax(agentConfigService.saveClientAssistantConfig(request));
    }

    /**
     * 查询通用能力 Agent 配置。
     *
     * @return 通用能力 Agent 配置
     */
    @GetMapping("/common-capability")
    @Operation(summary = "通用能力Agent配置详情")
    @PreAuthorize("hasAuthority('system:agent_config:common:query') or hasRole('super_admin')")
    public AjaxResult<CommonCapabilityAgentConfigVo> getCommonCapabilityConfig() {
        return success(agentConfigService.getCommonCapabilityConfig());
    }

    /**
     * 查询豆包语音 Agent 配置。
     *
     * @return 豆包语音 Agent 配置
     */
    @GetMapping("/speech")
    @Operation(summary = "豆包语音Agent配置详情")
    @PreAuthorize("hasAuthority('system:agent_config:common:query') or hasRole('super_admin')")
    public AjaxResult<SpeechAgentConfigVo> getSpeechConfig() {
        return success(agentConfigService.getSpeechConfig());
    }

    /**
     * 保存通用能力 Agent 配置。
     *
     * @param request 通用能力 Agent 配置请求
     * @return 操作结果
     */
    @PutMapping("/common-capability")
    @Operation(summary = "保存通用能力Agent配置")
    @PreAuthorize("hasAuthority('system:agent_config:common:update') or hasRole('super_admin')")
    public AjaxResult<Void> saveCommonCapabilityConfig(
            @Valid @RequestBody CommonCapabilityAgentConfigRequest request) {
        return toAjax(agentConfigService.saveCommonCapabilityConfig(request));
    }

    /**
     * 保存豆包语音 Agent 配置。
     *
     * @param request 豆包语音 Agent 配置请求
     * @return 操作结果
     */
    @PutMapping("/speech")
    @Operation(summary = "保存豆包语音Agent配置")
    @PreAuthorize("hasAuthority('system:agent_config:common:update') or hasRole('super_admin')")
    public AjaxResult<Void> saveSpeechConfig(@Valid @RequestBody SpeechAgentConfigRequest request) {
        return toAjax(agentConfigService.saveSpeechConfig(request));
    }

    /**
     * 查询向量模型选项。
     *
     * @return 向量模型选项列表
     */
    @GetMapping("/embedding-model/option")
    @Operation(summary = "向量模型选项")
    @PreAuthorize(MODEL_OPTION_PERMISSION)
    public AjaxResult<List<AgentModelOptionVo>> listEmbeddingModelOptions() {
        return success(agentConfigService.listEmbeddingModelOptions());
    }

    /**
     * 查询聊天模型选项。
     *
     * @return 聊天模型选项列表
     */
    @GetMapping("/chat-model/option")
    @Operation(summary = "聊天模型选项")
    @PreAuthorize(MODEL_OPTION_PERMISSION)
    public AjaxResult<List<AgentModelOptionVo>> listChatModelOptions() {
        return success(agentConfigService.listChatModelOptions());
    }

    /**
     * 查询重排模型选项。
     *
     * @return 重排模型选项列表
     */
    @GetMapping("/rerank-model/option")
    @Operation(summary = "重排模型选项")
    @PreAuthorize(MODEL_OPTION_PERMISSION)
    public AjaxResult<List<AgentModelOptionVo>> listRerankModelOptions() {
        return success(agentConfigService.listRerankModelOptions());
    }

    /**
     * 查询图片理解模型选项。
     *
     * @return 图片理解模型选项列表
     */
    @GetMapping("/vision-model/option")
    @Operation(summary = "图片理解模型选项")
    @PreAuthorize(MODEL_OPTION_PERMISSION)
    public AjaxResult<List<AgentModelOptionVo>> listVisionModelOptions() {
        return success(agentConfigService.listVisionModelOptions());
    }
}
