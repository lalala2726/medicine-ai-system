package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.admin.model.dto.LlmPresetProviderTemplateDto;
import com.zhangyichuang.medicine.admin.model.dto.LlmProviderListDto;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.LlmProviderConnectivityTestVo;
import com.zhangyichuang.medicine.model.entity.LlmProvider;

import java.util.List;

/**
 * 大模型提供商服务。
 *
 * @author Chuang
 */
public interface LlmProviderService extends IService<LlmProvider> {

    /**
     * 查询全部预设模型厂商模板。
     *
     * @return 预设模型厂商模板列表
     */
    List<LlmPresetProviderTemplateDto> listPresetProviders();

    /**
     * 根据厂商英文键查询预设模型厂商模板。
     *
     * @param providerKey 预设厂商英文键
     * @return 预设模型厂商模板详情
     */
    LlmPresetProviderTemplateDto getPresetProvider(String providerKey);

    /**
     * 分页查询提供商列表。
     *
     * @param request 查询参数
     * @return 提供商分页结果
     */
    Page<LlmProviderListDto> listProviders(LlmProviderListRequest request);

    /**
     * 测试 OpenAI 兼容提供商连通性。
     *
     * @param request 测试请求
     * @return 测试结果
     */
    LlmProviderConnectivityTestVo testConnectivity(LlmProviderConnectivityTestRequest request);

    /**
     * 查询必填提供商实体，不存在时抛出业务异常。
     *
     * @param id 提供商ID
     * @return 提供商实体
     */
    LlmProvider getRequiredProvider(Long id);

    /**
     * 新增提供商配置。
     *
     * @param request 新增请求参数
     * @return 新增后的提供商实体
     */
    LlmProvider createProvider(LlmProviderCreateRequest request);

    /**
     * 编辑提供商配置。
     *
     * @param request 编辑请求参数
     * @return 编辑后的提供商实体
     */
    LlmProvider updateProvider(LlmProviderUpdateRequest request);

    /**
     * 更新提供商状态。
     *
     * @param request 状态修改请求
     * @return 是否更新成功
     */
    boolean updateProviderStatus(LlmProviderUpdateStatusRequest request);

    /**
     * 更新提供商 API Key。
     *
     * @param request API Key 修改请求
     * @return 是否更新成功
     */
    boolean updateProviderApiKey(LlmProviderApiKeyUpdateRequest request);

    /**
     * 删除提供商配置。
     *
     * @param id 提供商ID
     * @return 是否删除成功
     */
    boolean deleteProvider(Long id);
}
