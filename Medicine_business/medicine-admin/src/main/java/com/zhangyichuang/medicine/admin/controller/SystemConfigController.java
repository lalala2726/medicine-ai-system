package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.admin.model.request.AgreementConfigUpdateRequest;
import com.zhangyichuang.medicine.admin.model.request.SecurityConfigUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.AgreementConfigVo;
import com.zhangyichuang.medicine.admin.model.vo.EsIndexConfigVo;
import com.zhangyichuang.medicine.admin.model.vo.SecurityConfigVo;
import com.zhangyichuang.medicine.admin.service.SystemConfigService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
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

/**
 * 系统配置控制器。
 */
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "系统配置管理接口")
public class SystemConfigController extends BaseController {

    private final SystemConfigService systemConfigService;

    /**
     * 查询安全配置。
     *
     * @return 安全配置详情
     */
    @GetMapping("/security")
    @Operation(summary = "安全配置详情")
    @PreAuthorize("hasAuthority('system:security_config:query') or hasRole('super_admin')")
    public AjaxResult<SecurityConfigVo> getSecurityConfig() {
        return success(systemConfigService.getSecurityConfig());
    }

    /**
     * 查询管理端水印配置。
     *
     * @return 管理端水印配置详情
     */
    @GetMapping("/security/admin-watermark")
    @Operation(summary = "管理端水印配置详情")
    @PreAuthorize("hasAuthority('system:security_config:query') or hasRole('super_admin')")
    public AjaxResult<SecurityConfigVo.AdminWatermarkConfigVo> getAdminWatermarkConfig() {
        return success(systemConfigService.getAdminWatermarkConfig());
    }

    /**
     * 查询软件协议配置。
     *
     * @return 软件协议配置详情
     */
    @GetMapping("/agreement")
    @Operation(summary = "软件协议配置详情")
    @PreAuthorize("hasAuthority('system:agreement_config:query') or hasRole('super_admin')")
    public AjaxResult<AgreementConfigVo> getAgreementConfig() {
        return success(systemConfigService.getAgreementConfig());
    }

    /**
     * 查询 Elasticsearch 与商品索引概览。
     *
     * @return Elasticsearch 与商品索引概览
     */
    @GetMapping("/es-index")
    @Operation(summary = "Elasticsearch 与商品索引概览")
    @PreAuthorize("hasAuthority('system:es_index:query') or hasRole('super_admin')")
    public AjaxResult<EsIndexConfigVo> getEsIndexConfig() {
        return success(systemConfigService.getEsIndexConfig());
    }

    /**
     * 更新安全配置。
     *
     * @param request 安全配置更新请求
     * @return 操作结果
     */
    @PutMapping("/security")
    @Operation(summary = "更新安全配置")
    @PreAuthorize("hasAuthority('system:security_config:update') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    @OperationLog(module = "系统配置", action = "更新安全配置", type = OperationType.UPDATE)
    public AjaxResult<Void> updateSecurityConfig(@Validated @RequestBody SecurityConfigUpdateRequest request) {
        return toAjax(systemConfigService.updateSecurityConfig(request));
    }

    /**
     * 更新软件协议配置。
     *
     * @param request 软件协议配置更新请求
     * @return 操作结果
     */
    @PutMapping("/agreement")
    @Operation(summary = "更新软件协议配置")
    @PreAuthorize("hasAuthority('system:agreement_config:update') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    @OperationLog(module = "系统配置", action = "更新软件协议配置", type = OperationType.UPDATE)
    public AjaxResult<Void> updateAgreementConfig(@Validated @RequestBody AgreementConfigUpdateRequest request) {
        return toAjax(systemConfigService.updateAgreementConfig(request));
    }

    /**
     * 手动触发商品索引全量重建。
     *
     * @return true 表示已成功提交重建任务
     */
    @PostMapping("/es-index/rebuild")
    @Operation(summary = "手动触发商品索引全量重建")
    @PreAuthorize("hasAuthority('system:es_index:rebuild') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    @OperationLog(module = "系统配置", action = "手动触发商品索引重建", type = OperationType.OTHER)
    public AjaxResult<Boolean> triggerEsIndexRebuild() {
        return success(systemConfigService.triggerEsIndexRebuild());
    }
}
