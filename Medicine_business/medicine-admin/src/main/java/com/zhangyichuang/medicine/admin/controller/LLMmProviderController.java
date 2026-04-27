package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.facade.LlmProviderFacade;
import com.zhangyichuang.medicine.admin.model.dto.LlmPresetProviderTemplateDto;
import com.zhangyichuang.medicine.admin.model.dto.LlmProviderDetailDto;
import com.zhangyichuang.medicine.admin.model.dto.LlmProviderListDto;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.*;
import com.zhangyichuang.medicine.admin.service.LlmProviderModelService;
import com.zhangyichuang.medicine.admin.service.LlmProviderService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.entity.LlmProviderModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 大模型提供商与模型一体化接口。
 */
@RestController
@RequestMapping("/llm/provider")
@RequiredArgsConstructor
@Validated
@Tag(name = "大模型提供商管理", description = "大模型提供商与模型一体化接口")
public class LLMmProviderController extends BaseController {

    private final LlmProviderService llmProviderService;
    private final LlmProviderModelService llmProviderModelService;
    private final LlmProviderFacade llmProviderFacade;

    /**
     * 查询预设提供商列表。
     *
     * @return 预设提供商列表
     */
    @GetMapping("/preset/list")
    @Operation(summary = "预设提供商列表")
    @PreAuthorize("hasAuthority('system:llm_provider:list') or hasRole('super_admin')")
    public AjaxResult<List<LlmPresetProviderVo>> listPresetProviders() {
        List<LlmPresetProviderTemplateDto> templates = llmProviderService.listPresetProviders();
        return success(toPresetProviderVoList(templates));
    }

    /**
     * 查询预设提供商详情。
     *
     * @param providerKey 预设提供商英文键
     * @return 预设提供商详情
     */
    @GetMapping("/preset/{providerKey}")
    @Operation(summary = "预设提供商详情")
    @PreAuthorize("hasAuthority('system:llm_provider:query') or hasRole('super_admin')")
    public AjaxResult<LlmPresetProviderDetailVo> getPresetProvider(@PathVariable String providerKey) {
        LlmPresetProviderTemplateDto template = llmProviderService.getPresetProvider(providerKey);
        return success(toPresetProviderDetailVo(template));
    }

