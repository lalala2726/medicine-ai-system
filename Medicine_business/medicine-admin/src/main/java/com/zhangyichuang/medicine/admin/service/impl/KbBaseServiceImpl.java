package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.integration.MedicineAgentClient;
import com.zhangyichuang.medicine.admin.mapper.KbBaseMapper;
import com.zhangyichuang.medicine.admin.model.dto.KnowledgeBaseListDto;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseAddRequest;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseListRequest;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseSearchRequest;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.KnowledgeBaseSearchHitVo;
import com.zhangyichuang.medicine.admin.model.vo.KnowledgeBaseSearchVo;
import com.zhangyichuang.medicine.admin.service.AgentConfigRuntimeSyncService;
import com.zhangyichuang.medicine.admin.service.KbBaseService;
import com.zhangyichuang.medicine.admin.service.LlmProviderModelService;
import com.zhangyichuang.medicine.admin.service.LlmProviderService;
import com.zhangyichuang.medicine.admin.support.KnowledgeBaseEmbeddingDimSupport;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.constants.LlmModelTypeConstants;
import com.zhangyichuang.medicine.model.entity.KbBase;
import com.zhangyichuang.medicine.model.entity.LlmProvider;
import com.zhangyichuang.medicine.model.entity.LlmProviderModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Chuang
 */
