package com.zhangyichuang.medicine.admin.facade.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhangyichuang.medicine.admin.facade.LlmProviderFacade;
import com.zhangyichuang.medicine.admin.model.dto.LlmProviderDetailDto;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderCreateRequest;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderModelCreateRequest;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderModelUpdateRequest;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderUpdateRequest;
import com.zhangyichuang.medicine.admin.service.AgentConfigRuntimeSyncService;
import com.zhangyichuang.medicine.admin.service.LlmProviderModelService;
import com.zhangyichuang.medicine.admin.service.LlmProviderService;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.model.entity.LlmProvider;
import com.zhangyichuang.medicine.model.entity.LlmProviderModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 大模型提供商聚合门面实现。
 */
@Service
@RequiredArgsConstructor
public class LlmProviderFacadeImpl implements LlmProviderFacade {

    private final AgentConfigRuntimeSyncService agentConfigRuntimeSyncService;
    private final LlmProviderService llmProviderService;
    private final LlmProviderModelService llmProviderModelService;

    /**
     * 查询提供商详情以及关联模型列表。
     *
     * @param id 提供商ID
     * @return 提供商详情
     */
    @Override
    public LlmProviderDetailDto getProviderDetail(Long id) {
        LlmProvider provider = llmProviderService.getRequiredProvider(id);
        List<LlmProviderModel> models = llmProviderModelService.listProviderModels(id);
        LlmProviderDetailDto detailDto = BeanCotyUtils.copyProperties(provider, LlmProviderDetailDto.class);
        detailDto.setModels(models);
        return detailDto;
    }

    /**
     * 新增提供商以及其关联模型。
     *
     * @param request 新增请求
     * @return 是否新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createProvider(LlmProviderCreateRequest request) {
        LlmProvider provider = llmProviderService.createProvider(request);
        boolean saved = llmProviderModelService.saveProviderModels(provider, request.getModels(), provider.getCreateBy());
        if (saved) {
            agentConfigRuntimeSyncService.syncActiveProviderSnapshot(provider, provider.getCreateBy());
        }
        return saved;
    }

    /**
     * 编辑提供商以及其关联模型。
     *
     * @param request 编辑请求
     * @return 是否编辑成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProvider(LlmProviderUpdateRequest request) {
        LlmProvider provider = llmProviderService.updateProvider(request);
        llmProviderModelService.remove(Wrappers.<LlmProviderModel>lambdaQuery()
                .eq(LlmProviderModel::getProviderId, provider.getId()));
        boolean saved = llmProviderModelService.saveProviderModels(provider, request.getModels(), provider.getUpdateBy());
        if (saved) {
            agentConfigRuntimeSyncService.syncActiveProviderSnapshot(provider, provider.getUpdateBy());
        }
        return saved;
    }

    /**
     * 删除提供商以及其关联模型。
     *
     * @param id 提供商ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProvider(Long id) {
        LlmProvider provider = llmProviderService.getRequiredProvider(id);
        agentConfigRuntimeSyncService.assertProviderCanDelete(provider);
        llmProviderModelService.remove(Wrappers.<LlmProviderModel>lambdaQuery()
                .eq(LlmProviderModel::getProviderId, id));
        return llmProviderService.deleteProvider(id);
    }

    /**
     * 查询指定提供商下的全部模型。
     *
     * @param providerId 提供商ID
     * @return 模型列表
     */
    @Override
    public List<LlmProviderModel> listProviderModels(Long providerId) {
        llmProviderService.getRequiredProvider(providerId);
        return llmProviderModelService.listProviderModels(providerId);
    }

    /**
     * 新增单个模型。
     *
     * @param request 新增模型请求
     * @return 是否新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createProviderModel(LlmProviderModelCreateRequest request) {
        LlmProvider provider = llmProviderService.getRequiredProvider(request.getProviderId());
        return llmProviderModelService.createProviderModel(provider, request);
    }

    /**
     * 编辑单个模型。
     *
     * @param request 编辑模型请求
     * @return 是否编辑成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProviderModel(LlmProviderModelUpdateRequest request) {
        LlmProvider provider = llmProviderService.getRequiredProvider(request.getProviderId());
        return llmProviderModelService.updateProviderModel(provider, request);
    }
}