    /**
     * 分页查询提供商列表。
     *
     * @param request 查询参数
     * @return 提供商分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "提供商分页列表")
    @PreAuthorize("hasAuthority('system:llm_provider:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listProviders(LlmProviderListRequest request) {
        Page<LlmProviderListDto> page = llmProviderService.listProviders(request);
        List<LlmProviderListVo> rows = copyListProperties(page, LlmProviderListVo.class);
        return getTableData(page, rows);
    }

    /**
     * 查询提供商详情。
     *
     * @param id 提供商ID
     * @return 提供商详情
     */
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "提供商详情")
    @PreAuthorize("hasAuthority('system:llm_provider:query') or hasRole('super_admin')")
    public AjaxResult<LlmProviderDetailVo> getProviderDetail(@PathVariable Long id) {
        LlmProviderDetailDto detailDto = llmProviderFacade.getProviderDetail(id);
        return success(toProviderDetailVo(detailDto));
    }

    /**
     * 新增提供商。
     *
     * @param request 新增请求
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增提供商")
    @PreAuthorize("hasAuthority('system:llm_provider:add') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    public AjaxResult<Void> createProvider(@Valid @RequestBody LlmProviderCreateRequest request) {
        return toAjax(llmProviderFacade.createProvider(request));
    }

    /**
     * 测试 OpenAI 兼容提供商连通性。
     *
     * @param request 测试请求
     * @return 测试结果
     */
    @PostMapping("/connectivity-test")
    @Operation(summary = "测试提供商连通性")
    @PreAuthorize("hasAuthority('system:llm_provider:test') or hasRole('super_admin')")
    public AjaxResult<LlmProviderConnectivityTestVo> testConnectivity(
            @Valid @RequestBody LlmProviderConnectivityTestRequest request) {
        return success(llmProviderService.testConnectivity(request));
    }

    /**
     * 编辑提供商。
     *
     * @param request 编辑请求
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "编辑提供商")
    @PreAuthorize("hasAuthority('system:llm_provider:update') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    public AjaxResult<Void> updateProvider(@Valid @RequestBody LlmProviderUpdateRequest request) {
        return toAjax(llmProviderFacade.updateProvider(request));
    }

    /**
     * 更新提供商状态。
     *
     * @param request 状态修改请求
     * @return 操作结果
     */
    @PutMapping("/status")
    @Operation(summary = "更新提供商状态")
    @PreAuthorize("hasAuthority('system:llm_provider:update') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    public AjaxResult<Void> updateProviderStatus(@Valid @RequestBody LlmProviderUpdateStatusRequest request) {
        return toAjax(llmProviderService.updateProviderStatus(request));
    }

    /**
     * 更新提供商 API Key。
     *
     * @param request API Key 修改请求
     * @return 操作结果
     */
    @PutMapping("/api-key")
    @Operation(summary = "更新提供商API Key")
    @PreAuthorize("hasAuthority('system:llm_provider:update') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    public AjaxResult<Void> updateProviderApiKey(@Valid @RequestBody LlmProviderApiKeyUpdateRequest request) {
        return toAjax(llmProviderService.updateProviderApiKey(request));
    }

    /**
     * 删除提供商。
     *
     * @param id 提供商ID
     * @return 操作结果
     */
    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "删除提供商")
    @PreAuthorize("hasAuthority('system:llm_provider:delete') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    public AjaxResult<Void> deleteProvider(@PathVariable Long id) {
        return toAjax(llmProviderFacade.deleteProvider(id));
    }

    /**
     * 查询提供商模型列表。
     *
     * @param providerId 提供商ID
     * @return 模型列表
     */
    @GetMapping("/{providerId:\\d+}/model/list")
    @Operation(summary = "查询提供商模型列表")
    @PreAuthorize("hasAuthority('system:llm_provider_model:list') or hasRole('super_admin')")
    public AjaxResult<List<LlmProviderModelVo>> listProviderModels(@PathVariable Long providerId) {
        List<LlmProviderModel> models = llmProviderFacade.listProviderModels(providerId);
        return success(copyListProperties(models, LlmProviderModelVo.class));
    }

    /**
     * 新增单个模型。
     *
     * @param request 新增模型请求
     * @return 操作结果
     */
    @PostMapping("/model")
    @Operation(summary = "新增单个模型")
    @PreAuthorize("hasAuthority('system:llm_provider_model:add') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    public AjaxResult<Void> createProviderModel(@Valid @RequestBody LlmProviderModelCreateRequest request) {
        return toAjax(llmProviderFacade.createProviderModel(request));
    }

    /**
     * 编辑单个模型。
     *
     * @param request 编辑模型请求
     * @return 操作结果
     */
    @PutMapping("/model")
    @Operation(summary = "编辑单个模型")
    @PreAuthorize("hasAuthority('system:llm_provider_model:update') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    public AjaxResult<Void> updateProviderModel(@Valid @RequestBody LlmProviderModelUpdateRequest request) {
        return toAjax(llmProviderFacade.updateProviderModel(request));
    }

    /**
     * 删除单个模型。
     *
     * @param id 模型ID
     * @return 操作结果
     */
    @DeleteMapping("/model/{id:\\d+}")
    @Operation(summary = "删除单个模型")
    @PreAuthorize("hasAuthority('system:llm_provider_model:delete') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    public AjaxResult<Void> deleteProviderModel(@PathVariable Long id) {
        return toAjax(llmProviderModelService.deleteProviderModel(id));
    }

    /**
     * 将预设模板列表转换为摘要视图列表。
     *
     * @param templates 预设模板列表
     * @return 预设摘要视图列表
     */
    private List<LlmPresetProviderVo> toPresetProviderVoList(List<LlmPresetProviderTemplateDto> templates) {
        if (templates == null || templates.isEmpty()) {
            return List.of();
        }
        return templates.stream()
                .filter(Objects::nonNull)
                .map(template -> copyProperties(template, LlmPresetProviderVo.class))
                .toList();
    }

    /**
     * 将预设模板详情转换为详情视图对象。
     *
     * @param template 预设模板
     * @return 预设详情视图
     */
    private LlmPresetProviderDetailVo toPresetProviderDetailVo(LlmPresetProviderTemplateDto template) {
        LlmPresetProviderDetailVo vo = copyProperties(template, LlmPresetProviderDetailVo.class);
        vo.setModels(toPresetModelVoList(template.getModels()));
        return vo;
    }

    /**
     * 将提供商详情 DTO 转换为详情视图对象。
     *
     * @param detailDto 提供商详情 DTO
     * @return 提供商详情视图
     */
    private LlmProviderDetailVo toProviderDetailVo(LlmProviderDetailDto detailDto) {
        LlmProviderDetailVo vo = copyProperties(detailDto, LlmProviderDetailVo.class);
        vo.setModels(copyListProperties(detailDto.getModels(), LlmProviderModelVo.class));
        return vo;
    }

    /**
     * 将预设模型模板转换为模型视图对象列表。
     *
     * @param models 预设模型模板列表
     * @return 模型视图对象列表
     */
    private List<LlmPresetProviderModelVo> toPresetModelVoList(List<LlmPresetProviderTemplateDto.Model> models) {
        if (models == null || models.isEmpty()) {
            return List.of();
        }
        return models.stream()
                .filter(Objects::nonNull)
                .map(model -> copyProperties(model, LlmPresetProviderModelVo.class))
                .toList();
    }
}
