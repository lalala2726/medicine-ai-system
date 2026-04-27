package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderModelCreateRequest;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderModelItemRequest;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderModelUpdateRequest;
import com.zhangyichuang.medicine.model.entity.LlmProvider;
import com.zhangyichuang.medicine.model.entity.LlmProviderModel;

import java.util.List;

/**
 * 大模型提供商模型服务。
 *
 * @author Chuang
 */
public interface LlmProviderModelService extends IService<LlmProviderModel> {

    /**
     * 查询指定提供商下的全部模型。
     *
     * @param providerId 提供商ID
     * @return 模型列表
     */
    List<LlmProviderModel> listProviderModels(Long providerId);

    /**
     * 新增单个模型。
     *
     * @param provider 提供商实体
     * @param request  新增模型请求
     * @return 是否新增成功
     */
    boolean createProviderModel(LlmProvider provider, LlmProviderModelCreateRequest request);

    /**
     * 编辑单个模型。
     *
     * @param provider 提供商实体
     * @param request  编辑模型请求
     * @return 是否编辑成功
     */
    boolean updateProviderModel(LlmProvider provider, LlmProviderModelUpdateRequest request);

    /**
     * 删除单个模型。
     *
     * @param id 模型ID
     * @return 是否删除成功
     */
    boolean deleteProviderModel(Long id);

    /**
     * 批量保存提供商模型。
     *
     * @param provider 提供商实体
     * @param requests 模型请求列表
     * @param operator 操作人
     * @return 是否保存成功
     */
    boolean saveProviderModels(LlmProvider provider, List<LlmProviderModelItemRequest> requests, String operator);

}
