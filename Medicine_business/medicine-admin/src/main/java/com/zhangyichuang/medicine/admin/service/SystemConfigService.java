package com.zhangyichuang.medicine.admin.service;

import com.zhangyichuang.medicine.admin.model.request.AgreementConfigUpdateRequest;
import com.zhangyichuang.medicine.admin.model.request.SecurityConfigUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.AgreementConfigVo;
import com.zhangyichuang.medicine.admin.model.vo.EsIndexConfigVo;
import com.zhangyichuang.medicine.admin.model.vo.SecurityConfigVo;

/**
 * 系统配置服务。
 */
public interface SystemConfigService {

    /**
     * 查询安全配置。
     *
     * @return 安全配置详情
     */
    SecurityConfigVo getSecurityConfig();

    /**
     * 查询管理端水印配置。
     *
     * @return 管理端水印配置详情
     */
    SecurityConfigVo.AdminWatermarkConfigVo getAdminWatermarkConfig();

    /**
     * 查询软件协议配置。
     *
     * @return 软件协议配置详情
     */
    AgreementConfigVo getAgreementConfig();

    /**
     * 查询 Elasticsearch 与商品索引概览。
     *
     * @return Elasticsearch 与商品索引概览
     */
    EsIndexConfigVo getEsIndexConfig();

    /**
     * 更新安全配置。
     *
     * @param request 安全配置更新请求
     * @return 是否更新成功
     */
    boolean updateSecurityConfig(SecurityConfigUpdateRequest request);

    /**
     * 更新软件协议配置。
     *
     * @param request 软件协议配置更新请求
     * @return 是否更新成功
     */
    boolean updateAgreementConfig(AgreementConfigUpdateRequest request);

    /**
     * 手动触发商品索引全量重建。
     *
     * @return true 表示已成功提交重建任务
     */
    boolean triggerEsIndexRebuild();
}
