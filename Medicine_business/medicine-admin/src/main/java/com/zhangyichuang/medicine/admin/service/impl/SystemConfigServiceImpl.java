package com.zhangyichuang.medicine.admin.service.impl;

import com.zhangyichuang.medicine.admin.model.request.AgreementConfigUpdateRequest;
import com.zhangyichuang.medicine.admin.model.request.SecurityConfigUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.AgreementConfigVo;
import com.zhangyichuang.medicine.admin.model.vo.EsIndexConfigVo;
import com.zhangyichuang.medicine.admin.model.vo.SecurityConfigVo;
import com.zhangyichuang.medicine.admin.service.SystemConfigService;
import com.zhangyichuang.medicine.admin.task.MallProductIndexRebuildCoordinator;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.agreement.AgreementConfig;
import com.zhangyichuang.medicine.common.security.agreement.AgreementConfigService;
import com.zhangyichuang.medicine.common.security.login.AdminWatermarkConfig;
import com.zhangyichuang.medicine.common.security.login.LoginSecurityConfig;
import com.zhangyichuang.medicine.common.security.login.LoginSecurityPolicyConfig;
import com.zhangyichuang.medicine.common.security.login.LoginSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 系统配置服务实现。
 */
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final AgreementConfigService agreementConfigService;

    private final LoginSecurityService loginSecurityService;

    private final MallProductIndexRebuildCoordinator mallProductIndexRebuildCoordinator;

    /**
     * 查询安全配置。
     *
     * @return 安全配置详情
     */
    @Override
    public SecurityConfigVo getSecurityConfig() {
        LoginSecurityConfig config = loginSecurityService.getSecurityConfig();
        SecurityConfigVo vo = new SecurityConfigVo();
        vo.setAdmin(toPolicyVo(config.getAdmin()));
        vo.setClient(toPolicyVo(config.getClient()));
        vo.setAdminWatermark(toAdminWatermarkVo(config.getAdminWatermark()));
        return vo;
    }

    /**
     * 查询管理端水印配置。
     *
     * @return 管理端水印配置详情
     */
    @Override
    public SecurityConfigVo.AdminWatermarkConfigVo getAdminWatermarkConfig() {
        return toAdminWatermarkVo(loginSecurityService.getAdminWatermarkConfig());
    }

    /**
     * 查询软件协议配置。
     *
     * @return 软件协议配置详情
     */
    @Override
    public AgreementConfigVo getAgreementConfig() {
        AgreementConfig config = agreementConfigService.getAgreementConfig();
        return toAgreementConfigVo(config);
    }

    /**
     * 查询 Elasticsearch 与商品索引概览。
     *
     * @return Elasticsearch 与商品索引概览
     */
    @Override
    public EsIndexConfigVo getEsIndexConfig() {
        return mallProductIndexRebuildCoordinator.getEsIndexConfig();
    }

    /**
     * 更新安全配置。
     *
     * @param request 安全配置更新请求
     * @return 是否更新成功
     */
    @Override
    public boolean updateSecurityConfig(SecurityConfigUpdateRequest request) {
        Assert.notNull(request, "安全配置更新请求不能为空");
        Assert.notNull(request.getAdmin(), "管理端安全策略不能为空");
        Assert.notNull(request.getClient(), "客户端安全策略不能为空");

        LoginSecurityConfig config = new LoginSecurityConfig();
        config.setAdmin(toPolicyConfig(request.getAdmin()));
        config.setClient(toPolicyConfig(request.getClient()));
        config.setAdminWatermark(toAdminWatermarkConfig(request.getAdminWatermark()));
        loginSecurityService.updateSecurityConfig(config);
        return true;
    }

    /**
     * 更新软件协议配置。
     *
     * @param request 软件协议配置更新请求
     * @return 是否更新成功
     */
    @Override
    public boolean updateAgreementConfig(AgreementConfigUpdateRequest request) {
        Assert.notNull(request, "软件协议配置更新请求不能为空");
        agreementConfigService.updateAgreementConfig(toAgreementConfig(request));
        return true;
    }

    /**
     * 手动触发商品索引全量重建。
     *
     * @return true 表示已成功提交重建任务
     */
    @Override
    public boolean triggerEsIndexRebuild() {
        return mallProductIndexRebuildCoordinator.triggerManualRebuild();
    }

    /**
     * 将策略配置映射为前端视图对象。
     *
     * @param policyConfig 登录策略配置
     * @return 安全策略视图对象
     */
    private SecurityConfigVo.SecurityPolicyVo toPolicyVo(LoginSecurityPolicyConfig policyConfig) {
        SecurityConfigVo.SecurityPolicyVo vo = new SecurityConfigVo.SecurityPolicyVo();
        vo.setMaxRetryCount(policyConfig.getMaxRetryCount());
        vo.setLockMinutes(policyConfig.getLockMinutes());
        return vo;
    }

    /**
     * 将更新请求映射为登录策略配置。
     *
     * @param requestPolicy 单端安全策略更新请求
     * @return 登录策略配置
     */
    private LoginSecurityPolicyConfig toPolicyConfig(SecurityConfigUpdateRequest.SecurityPolicyUpdateRequest requestPolicy) {
        LoginSecurityPolicyConfig config = new LoginSecurityPolicyConfig();
        config.setMaxRetryCount(requestPolicy.getMaxRetryCount());
        config.setLockMinutes(requestPolicy.getLockMinutes());
        return config;
    }

    /**
     * 将管理端水印配置映射为前端视图对象。
     *
     * @param config 管理端水印配置
     * @return 管理端水印配置视图对象
     */
    private SecurityConfigVo.AdminWatermarkConfigVo toAdminWatermarkVo(AdminWatermarkConfig config) {
        SecurityConfigVo.AdminWatermarkConfigVo vo = new SecurityConfigVo.AdminWatermarkConfigVo();
        if (config == null) {
            vo.setEnabled(false);
            vo.setShowUsername(true);
            vo.setShowUserId(true);
            return vo;
        }
        vo.setEnabled(config.getEnabled());
        vo.setShowUsername(config.getShowUsername());
        vo.setShowUserId(config.getShowUserId());
        return vo;
    }

    /**
     * 将更新请求映射为管理端水印配置。
     *
     * @param request 水印配置更新请求
     * @return 管理端水印配置
     */
    private AdminWatermarkConfig toAdminWatermarkConfig(
            SecurityConfigUpdateRequest.AdminWatermarkConfigUpdateRequest request
    ) {
        Assert.notNull(request, "管理端水印配置不能为空");
        AdminWatermarkConfig config = new AdminWatermarkConfig();
        config.setEnabled(request.getEnabled());
        config.setShowUsername(request.getShowUsername());
        config.setShowUserId(request.getShowUserId());
        return config;
    }

    /**
     * 将软件协议配置映射为前端视图对象。
     *
     * @param config 软件协议配置
     * @return 软件协议视图对象
     */
    private AgreementConfigVo toAgreementConfigVo(AgreementConfig config) {
        AgreementConfigVo vo = new AgreementConfigVo();
        vo.setSoftwareAgreementMarkdown(config.getSoftwareAgreementMarkdown());
        vo.setPrivacyAgreementMarkdown(config.getPrivacyAgreementMarkdown());
        vo.setUpdatedTime(config.getUpdatedTime());
        return vo;
    }

    /**
     * 将更新请求映射为软件协议配置。
     *
     * @param request 软件协议配置更新请求
     * @return 软件协议配置
     */
    private AgreementConfig toAgreementConfig(AgreementConfigUpdateRequest request) {
        AgreementConfig config = new AgreementConfig();
        config.setSoftwareAgreementMarkdown(request.getSoftwareAgreementMarkdown());
        config.setPrivacyAgreementMarkdown(request.getPrivacyAgreementMarkdown());
        return config;
    }
}