@Service
@RequiredArgsConstructor
public class KbBaseServiceImpl extends ServiceImpl<KbBaseMapper, KbBase>
        implements KbBaseService, BaseService {

    /**
     * 模型提供商启用状态：1。
     */
    private static final int PROVIDER_STATUS_ENABLED = 1;
    /**
     * 启用状态：0。
     */
    private static final int STATUS_ENABLED = 0;

    /**
     * 禁用状态：1。
     */
    private static final int STATUS_DISABLED = 1;
    /**
     * 模型启用状态：0。
     */
    private static final int MODEL_STATUS_ENABLED = 0;
    /**
     * 手动知识检索固定返回条数。
     */
    private static final int KNOWLEDGE_SEARCH_TOP_K = 10;
    /**
     * 当前没有启用模型提供商时的提示文案。
     */
    private static final String ENABLED_PROVIDER_MISSING_MESSAGE = "当前没有启用的模型提供商";

    /**
     * 向量模型不合法时的提示文案模板。
     */
    private static final String EMBEDDING_MODEL_INVALID_MESSAGE = "向量模型必须是当前激活提供商下的启用向量模型：%s";

    /**
     * 重排模型不合法时的提示文案模板。
     */
    private static final String RERANK_MODEL_INVALID_MESSAGE = "重排模型必须是当前激活提供商下的启用重排模型：%s";

    /**
     * 知识库名称重复时的提示文案模板。
     */
    private static final String KNOWLEDGE_SEARCH_DUPLICATE_MESSAGE = "知识库名称重复：%s";

    /**
     * 知识库不存在或未启用时的提示文案模板。
     */
    private static final String KNOWLEDGE_SEARCH_NOT_FOUND_MESSAGE = "知识库不存在或未启用：%s";

    /**
     * 向量模型不一致时的提示文案模板。
     */
    private static final String KNOWLEDGE_SEARCH_MODEL_MISMATCH_MESSAGE = "知识库向量模型必须与第一个知识库保持一致：%s";

    /**
     * 向量维度不一致时的提示文案模板。
     */
    private static final String KNOWLEDGE_SEARCH_DIM_MISMATCH_MESSAGE = "知识库向量维度必须与第一个知识库保持一致：%s";

    /**
     * Agent 返回未知知识库时的提示文案模板。
     */
    private static final String KNOWLEDGE_SEARCH_UNKNOWN_KB_MESSAGE = "知识检索返回了未知知识库：%s";

    private final MedicineAgentClient medicineAgentClient;
    private final AgentConfigRuntimeSyncService agentConfigRuntimeSyncService;
    private final LlmProviderService llmProviderService;
    private final LlmProviderModelService llmProviderModelService;

    @Override
    public Page<KnowledgeBaseListDto> listKnowledgeBase(KnowledgeBaseListRequest request) {
        Assert.notNull(request, "查询参数不能为空");
        Page<KnowledgeBaseListDto> page = request.toPage();
        return baseMapper.listKnowledgeBase(page, request);
    }

    @Override
    public KbBase getKnowledgeBaseById(Long id) {
        Assert.isPositive(id, "知识库ID必须大于0");
        KbBase kbBase = getById(id);
        Assert.isTrue(kbBase != null, "知识库不存在");
        return kbBase;
    }

    @Override
    public List<KbBase> listEnabledKnowledgeBases() {
        return lambdaQuery()
                .eq(KbBase::getStatus, STATUS_ENABLED)
                .orderByAsc(KbBase::getId)
                .list();
    }

    @Override
    public List<KbBase> listEnabledKnowledgeBasesByNames(List<String> knowledgeNames) {
        if (knowledgeNames == null || knowledgeNames.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .eq(KbBase::getStatus, STATUS_ENABLED)
                .in(KbBase::getKnowledgeName, knowledgeNames)
                .list();
    }

    @Override
    public KnowledgeBaseSearchVo searchKnowledgeBase(KnowledgeBaseSearchRequest request) {
        Assert.notNull(request, "知识库检索请求不能为空");
        String question = normalizeSearchQuestion(request.getQuestion());
        List<String> knowledgeNames = normalizeSearchKnowledgeNames(request.getKnowledgeNames());
        List<KbBase> knowledgeBases = loadSearchKnowledgeBases(knowledgeNames);
        validateSearchKnowledgeBasesAgainstBaseline(knowledgeBases);

        KbBase baseline = knowledgeBases.getFirst();
        String rankingModel = normalizeSearchRankingModel(request.getRankingModel());
        boolean rankingEnabled = StringUtils.hasText(rankingModel);
        MedicineAgentClient.KnowledgeSearchResponseData responseData = medicineAgentClient.searchKnowledge(
                question,
                knowledgeNames,
                baseline.getEmbeddingModel(),
                baseline.getEmbeddingDim(),
                rankingEnabled,
                rankingModel,
                KNOWLEDGE_SEARCH_TOP_K
        );
        Assert.notNull(responseData, "知识检索结果不能为空");
        return toKnowledgeBaseSearchVo(responseData, knowledgeBases);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addKnowledgeBase(KnowledgeBaseAddRequest request) {
        Assert.notNull(request, "知识库信息不能为空");
        Assert.notEmpty(request.getKnowledgeName(), "知识库名称不能为空");
        String embeddingModel = validateEmbeddingModel(request.getEmbeddingModel());
        validateEmbeddingDim(request.getEmbeddingDim());

        if (isKnowledgeNameExists(request.getKnowledgeName())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "知识库名称已存在");
        }

        medicineAgentClient.createKnowledgeBase(request.getKnowledgeName(), request.getEmbeddingDim(), request.getDescription());

        KbBase kbBase = copyProperties(request, KbBase.class);
        kbBase.setCover(normalizeCover(request.getCover()));
        kbBase.setEmbeddingModel(embeddingModel);
        kbBase.setCreateBy(getUsername());
        try {
            boolean saved = save(kbBase);
            if (!saved) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "创建知识库失败");
            }
            return true;
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "知识库名称已存在");
        }
    }

    @Override
    public boolean updateKnowledgeBase(KnowledgeBaseUpdateRequest request) {
        Assert.notNull(request, "知识库信息不能为空");
        Assert.isPositive(request.getId(), "知识库ID必须大于0");
        KbBase existingKbBase = getById(request.getId());
        Assert.isTrue(existingKbBase != null, "知识库不存在");
        applyStatusChangeIfNecessary(existingKbBase, request.getStatus());

        existingKbBase.setDisplayName(request.getDisplayName());
        existingKbBase.setCover(normalizeCover(request.getCover()));
        existingKbBase.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            existingKbBase.setStatus(request.getStatus());
        }
        existingKbBase.setUpdateBy(getUsername());
        existingKbBase.setUpdatedAt(new Date());
        return updateById(existingKbBase);
    }

    @Override
    public boolean deleteKnowledgeBase(Long id) {
        Assert.isPositive(id, "知识库ID必须大于0");
        KbBase kbBase = getKnowledgeBaseById(id);
        agentConfigRuntimeSyncService.assertKnowledgeBaseCanDelete(kbBase.getKnowledgeName());
        medicineAgentClient.deleteKnowledgeBase(kbBase.getKnowledgeName());
        return removeById(id);
    }

    /**
     * 判断业务知识库名称是否已被占用。
     *
     * @param knowledgeName 业务知识库名称
     * @return true: 已存在；false: 不存在
     */
    boolean isKnowledgeNameExists(String knowledgeName) {
        return lambdaQuery()
                .eq(KbBase::getKnowledgeName, knowledgeName)
                .count() > 0;
    }

    /**
     * 校验知识库向量维度是否属于支持集合。
     *
     * @param embeddingDim 向量维度
     */
    private void validateEmbeddingDim(Integer embeddingDim) {
        Assert.notNull(embeddingDim, "向量维度不能为空");
        Assert.isParamTrue(KnowledgeBaseEmbeddingDimSupport.isSupported(embeddingDim),
                KnowledgeBaseEmbeddingDimSupport.SUPPORTED_DIM_MESSAGE);
    }

    /**
     * 校验重排模型是否属于当前激活提供商的启用重排模型。
     *
     * @param rankingModel 重排模型名称
     * @return 归一化后的重排模型名称
     */
    private String validateRankingModel(String rankingModel) {
        Assert.notEmpty(rankingModel, "重排模型不能为空");
        String normalizedRankingModel = rankingModel.trim();
        LlmProvider provider = getRequiredEnabledProvider();
        List<LlmProviderModel> models = llmProviderModelService.lambdaQuery()
                .eq(LlmProviderModel::getProviderId, provider.getId())
                .eq(LlmProviderModel::getModelType, LlmModelTypeConstants.RERANK)
                .eq(LlmProviderModel::getModelName, normalizedRankingModel)
                .eq(LlmProviderModel::getEnabled, MODEL_STATUS_ENABLED)
                .orderByAsc(LlmProviderModel::getSort, LlmProviderModel::getId)
                .list();
        if (models.isEmpty()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    RERANK_MODEL_INVALID_MESSAGE.formatted(normalizedRankingModel));
        }
        return normalizedRankingModel;
    }

    /**
     * 校验知识库向量模型是否属于当前激活提供商的启用向量模型。
     *
     * @param embeddingModel 向量模型名称
     * @return 归一化后的向量模型名称
     */
    private String validateEmbeddingModel(String embeddingModel) {
        Assert.notEmpty(embeddingModel, "向量模型标识不能为空");
        String normalizedEmbeddingModel = embeddingModel.trim();
        LlmProvider provider = getRequiredEnabledProvider();
        List<LlmProviderModel> models = llmProviderModelService.lambdaQuery()
                .eq(LlmProviderModel::getProviderId, provider.getId())
                .eq(LlmProviderModel::getModelType, LlmModelTypeConstants.EMBEDDING)
                .eq(LlmProviderModel::getModelName, normalizedEmbeddingModel)
                .eq(LlmProviderModel::getEnabled, MODEL_STATUS_ENABLED)
                .orderByAsc(LlmProviderModel::getSort, LlmProviderModel::getId)
                .list();
        if (models.isEmpty()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    EMBEDDING_MODEL_INVALID_MESSAGE.formatted(normalizedEmbeddingModel));
        }
        return normalizedEmbeddingModel;
    }

    /**
     * 查询当前激活提供商，不存在时抛出异常。
     *
     * @return 当前激活提供商
     */
    private LlmProvider getRequiredEnabledProvider() {
        List<LlmProvider> providers = llmProviderService.lambdaQuery()
                .eq(LlmProvider::getStatus, PROVIDER_STATUS_ENABLED)
                .orderByAsc(LlmProvider::getSort, LlmProvider::getId)
                .list();
        if (providers.isEmpty()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, ENABLED_PROVIDER_MISSING_MESSAGE);
        }
        return providers.getFirst();
    }

    /**
     * 按需同步知识库启停状态到 Agent 服务。
     *
     * @param kbBase       当前知识库实体
     * @param targetStatus 目标状态
     */
    private void applyStatusChangeIfNecessary(KbBase kbBase, Integer targetStatus) {
        if (targetStatus == null) {
            return;
        }
        Assert.isParamTrue(STATUS_ENABLED == targetStatus || STATUS_DISABLED == targetStatus, "状态值不合法");
        Integer currentStatus = kbBase.getStatus();
        if (currentStatus != null && currentStatus.equals(targetStatus)) {
            return;
        }
        Assert.notEmpty(kbBase.getKnowledgeName(), "知识库名称不能为空");
        if (STATUS_ENABLED == targetStatus) {
            medicineAgentClient.loadKnowledgeBase(kbBase.getKnowledgeName());
            return;
        }
        agentConfigRuntimeSyncService.assertKnowledgeBaseCanDisable(kbBase.getKnowledgeName());
        medicineAgentClient.releaseKnowledgeBase(kbBase.getKnowledgeName());
    }

    /**
     * 归一化检索问题。
     *
     * @param question 原始检索问题
     * @return 归一化后的检索问题
     */
    private String normalizeSearchQuestion(String question) {
        String normalizedQuestion = normalizeNullableText(question);
        Assert.notEmpty(normalizedQuestion, "检索问题不能为空");
        return normalizedQuestion;
    }

    /**
     * 归一化检索知识库名称列表。
     *
     * @param knowledgeNames 原始知识库名称列表
     * @return 去重且保持顺序的知识库名称列表
     */
    private List<String> normalizeSearchKnowledgeNames(List<String> knowledgeNames) {
        Assert.notEmpty(knowledgeNames, "知识库名称列表不能为空");
        Assert.isParamTrue(knowledgeNames.size() <= 5, "知识库最多支持5个");
        List<String> normalizedNames = new ArrayList<>(knowledgeNames.size());
        LinkedHashSet<String> uniqueNames = new LinkedHashSet<>();
        for (String knowledgeName : knowledgeNames) {
            String normalizedName = normalizeNullableText(knowledgeName);
            Assert.notEmpty(normalizedName, "知识库名称不能为空");
            Assert.isParamTrue(uniqueNames.add(normalizedName),
                    KNOWLEDGE_SEARCH_DUPLICATE_MESSAGE.formatted(normalizedName));
            normalizedNames.add(normalizedName);
        }
        return normalizedNames;
    }

    /**
     * 按请求顺序加载启用中的检索知识库。
     *
     * @param knowledgeNames 知识库名称列表
     * @return 已加载的知识库实体列表
     */
    private List<KbBase> loadSearchKnowledgeBases(List<String> knowledgeNames) {
        List<KbBase> knowledgeBases = listEnabledKnowledgeBasesByNames(knowledgeNames);
        Map<String, KbBase> knowledgeBaseMap = knowledgeBases.stream()
                .collect(Collectors.toMap(KbBase::getKnowledgeName, Function.identity()));
        List<KbBase> orderedKnowledgeBases = new ArrayList<>(knowledgeNames.size());
        for (String knowledgeName : knowledgeNames) {
            KbBase kbBase = knowledgeBaseMap.get(knowledgeName);
            if (kbBase == null) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR,
                        KNOWLEDGE_SEARCH_NOT_FOUND_MESSAGE.formatted(knowledgeName));
            }
            orderedKnowledgeBases.add(kbBase);
        }
        return orderedKnowledgeBases;
    }

    /**
     * 归一化检索重排模型名称。
     *
     * @param rankingModel 原始重排模型名称
     * @return 校验通过后的重排模型名称；未选择时返回 null
     */
    private String normalizeSearchRankingModel(String rankingModel) {
        String normalizedRankingModel = normalizeNullableText(rankingModel);
        if (!StringUtils.hasText(normalizedRankingModel)) {
            return null;
        }
        return validateRankingModel(normalizedRankingModel);
    }

    /**
     * 校验检索知识库集合的向量配置是否一致。
     *
     * @param knowledgeBases 待校验的知识库实体列表
     */
    private void validateSearchKnowledgeBasesAgainstBaseline(List<KbBase> knowledgeBases) {
        Assert.notEmpty(knowledgeBases, "知识库名称列表不能为空");
        KbBase baseline = knowledgeBases.getFirst();
        for (int index = 1; index < knowledgeBases.size(); index++) {
            KbBase currentKnowledgeBase = knowledgeBases.get(index);
            if (!Objects.equals(normalizeNullableText(currentKnowledgeBase.getEmbeddingModel()),
                    normalizeNullableText(baseline.getEmbeddingModel()))) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR,
                        KNOWLEDGE_SEARCH_MODEL_MISMATCH_MESSAGE.formatted(currentKnowledgeBase.getKnowledgeName()));
            }
            if (!Objects.equals(currentKnowledgeBase.getEmbeddingDim(), baseline.getEmbeddingDim())) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR,
                        KNOWLEDGE_SEARCH_DIM_MISMATCH_MESSAGE.formatted(currentKnowledgeBase.getKnowledgeName()));
            }
        }
    }

    /**
     * 将结构化检索结果转换为页面响应对象。
     *
     * @param responseData   Agent 返回的结构化检索结果
     * @param knowledgeBases 本次参与检索的知识库实体列表
     * @return 页面响应对象
     */
    private KnowledgeBaseSearchVo toKnowledgeBaseSearchVo(MedicineAgentClient.KnowledgeSearchResponseData responseData,
                                                          List<KbBase> knowledgeBases) {
        Map<String, KbBase> knowledgeBaseMap = knowledgeBases.stream()
                .collect(Collectors.toMap(KbBase::getKnowledgeName, Function.identity()));
        List<KnowledgeBaseSearchHitVo> hitVos = new ArrayList<>();
        if (responseData.getHits() != null) {
            for (MedicineAgentClient.KnowledgeSearchHitRow hitRow : responseData.getHits()) {
                hitVos.add(toKnowledgeBaseSearchHitVo(hitRow, knowledgeBaseMap));
            }
        }
        KnowledgeBaseSearchVo vo = new KnowledgeBaseSearchVo();
        vo.setHits(hitVos);
        return vo;
    }

    /**
     * 将单条结构化检索命中结果转换为页面响应对象。
     *
     * @param hitRow            Agent 返回的单条命中结果
     * @param knowledgeBaseMap 参与检索的知识库映射
     * @return 页面响应对象
     */
    private KnowledgeBaseSearchHitVo toKnowledgeBaseSearchHitVo(MedicineAgentClient.KnowledgeSearchHitRow hitRow,
                                                                Map<String, KbBase> knowledgeBaseMap) {
        Assert.notNull(hitRow, "知识检索命中结果不能为空");
        String knowledgeName = hitRow.getKnowledge_name();
        KbBase kbBase = knowledgeBaseMap.get(knowledgeName);
        Assert.isTrue(kbBase != null, KNOWLEDGE_SEARCH_UNKNOWN_KB_MESSAGE.formatted(knowledgeName));

        KnowledgeBaseSearchHitVo hitVo = new KnowledgeBaseSearchHitVo();
        hitVo.setKnowledgeName(knowledgeName);
        hitVo.setKnowledgeDisplayName(kbBase.getDisplayName());
        hitVo.setScore(hitRow.getScore());
        hitVo.setDocumentId(hitRow.getDocument_id());
        hitVo.setChunkIndex(hitRow.getChunk_index());
        hitVo.setCharCount(hitRow.getChar_count());
        hitVo.setContent(hitRow.getContent());
        return hitVo;
    }

    /**
     * 归一化可选文本内容。
     *
     * @param value 原始文本
     * @return 去除首尾空白后的文本；为空时返回 null
     */
    private String normalizeNullableText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 归一化知识库封面地址。
     *
     * @param cover 原始封面地址
     * @return 去除首尾空白后的封面地址；为空时返回 null
     */
    private String normalizeCover(String cover) {
        if (cover == null) {
            return null;
        }
        String normalized = cover.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}
