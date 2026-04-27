package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.MallProductTagMapper;
import com.zhangyichuang.medicine.admin.service.MallProductTagRelService;
import com.zhangyichuang.medicine.admin.service.MallProductTagService;
import com.zhangyichuang.medicine.admin.service.MallProductTagTypeService;
import com.zhangyichuang.medicine.admin.task.MallProductSearchIndexer;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.constants.MallProductTagConstants;
import com.zhangyichuang.medicine.model.dto.MallProductTagFilterGroup;
import com.zhangyichuang.medicine.model.entity.MallProductTag;
import com.zhangyichuang.medicine.model.entity.MallProductTagRel;
import com.zhangyichuang.medicine.model.entity.MallProductTagType;
import com.zhangyichuang.medicine.model.request.*;
import com.zhangyichuang.medicine.model.vo.MallProductTagAdminVo;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品标签服务实现。
 *
 * @author Chuang
 */
@Service
@RequiredArgsConstructor
public class MallProductTagServiceImpl extends ServiceImpl<MallProductTagMapper, MallProductTag>
        implements MallProductTagService, BaseService {

    /**
     * 商品标签关联服务。
     */
    private final MallProductTagRelService mallProductTagRelService;

    /**
     * 商品标签类型服务。
     */
    private final MallProductTagTypeService mallProductTagTypeService;

    /**
     * 商品搜索索引器提供器。
     */
    private final ObjectProvider<MallProductSearchIndexer> mallProductSearchIndexerProvider;

    /**
     * 分页查询标签列表。
     *
     * @param request 查询参数
     * @return 标签分页结果
     */
    @Override
    public Page<MallProductTagAdminVo> listTags(MallProductTagListQueryRequest request) {
        Assert.notNull(request, "查询参数不能为空");
        return baseMapper.listTags(request.toPage(), request);
    }

    /**
     * 查询标签详情。
     *
     * @param id 标签ID
     * @return 标签详情
     */
    @Override
    public MallProductTagAdminVo getTagById(Long id) {
        Assert.isPositive(id, "标签ID不能为空");
        MallProductTag tag = getById(id);
        if (tag == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "标签不存在");
        }
        MallProductTagType type = mallProductTagTypeService.getTypeEntityById(tag.getTypeId());
        return toAdminVo(tag, type);
    }

    /**
     * 查询启用标签下拉数据。
     *
     * @param typeCode 标签类型编码
     * @return 标签下拉数据
     */
    @Override
    public List<MallProductTagVo> option(String typeCode) {
        Long filterTypeId = null;
        if (StringUtils.hasText(typeCode)) {
            MallProductTagType type = mallProductTagTypeService.getTypeEntityByCode(typeCode);
            if (type == null || !Objects.equals(type.getStatus(), MallProductTagConstants.STATUS_ENABLED)) {
                return List.of();
            }
            filterTypeId = type.getId();
        }
        LambdaQueryWrapper<MallProductTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MallProductTag::getStatus, MallProductTagConstants.STATUS_ENABLED);
        if (filterTypeId != null) {
            queryWrapper.eq(MallProductTag::getTypeId, filterTypeId);
        }
        List<MallProductTag> tags = list(queryWrapper);
        Map<Long, MallProductTagType> typeMap = mallProductTagTypeService.listTypeEntityMapByIds(
                tags.stream().map(MallProductTag::getTypeId).filter(Objects::nonNull).distinct().toList()
        );
        return sortTags(tags, typeMap).stream()
                .filter(tag -> isTypeEnabled(typeMap.get(tag.getTypeId())))
                .map(tag -> toTagVo(tag, typeMap.get(tag.getTypeId())))
                .toList();
    }

    /**
     * 新增标签。
     *
     * @param request 新增请求
     * @return 是否成功
     */
    @Override
    public boolean addTag(MallProductTagAddRequest request) {
        Assert.notNull(request, "标签信息不能为空");
        String normalizedName = normalizeName(request.getName());
        MallProductTagType type = assertEnabledType(request.getTypeId());
        normalizeStatus(request.getStatus());
        MallProductTag existingTag = findTagByTypeAndName(type.getId(), normalizedName);
        if (existingTag != null) {
            if (Objects.equals(existingTag.getStatus(), MallProductTagConstants.STATUS_ENABLED)) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "同类型标签名称已存在");
            }
            existingTag.setSort(normalizeSort(request.getSort()));
            existingTag.setStatus(MallProductTagConstants.STATUS_ENABLED);
            existingTag.setUpdateTime(new Date());
            existingTag.setUpdateBy(getUsername());
            boolean updated = updateById(existingTag);
            if (updated) {
                reindexRelatedProducts(List.of(existingTag.getId()));
            }
            return updated;
        }

        MallProductTag tag = new MallProductTag();
        tag.setName(normalizedName);
        tag.setTypeId(type.getId());
        tag.setSort(normalizeSort(request.getSort()));
        tag.setStatus(MallProductTagConstants.STATUS_ENABLED);
        tag.setCreateTime(new Date());
        tag.setCreateBy(getUsername());
        try {
            return save(tag);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "同类型标签名称已存在");
        }
    }

    /**
     * 修改标签。
     *
     * @param request 修改请求
     * @return 是否成功
     */
    @Override
    public boolean updateTag(MallProductTagUpdateRequest request) {
        Assert.notNull(request, "标签信息不能为空");
        MallProductTag existingTag = getTagEntityById(request.getId());
        String normalizedName = normalizeName(request.getName());
        MallProductTagType type = assertEnabledType(request.getTypeId());
        assertTagNameUnique(existingTag.getId(), type.getId(), normalizedName);

        existingTag.setName(normalizedName);
        existingTag.setTypeId(type.getId());
        existingTag.setSort(normalizeSort(request.getSort()));
        existingTag.setUpdateTime(new Date());
        existingTag.setUpdateBy(getUsername());
        try {
            boolean updated = updateById(existingTag);
            if (updated) {
                reindexRelatedProducts(List.of(existingTag.getId()));
            }
            return updated;
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "同类型标签名称已存在");
        }
    }

    /**
     * 修改标签状态。
     *
     * @param request 状态修改请求
     * @return 是否成功
     */
    @Override
    public boolean updateTagStatus(MallProductTagStatusUpdateRequest request) {
        Assert.notNull(request, "标签状态信息不能为空");
        MallProductTag existingTag = getTagEntityById(request.getId());
        Integer targetStatus = normalizeStatus(request.getStatus());
        if (Objects.equals(existingTag.getStatus(), targetStatus)) {
            return true;
        }
        existingTag.setStatus(targetStatus);
        existingTag.setUpdateTime(new Date());
        existingTag.setUpdateBy(getUsername());
        boolean updated = updateById(existingTag);
        if (updated) {
            reindexRelatedProducts(List.of(existingTag.getId()));
        }
        return updated;
    }

    /**
     * 删除标签。
     *
     * @param id 标签ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTag(Long id) {
        MallProductTag tag = getTagEntityById(id);
        if (mallProductTagRelService.existsByTagId(id)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "标签已绑定商品，请先禁用标签");
        }
        return baseMapper.physicalDeleteById(tag.getId()) > 0;
    }

    /**
     * 校验并返回启用标签ID集合。
     *
     * @param tagIds 标签ID集合
     * @return 去重后的标签ID集合
     */
    @Override
    public List<Long> normalizeEnabledTagIds(List<Long> tagIds) {
        List<Long> normalizedIds = normalizeDistinctIds(tagIds);
        if (normalizedIds.isEmpty()) {
            return List.of();
        }
        List<MallProductTag> tags = listByIds(normalizedIds);
        if (tags.size() != normalizedIds.size()) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "商品标签不存在");
        }
        Map<Long, MallProductTag> tagMap = tags.stream()
                .collect(Collectors.toMap(MallProductTag::getId, tag -> tag));
        Map<Long, MallProductTagType> typeMap = mallProductTagTypeService.listTypeEntityMapByIds(
                tags.stream().map(MallProductTag::getTypeId).filter(Objects::nonNull).distinct().toList()
        );
        for (Long tagId : normalizedIds) {
            MallProductTag tag = tagMap.get(tagId);
            if (tag == null) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "商品标签不存在");
            }
            if (!Objects.equals(tag.getStatus(), MallProductTagConstants.STATUS_ENABLED)) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "商品标签未启用，无法绑定");
            }
            MallProductTagType type = typeMap.get(tag.getTypeId());
            if (type == null) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "商品标签类型不存在");
            }
            if (!Objects.equals(type.getStatus(), MallProductTagConstants.STATUS_ENABLED)) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "商品标签类型未启用，无法绑定");
            }
        }
        return normalizedIds;
    }

    /**
     * 校验并返回商品可保存的标签ID集合。
     *
     * @param tagIds         商品当前提交的标签ID集合
     * @param existingTagIds 商品原已绑定的标签ID集合
     * @return 去重后的标签ID集合
     */
    @Override
    public List<Long> normalizeProductTagIds(List<Long> tagIds, List<Long> existingTagIds) {
        List<Long> normalizedIds = normalizeDistinctIds(tagIds);
        if (normalizedIds.isEmpty()) {
            return List.of();
        }
        List<Long> normalizedExistingIds = normalizeDistinctIds(existingTagIds);
        List<MallProductTag> tags = listByIds(normalizedIds);
        if (tags.size() != normalizedIds.size()) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "商品标签不存在");
        }
        Map<Long, MallProductTag> tagMap = tags.stream()
                .collect(Collectors.toMap(MallProductTag::getId, tag -> tag));
        Map<Long, MallProductTagType> typeMap = mallProductTagTypeService.listTypeEntityMapByIds(
                tags.stream().map(MallProductTag::getTypeId).filter(Objects::nonNull).distinct().toList()
        );
        for (Long tagId : normalizedIds) {
            MallProductTag tag = tagMap.get(tagId);
            if (tag == null) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "商品标签不存在");
            }
            MallProductTagType type = typeMap.get(tag.getTypeId());
            if (type == null) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "商品标签类型不存在");
            }
            boolean isEnabledTag = Objects.equals(tag.getStatus(), MallProductTagConstants.STATUS_ENABLED);
            boolean isEnabledType = Objects.equals(type.getStatus(), MallProductTagConstants.STATUS_ENABLED);
            if (isEnabledTag && isEnabledType) {
                continue;
            }
            boolean isExistingBinding = normalizedExistingIds.contains(tagId);
            if (!isExistingBinding) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "商品标签未启用，无法绑定");
            }
        }
        return normalizedIds;
    }

    /**
     * 将商品标签筛选条件按类型拆分。
     *
     * @param request 商品查询请求
     */
    @Override
    public void fillTagFilterGroups(MallProductListQueryRequest request) {
        Assert.notNull(request, "查询参数不能为空");
        List<Long> normalizedTagIds = normalizeDistinctIds(request.getTagIds());
        request.setTagIds(normalizedTagIds);
        if (normalizedTagIds.isEmpty()) {
            request.setTagFilterGroups(List.of());
            return;
        }
        List<MallProductTag> tags = listByIds(normalizedTagIds);
        if (tags.size() != normalizedTagIds.size()) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "筛选标签不存在");
        }
        Map<Long, MallProductTag> tagMap = tags.stream()
                .collect(Collectors.toMap(MallProductTag::getId, tag -> tag));
        Map<Long, MallProductTagType> typeMap = mallProductTagTypeService.listTypeEntityMapByIds(
                tags.stream().map(MallProductTag::getTypeId).filter(Objects::nonNull).distinct().toList()
        );
        request.setTagFilterGroups(buildFilterGroups(normalizedTagIds, tagMap, typeMap, "筛选标签类型不存在"));
    }

    /**
     * 查询商品与标签的映射。
     *
     * @param productIds 商品ID列表
     * @return 商品ID到标签列表的映射
     */
    @Override
    public Map<Long, List<MallProductTagVo>> listTagVoMapByProductIds(List<Long> productIds) {
        return listTagVoMapByProductIds(productIds, false);
    }

    /**
     * 查询商品与启用标签的映射。
     *
     * @param productIds 商品ID列表
     * @return 商品ID到标签列表的映射
     */
    @Override
    public Map<Long, List<MallProductTagVo>> listEnabledTagVoMapByProductIds(List<Long> productIds) {
        return listTagVoMapByProductIds(productIds, true);
    }

    /**
     * 查询标签实体。
     *
     * @param id 标签ID
     * @return 标签实体
     */
    private MallProductTag getTagEntityById(Long id) {
        Assert.isPositive(id, "标签ID不能为空");
        MallProductTag tag = getById(id);
        if (tag == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "标签不存在");
        }
        return tag;
    }

    /**
     * 校验标签名称在同类型下唯一。
     *
     * @param currentId 当前标签ID
     * @param typeId    标签类型ID
     * @param name      标签名称
     */
    private void assertTagNameUnique(Long currentId, Long typeId, String name) {
        LambdaQueryWrapper<MallProductTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MallProductTag::getTypeId, typeId)
                .eq(MallProductTag::getName, name);
        if (currentId != null) {
            queryWrapper.ne(MallProductTag::getId, currentId);
        }
        if (count(queryWrapper) > 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "同类型标签名称已存在");
        }
    }

    /**
     * 按类型与名称查询标签实体。
     *
     * @param typeId 标签类型ID
     * @param name   标签名称
     * @return 标签实体；不存在时返回 {@code null}
     */
    private MallProductTag findTagByTypeAndName(Long typeId, String name) {
        return lambdaQuery()
                .eq(MallProductTag::getTypeId, typeId)
                .eq(MallProductTag::getName, name)
                .one();
    }

    /**
     * 构建标签筛选分组。
     *
     * @param orderedTagIds 原始有序标签ID列表
     * @param tagMap        标签映射
     * @param typeMap       类型映射
     * @param errorMessage  异常消息
     * @return 标签筛选分组
     */
    private List<MallProductTagFilterGroup> buildFilterGroups(List<Long> orderedTagIds,
                                                              Map<Long, MallProductTag> tagMap,
                                                              Map<Long, MallProductTagType> typeMap,
                                                              String errorMessage) {
        Map<Long, MallProductTagFilterGroup> groupMap = new LinkedHashMap<>();
        for (Long tagId : orderedTagIds) {
            MallProductTag tag = tagMap.get(tagId);
            if (tag == null) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "筛选标签不存在");
            }
            MallProductTagType type = typeMap.get(tag.getTypeId());
            if (type == null) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, errorMessage);
            }
            MallProductTagFilterGroup group = groupMap.computeIfAbsent(type.getId(), key -> {
                MallProductTagFilterGroup filterGroup =
                        new MallProductTagFilterGroup();
                filterGroup.setTypeId(type.getId());
                filterGroup.setTypeCode(type.getCode());
                filterGroup.setTagIds(new ArrayList<>());
                return filterGroup;
            });
            group.getTagIds().add(tagId);
        }
        return new ArrayList<>(groupMap.values());
    }

    /**
     * 根据商品ID列表查询标签映射。
     *
     * @param productIds  商品ID列表
     * @param enabledOnly 是否仅返回启用标签
     * @return 商品标签映射
     */
    private Map<Long, List<MallProductTagVo>> listTagVoMapByProductIds(List<Long> productIds, boolean enabledOnly) {
        List<Long> normalizedProductIds = normalizeDistinctIds(productIds);
        if (normalizedProductIds.isEmpty()) {
            return Map.of();
        }
        List<MallProductTagRel> relations = mallProductTagRelService.listByProductIds(normalizedProductIds);
        if (relations.isEmpty()) {
            return Map.of();
        }
        List<Long> tagIds = relations.stream()
                .map(MallProductTagRel::getTagId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (tagIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<MallProductTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(MallProductTag::getId, tagIds);
        if (enabledOnly) {
            queryWrapper.eq(MallProductTag::getStatus, MallProductTagConstants.STATUS_ENABLED);
        }
        List<MallProductTag> tags = list(queryWrapper);
        if (tags.isEmpty()) {
            return Map.of();
        }
        Map<Long, MallProductTagType> typeMap = mallProductTagTypeService.listTypeEntityMapByIds(
                tags.stream().map(MallProductTag::getTypeId).filter(Objects::nonNull).distinct().toList()
        );
        if (enabledOnly) {
            typeMap = typeMap.entrySet().stream()
                    .filter(entry -> isTypeEnabled(entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        Map<Long, MallProductTagType> availableTypeMap = typeMap;
        List<MallProductTag> orderedTags = sortTags(tags, availableTypeMap).stream()
                .filter(tag -> availableTypeMap.containsKey(tag.getTypeId()))
                .toList();
        Map<Long, MallProductTagVo> tagVoMap = new LinkedHashMap<>();
        Map<Long, Integer> tagOrderMap = new LinkedHashMap<>();
        for (int i = 0; i < orderedTags.size(); i++) {
            MallProductTag tag = orderedTags.get(i);
            MallProductTagType type = availableTypeMap.get(tag.getTypeId());
            tagVoMap.put(tag.getId(), toTagVo(tag, type));
            tagOrderMap.put(tag.getId(), i);
        }
        Map<Long, List<Long>> productTagIdsMap = new LinkedHashMap<>();
        for (MallProductTagRel relation : relations) {
            if (relation == null || relation.getProductId() == null || relation.getTagId() == null) {
                continue;
            }
            if (!tagVoMap.containsKey(relation.getTagId())) {
                continue;
            }
            productTagIdsMap.computeIfAbsent(relation.getProductId(), key -> new ArrayList<>()).add(relation.getTagId());
        }
        Map<Long, List<MallProductTagVo>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Long>> entry : productTagIdsMap.entrySet()) {
            List<MallProductTagVo> tagVos = entry.getValue().stream()
                    .distinct()
                    .sorted(Comparator.comparing(tagId -> tagOrderMap.getOrDefault(tagId, Integer.MAX_VALUE)))
                    .map(tagVoMap::get)
                    .filter(Objects::nonNull)
                    .toList();
            result.put(entry.getKey(), tagVos);
        }
        return result;
    }

    /**
     * 按类型排序后返回标签列表。
     *
     * @param tags    标签列表
     * @param typeMap 类型映射
     * @return 排序后的标签列表
     */
    private List<MallProductTag> sortTags(List<MallProductTag> tags, Map<Long, MallProductTagType> typeMap) {
        return tags.stream()
                .sorted(Comparator
                        .comparing((MallProductTag tag) -> defaultSort(typeMap.get(tag.getTypeId())))
                        .thenComparing(tag -> defaultSort(tag.getSort()))
                        .thenComparing(MallProductTag::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * 规范化标签名称。
     *
     * @param name 原始标签名称
     * @return 规范化后的标签名称
     */
    private String normalizeName(String name) {
        Assert.notEmpty(name, "标签名称不能为空");
        String normalizedName = name.trim();
        Assert.notEmpty(normalizedName, "标签名称不能为空");
        return normalizedName;
    }

    /**
     * 规范化排序值。
     *
     * @param sort 原始排序值
     * @return 规范化后的排序值
     */
    private Integer normalizeSort(Integer sort) {
        return sort == null ? 0 : sort;
    }

    /**
     * 规范化状态值。
     *
     * @param status 原始状态值
     * @return 规范化后的状态值
     */
    private Integer normalizeStatus(Integer status) {
        Assert.notNull(status, "标签状态不能为空");
        Set<Integer> supportedStatusSet = Set.of(
                MallProductTagConstants.STATUS_ENABLED,
                MallProductTagConstants.STATUS_DISABLED
        );
        if (!supportedStatusSet.contains(status)) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "标签状态不合法");
        }
        return status;
    }

    /**
     * 规范化并去重ID集合。
     *
     * @param ids 原始ID集合
     * @return 去重后的ID列表
     */
    private List<Long> normalizeDistinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 校验并返回启用标签类型。
     *
     * @param typeId 标签类型ID
     * @return 标签类型
     */
    private MallProductTagType assertEnabledType(Long typeId) {
        MallProductTagType type = mallProductTagTypeService.getTypeEntityById(typeId);
        if (!Objects.equals(type.getStatus(), MallProductTagConstants.STATUS_ENABLED)) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "标签类型未启用");
        }
        return type;
    }

    /**
     * 判断标签类型是否启用。
     *
     * @param type 标签类型
     * @return 是否启用
     */
    private boolean isTypeEnabled(MallProductTagType type) {
        return type != null && Objects.equals(type.getStatus(), MallProductTagConstants.STATUS_ENABLED);
    }

    /**
     * 将标签转换为简要视图。
     *
     * @param tag  标签实体
     * @param type 标签类型实体
     * @return 标签视图
     */
    private MallProductTagVo toTagVo(MallProductTag tag, MallProductTagType type) {
        MallProductTagVo tagVo = new MallProductTagVo();
        tagVo.setId(tag.getId());
        tagVo.setName(tag.getName());
        tagVo.setTypeId(tag.getTypeId());
        tagVo.setTypeCode(type == null ? null : type.getCode());
        tagVo.setTypeName(type == null ? null : type.getName());
        return tagVo;
    }

    /**
     * 将标签转换为管理视图。
     *
     * @param tag  标签实体
     * @param type 标签类型实体
     * @return 管理视图
     */
    private MallProductTagAdminVo toAdminVo(MallProductTag tag, MallProductTagType type) {
        MallProductTagAdminVo adminVo = new MallProductTagAdminVo();
        adminVo.setId(tag.getId());
        adminVo.setName(tag.getName());
        adminVo.setTypeId(tag.getTypeId());
        adminVo.setTypeCode(type == null ? null : type.getCode());
        adminVo.setTypeName(type == null ? null : type.getName());
        adminVo.setSort(tag.getSort());
        adminVo.setStatus(tag.getStatus());
        adminVo.setCreateTime(tag.getCreateTime());
        adminVo.setUpdateTime(tag.getUpdateTime());
        adminVo.setCreateBy(tag.getCreateBy());
        adminVo.setUpdateBy(tag.getUpdateBy());
        return adminVo;
    }

    /**
     * 计算默认排序值。
     *
     * @param sort 排序值
     * @return 规范化后的排序值
     */
    private Integer defaultSort(Integer sort) {
        return sort == null ? Integer.MAX_VALUE : sort;
    }

    /**
     * 计算标签类型默认排序值。
     *
     * @param type 标签类型
     * @return 规范化后的排序值
     */
    private Integer defaultSort(MallProductTagType type) {
        return type == null ? Integer.MAX_VALUE : defaultSort(type.getSort());
    }

    /**
     * 重建关联商品索引。
     *
     * @param tagIds 标签ID列表
     */
    private void reindexRelatedProducts(List<Long> tagIds) {
        List<Long> productIds = mallProductTagRelService.listProductIdsByTagIds(tagIds);
        if (productIds.isEmpty()) {
            return;
        }
        runAfterCommit(() -> mallProductSearchIndexerProvider.getObject().reindexByProductIdsAsync(productIds));
    }

    /**
     * 在事务提交后触发异步同步。
     *
     * @param task 提交后执行任务
     */
    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    /**
     * 返回空分页结果。
     *
     * @param request 查询请求
     * @return 空分页
     */
    private Page<MallProductTagAdminVo> emptyPage(MallProductTagListQueryRequest request) {
        return new Page<>(request.getPageNum(), request.getPageSize(), 0);
    }
}
