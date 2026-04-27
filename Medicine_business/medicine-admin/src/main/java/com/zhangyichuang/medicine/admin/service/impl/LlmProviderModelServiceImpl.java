package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.LlmProviderModelMapper;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderModelCreateRequest;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderModelItemRequest;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderModelUpdateRequest;
import com.zhangyichuang.medicine.admin.service.AgentConfigRuntimeSyncService;
import com.zhangyichuang.medicine.admin.service.LlmProviderModelService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.constants.LlmModelTypeConstants;
import com.zhangyichuang.medicine.model.entity.LlmProvider;
import com.zhangyichuang.medicine.model.entity.LlmProviderModel;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 大模型提供商模型服务实现。
 */
@Service
@RequiredArgsConstructor
public class LlmProviderModelServiceImpl extends ServiceImpl<LlmProviderModelMapper, LlmProviderModel>
        implements LlmProviderModelService, BaseService {

    private static final String DEFAULT_OPERATOR = "system";
    private static final int MODEL_STATUS_ENABLED = 0;
    private static final int MODEL_STATUS_DISABLED = 1;
    private static final int CAPABILITY_DISABLED = 0;
    private static final int CAPABILITY_ENABLED = 1;

    private final LlmProviderModelMapper llmProviderModelMapper;
    private final AgentConfigRuntimeSyncService agentConfigRuntimeSyncService;

    /**
     * 查询指定提供商下的全部模型。
     *
     * @param providerId 提供商ID
     * @return 模型列表
     */
    @Override
    public List<LlmProviderModel> listProviderModels(Long providerId) {
        return llmProviderModelMapper.selectList(Wrappers.<LlmProviderModel>lambdaQuery()
                .eq(LlmProviderModel::getProviderId, providerId)
                .orderByAsc(LlmProviderModel::getSort, LlmProviderModel::getId));
    }

    /**
     * 新增单个模型。
     *
     * @param provider 提供商实体
     * @param request  新增模型请求
     * @return 是否新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createProviderModel(LlmProvider provider, LlmProviderModelCreateRequest request) {
        Assert.notNull(provider, "提供商信息不能为空");
        Assert.notNull(request, "模型信息不能为空");

        String modelName = resolveModelName(request.getModelName(), null);
        String modelType = resolveModelType(request.getModelType(), null);
        validateUniqueModel(provider.getId(), modelName, modelType, null);

        LlmProviderModel model = buildCreateModelEntity(provider, request, modelName, modelType, currentOperator());
        try {
            return llmProviderModelMapper.insert(model) > 0;
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "同一提供商下模型名称和类型不能重复");
        }
    }

    /**
     * 编辑单个模型。
     *
     * @param provider 提供商实体
     * @param request  编辑模型请求
     * @return 是否编辑成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProviderModel(LlmProvider provider, LlmProviderModelUpdateRequest request) {
        Assert.notNull(provider, "提供商信息不能为空");
        Assert.notNull(request, "模型信息不能为空");

        LlmProviderModel existing = getRequiredProviderModel(request.getId());
        String modelName = resolveModelName(request.getModelName(), existing.getModelName());
        String modelType = resolveModelType(request.getModelType(), existing.getModelType());
        validateUniqueModel(provider.getId(), modelName, modelType, existing.getId());

        String operator = currentOperator();
        LlmProviderModel model = buildUpdateModelEntity(existing, provider, request, modelName, modelType, operator);
        try {
            boolean updated = llmProviderModelMapper.updateById(model) > 0;
            if (updated) {
                agentConfigRuntimeSyncService.syncAfterModelUpdate(existing, model, operator);
            }
            return updated;
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "同一提供商下模型名称和类型不能重复");
        }
    }

    /**
     * 删除单个模型。
     *
     * @param id 模型ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProviderModel(Long id) {
        LlmProviderModel existing = getRequiredProviderModel(id);
        boolean deleted = llmProviderModelMapper.deleteById(id) > 0;
        if (deleted) {
            agentConfigRuntimeSyncService.syncAfterModelDelete(existing, currentOperator());
        }
        return deleted;
    }

    /**
     * 批量保存提供商模型。
     *
     * @param provider 提供商实体
     * @param requests 模型请求列表
     * @param operator 操作人
     * @return 是否保存成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveProviderModels(LlmProvider provider, List<LlmProviderModelItemRequest> requests, String operator) {
        Assert.notNull(provider, "提供商信息不能为空");
        List<LlmProviderModel> models = buildModelEntities(provider, requests, resolveOperator(operator));
        validateDuplicateModels(models);
        try {
            return saveBatch(models);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "同一提供商下模型名称和类型不能重复");
        }
    }

    /**
     * 构建待保存的模型实体列表。
     *
     * @param provider 提供商实体
     * @param requests 模型请求列表
     * @param operator 操作人
     * @return 模型实体列表
     */
    private List<LlmProviderModel> buildModelEntities(LlmProvider provider,
                                                      List<LlmProviderModelItemRequest> requests,
                                                      String operator) {
        List<LlmProviderModel> models = new ArrayList<>(requests.size());
        for (LlmProviderModelItemRequest request : requests) {
            String modelName = resolveModelName(request.getModelName(), null);
            String modelType = resolveModelType(request.getModelType(), null);
            models.add(buildCreateModelEntity(provider, request, modelName, modelType, operator));
        }
        return models;
    }

    /**
     * 构建新增模型实体。
     *
     * @param provider  提供商实体
     * @param request   模型请求
     * @param modelName 模型实际名称
     * @param modelType 模型类型
     * @param operator  操作人
     * @return 模型实体
     */
    private LlmProviderModel buildCreateModelEntity(LlmProvider provider,
                                                    LlmProviderModelItemRequest request,
                                                    String modelName,
                                                    String modelType,
                                                    String operator) {
        return LlmProviderModel.builder()
                .providerId(provider.getId())
                .modelName(modelName)
                .modelType(modelType)
                .supportReasoning(resolveCapability(request.getSupportReasoning(), null, "是否支持深度思考值不合法"))
                .supportVision(resolveCapability(request.getSupportVision(), null, "是否支持图片识别值不合法"))
                .description(normalizeNullableText(request.getDescription()))
                .enabled(resolveModelStatus(request.getEnabled(), MODEL_STATUS_ENABLED))
                .sort(defaultSort(request.getSort()))
                .createBy(operator)
                .updateBy(operator)
                .build();
    }

    /**
     * 构建编辑模型实体。
     *
     * @param existing  已存在模型
     * @param provider  提供商实体
     * @param request   模型请求
     * @param modelName 模型实际名称
     * @param modelType 模型类型
     * @param operator  操作人
     * @return 模型实体
     */
    private LlmProviderModel buildUpdateModelEntity(LlmProviderModel existing,
                                                    LlmProvider provider,
                                                    LlmProviderModelUpdateRequest request,
                                                    String modelName,
                                                    String modelType,
                                                    String operator) {
        return LlmProviderModel.builder()
                .id(existing.getId())
                .providerId(provider.getId())
                .modelName(modelName)
                .modelType(modelType)
                .supportReasoning(resolveCapability(request.getSupportReasoning(), existing.getSupportReasoning(),
                        "是否支持深度思考值不合法"))
                .supportVision(resolveCapability(request.getSupportVision(), existing.getSupportVision(),
                        "是否支持图片识别值不合法"))
                .description(request.getDescription() == null ? existing.getDescription()
                        : normalizeNullableText(request.getDescription()))
                .enabled(resolveModelStatus(request.getEnabled(), existing.getEnabled()))
                .sort(request.getSort() == null ? defaultSort(existing.getSort()) : defaultSort(request.getSort()))
                .updateBy(operator)
                .build();
    }

    /**
     * 校验单次请求中的模型名称和类型不能重复。
     *
     * @param models 模型实体列表
     */
    private void validateDuplicateModels(List<LlmProviderModel> models) {
        Set<String> uniqueKeys = new LinkedHashSet<>();
        for (LlmProviderModel model : models) {
            String uniqueKey = model.getModelName() + "#" + model.getModelType();
            Assert.isParamTrue(uniqueKeys.add(uniqueKey), "模型名称和模型类型不能重复");
        }
    }

    /**
     * 校验数据库中模型唯一性。
     *
     * @param providerId 提供商ID
     * @param modelName  模型实际名称
     * @param modelType  模型类型
     * @param excludeId  排除的模型ID
     */
    private void validateUniqueModel(Long providerId, String modelName, String modelType, Long excludeId) {
        Long count = llmProviderModelMapper.selectCount(Wrappers.<LlmProviderModel>lambdaQuery()
                .eq(LlmProviderModel::getProviderId, providerId)
                .eq(LlmProviderModel::getModelName, modelName)
                .eq(LlmProviderModel::getModelType, modelType)
                .ne(excludeId != null, LlmProviderModel::getId, excludeId));
        if (count != null && count > 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "同一提供商下模型名称和类型不能重复");
        }
    }

    /**
     * 查询必填模型实体。
     *
     * @param id 模型ID
     * @return 模型实体
     */
    private LlmProviderModel getRequiredProviderModel(Long id) {
        LlmProviderModel model = llmProviderModelMapper.selectById(id);
        if (model == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "模型不存在");
        }
        return model;
    }

    /**
     * 解析模型名称。
     *
     * @param modelName         请求中的模型名称
     * @param fallbackModelName 回退模型名称
     * @return 最终模型名称
     */
    private String resolveModelName(String modelName, String fallbackModelName) {
        String resolvedModelName = normalizeNullableText(modelName, fallbackModelName);
        Assert.notEmpty(resolvedModelName, "模型名称不能为空");
        return resolvedModelName;
    }

    /**
     * 解析模型类型。
     *
     * @param modelType         请求中的模型类型
     * @param fallbackModelType 回退模型类型
     * @return 最终模型类型
     */
    private String resolveModelType(String modelType, String fallbackModelType) {
        return normalizeModelType(normalizeNullableText(modelType, fallbackModelType));
    }

    /**
     * 校验并返回模型类型。
     *
     * @param modelType 原始模型类型
     * @return 归一化后的模型类型
     */
    private String normalizeModelType(String modelType) {
        String normalizedModelType = normalizeUpperCaseNullableText(modelType);
        Assert.notEmpty(normalizedModelType, "模型类型不能为空");
        Assert.isParamTrue(LlmModelTypeConstants.ALL.contains(normalizedModelType), "模型类型不合法");
        return normalizedModelType;
    }

    /**
     * 解析模型能力开关。
     *
     * @param capability   请求值
     * @param fallback     回退值
     * @param errorMessage 非法值错误提示
     * @return 最终能力值
     */
    private Integer resolveCapability(Integer capability, Integer fallback, String errorMessage) {
        Integer resolvedCapability = capability == null ? fallback : capability;
        if (resolvedCapability == null) {
            return CAPABILITY_DISABLED;
        }
        Assert.isParamTrue(resolvedCapability == CAPABILITY_DISABLED || resolvedCapability == CAPABILITY_ENABLED, errorMessage);
        return resolvedCapability;
    }

    /**
     * 解析模型启停状态。
     *
     * @param status        请求中的状态
     * @param defaultStatus 默认状态
     * @return 最终状态值
     */
    private Integer resolveModelStatus(Integer status, Integer defaultStatus) {
        if (status == null) {
            return defaultStatus == null ? MODEL_STATUS_ENABLED : defaultStatus;
        }
        Assert.isParamTrue(status == MODEL_STATUS_ENABLED || status == MODEL_STATUS_DISABLED, "模型状态值不合法");
        return status;
    }

    /**
     * 获取默认排序值。
     *
     * @param sort 排序值
     * @return 最终排序值
     */
    private Integer defaultSort(Integer sort) {
        return sort == null ? 0 : sort;
    }

    /**
     * 解析最终操作人。
     *
     * @param operator 显式传入的操作人
     * @return 操作人账号
     */
    private String resolveOperator(String operator) {
        String normalizedOperator = normalizeNullableText(operator);
        return normalizedOperator != null ? normalizedOperator : currentOperator();
    }

    /**
     * 获取当前操作人。
     *
     * @return 操作人账号
     */
    private String currentOperator() {
        try {
            String username = getUsername();
            return StringUtils.hasText(username) ? username : DEFAULT_OPERATOR;
        } catch (RuntimeException ex) {
            return DEFAULT_OPERATOR;
        }
    }

    /**
     * 归一化可空文本。
     *
     * @param value 原始文本
     * @return 归一化后的文本
     */
    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * 归一化可空文本并提供默认值。
     *
     * @param value        原始文本
     * @param defaultValue 默认值
     * @return 归一化后的文本
     */
    private String normalizeNullableText(String value, String defaultValue) {
        String normalized = normalizeNullableText(value);
        return normalized != null ? normalized : normalizeNullableText(defaultValue);
    }

    /**
     * 归一化可空大写文本。
     *
     * @param value 原始文本
     * @return 归一化后的大写文本
     */
    private String normalizeUpperCaseNullableText(String value) {
        String normalized = normalizeNullableText(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
