package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.mapper.MallProductTagMapper;
import com.zhangyichuang.medicine.client.mapper.MallProductTagTypeMapper;
import com.zhangyichuang.medicine.client.model.request.MallProductSearchRequest;
import com.zhangyichuang.medicine.client.model.vo.MallProductSearchTagFilterOptionVo;
import com.zhangyichuang.medicine.client.model.vo.MallProductSearchTagFilterVo;
import com.zhangyichuang.medicine.client.service.MallProductTagRelService;
import com.zhangyichuang.medicine.client.service.MallProductTagService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.model.constants.MallProductTagConstants;
import com.zhangyichuang.medicine.model.dto.MallProductTagFilterGroup;
import com.zhangyichuang.medicine.model.entity.MallProductTag;
import com.zhangyichuang.medicine.model.entity.MallProductTagRel;
import com.zhangyichuang.medicine.model.entity.MallProductTagType;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品标签服务实现（客户端）。
 *
 * @author Chuang
 */
@Service
@RequiredArgsConstructor
public class MallProductTagServiceImpl extends ServiceImpl<MallProductTagMapper, MallProductTag>
        implements MallProductTagService {

    /**
     * 商品标签关联服务。
     */
    private final MallProductTagRelService mallProductTagRelService;

    /**
     * 商品标签类型 Mapper。
     */
    private final MallProductTagTypeMapper mallProductTagTypeMapper;

    /**
     * 为搜索请求补充分组后的标签条件。
     *
     * @param request 搜索请求
     */
    @Override
    public void fillSearchTagGroups(MallProductSearchRequest request) {
        Assert.notNull(request, "搜索参数不能为空");
        List<Long> normalizedTagIds = normalizeDistinctIds(request.getTagIds());
        request.setTagIds(normalizedTagIds);
        if (normalizedTagIds.isEmpty()) {
            request.setTagFilterGroups(List.of());
            return;
        }
        List<MallProductTag> tags = lambdaQuery()
                .in(MallProductTag::getId, normalizedTagIds)
                .eq(MallProductTag::getStatus, MallProductTagConstants.STATUS_ENABLED)
                .list();
        if (tags.size() != normalizedTagIds.size()) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "筛选标签不存在或未启用");
        }
        Map<Long, MallProductTag> tagMap = tags.stream()
                .collect(Collectors.toMap(MallProductTag::getId, tag -> tag));
        Map<Long, MallProductTagType> typeMap = listTypeMapByIds(
                tags.stream().map(MallProductTag::getTypeId).filter(Objects::nonNull).distinct().toList()
        );
        request.setTagFilterGroups(buildFilterGroups(normalizedTagIds, tagMap, typeMap));
    }

    /**
     * 查询商品与启用标签映射。
     *
     * @param productIds 商品ID列表
     * @return 商品ID到标签列表映射
     */
    @Override
    public Map<Long, List<MallProductTagVo>> listEnabledTagVoMapByProductIds(List<Long> productIds) {
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
        List<MallProductTag> tags = lambdaQuery()
                .in(MallProductTag::getId, tagIds)
                .eq(MallProductTag::getStatus, MallProductTagConstants.STATUS_ENABLED)
                .list();
        if (tags.isEmpty()) {
            return Map.of();
        }
        Map<Long, MallProductTagType> typeMap = listTypeMapByIds(
                tags.stream().map(MallProductTag::getTypeId).filter(Objects::nonNull).distinct().toList()
        );
        List<MallProductTag> orderedTags = sortTags(tags, typeMap).stream()
                .filter(tag -> typeMap.containsKey(tag.getTypeId()))
                .toList();
        Map<Long, MallProductTagVo> tagVoMap = new LinkedHashMap<>();
        Map<Long, Integer> orderMap = new LinkedHashMap<>();
        for (int i = 0; i < orderedTags.size(); i++) {
            MallProductTag tag = orderedTags.get(i);
            tagVoMap.put(tag.getId(), toTagVo(tag, typeMap.get(tag.getTypeId())));
            orderMap.put(tag.getId(), i);
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
                    .sorted(Comparator.comparing(tagId -> orderMap.getOrDefault(tagId, Integer.MAX_VALUE)))
                    .map(tagVoMap::get)
                    .filter(Objects::nonNull)
                    .toList();
            result.put(entry.getKey(), tagVos);
        }
        return result;
    }

    /**
     * 根据 ES 聚合结果构建搜索标签筛选列表。
     *
     * @param tagBindingCountMap 标签类型绑定与命中数量映射
     * @return 搜索标签筛选列表
     */
    @Override
    public List<MallProductSearchTagFilterVo> buildSearchTagFilters(Map<String, Long> tagBindingCountMap) {
        if (tagBindingCountMap == null || tagBindingCountMap.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> tagCountMap = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : tagBindingCountMap.entrySet()) {
            TagBinding tagBinding = parseTagBinding(entry.getKey());
            if (tagBinding == null || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            tagCountMap.put(tagBinding.tagId(), entry.getValue());
        }
        if (tagCountMap.isEmpty()) {
            return List.of();
        }
        List<MallProductTag> tags = lambdaQuery()
                .in(MallProductTag::getId, tagCountMap.keySet())
                .eq(MallProductTag::getStatus, MallProductTagConstants.STATUS_ENABLED)
                .list();
        if (tags.isEmpty()) {
            return List.of();
        }
        Map<Long, MallProductTagType> typeMap = listTypeMapByIds(
                tags.stream().map(MallProductTag::getTypeId).filter(Objects::nonNull).distinct().toList()
        );
        List<MallProductTag> orderedTags = sortTags(tags, typeMap).stream()
                .filter(tag -> typeMap.containsKey(tag.getTypeId()))
                .toList();
        Map<Long, MallProductSearchTagFilterVo> groupMap = new LinkedHashMap<>();
        for (MallProductTag tag : orderedTags) {
            Long count = tagCountMap.get(tag.getId());
            if (count == null || count <= 0) {
                continue;
            }
            MallProductTagType type = typeMap.get(tag.getTypeId());
            MallProductSearchTagFilterVo filterVo = groupMap.computeIfAbsent(type.getId(), key -> {
                MallProductSearchTagFilterVo group = new MallProductSearchTagFilterVo();
                group.setTypeId(type.getId());
                group.setTypeCode(type.getCode());
                group.setTypeName(type.getName());
                group.setOptions(new ArrayList<>());
                return group;
            });
            MallProductSearchTagFilterOptionVo optionVo = new MallProductSearchTagFilterOptionVo();
            optionVo.setTagId(tag.getId());
            optionVo.setTagName(tag.getName());
            optionVo.setCount(count);
            filterVo.getOptions().add(optionVo);
        }
        return groupMap.values().stream()
                .filter(group -> group.getOptions() != null && !group.getOptions().isEmpty())
                .toList();
    }

    /**
     * 查询数据库中全部启用的商品标签筛选分组。
     *
     * @return 按标签类型分组后的筛选项列表
     */
    @Override
    public List<MallProductSearchTagFilterVo> listAllEnabledSearchTagFilters() {
        List<MallProductTag> tags = lambdaQuery()
                .eq(MallProductTag::getStatus, MallProductTagConstants.STATUS_ENABLED)
                .list();
        if (tags.isEmpty()) {
            return List.of();
        }
        Map<Long, MallProductTagType> typeMap = listTypeMapByIds(
                tags.stream().map(MallProductTag::getTypeId).filter(Objects::nonNull).distinct().toList()
        );
        List<MallProductTag> orderedTags = sortTags(tags, typeMap).stream()
                .filter(tag -> typeMap.containsKey(tag.getTypeId()))
                .toList();
        Map<Long, MallProductSearchTagFilterVo> groupMap = new LinkedHashMap<>();
        for (MallProductTag tag : orderedTags) {
            MallProductTagType type = typeMap.get(tag.getTypeId());
            MallProductSearchTagFilterVo filterVo = groupMap.computeIfAbsent(type.getId(), key -> {
                MallProductSearchTagFilterVo group = new MallProductSearchTagFilterVo();
                group.setTypeId(type.getId());
                group.setTypeCode(type.getCode());
                group.setTypeName(type.getName());
                group.setOptions(new ArrayList<>());
                return group;
            });
            MallProductSearchTagFilterOptionVo optionVo = new MallProductSearchTagFilterOptionVo();
            optionVo.setTagId(tag.getId());
            optionVo.setTagName(tag.getName());
            filterVo.getOptions().add(optionVo);
        }
        return groupMap.values().stream()
                .filter(group -> group.getOptions() != null && !group.getOptions().isEmpty())
                .toList();
    }

    /**
     * 构建标签筛选分组。
     *
     * @param orderedTagIds 原始有序标签ID列表
     * @param tagMap        标签映射
     * @param typeMap       类型映射
     * @return 标签筛选分组
     */
    private List<MallProductTagFilterGroup> buildFilterGroups(List<Long> orderedTagIds,
                                                              Map<Long, MallProductTag> tagMap,
                                                              Map<Long, MallProductTagType> typeMap) {
        Map<Long, MallProductTagFilterGroup> groupMap = new LinkedHashMap<>();
        for (Long tagId : orderedTagIds) {
            MallProductTag tag = tagMap.get(tagId);
            if (tag == null) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "筛选标签不存在或未启用");
            }
            MallProductTagType type = typeMap.get(tag.getTypeId());
            if (type == null) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "筛选标签类型不存在或未启用");
            }
            MallProductTagFilterGroup filterGroup = groupMap.computeIfAbsent(type.getId(), key -> {
                MallProductTagFilterGroup group =
                        new MallProductTagFilterGroup();
                group.setTypeId(type.getId());
                group.setTypeCode(type.getCode());
                group.setTagIds(new ArrayList<>());
                return group;
            });
            filterGroup.getTagIds().add(tagId);
        }
        return new ArrayList<>(groupMap.values());
    }

    /**
     * 按标签类型和标签排序规则返回标签列表。
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
     * 查询启用标签类型映射。
     *
     * @param typeIds 标签类型ID列表
     * @return 启用标签类型映射
     */
    private Map<Long, MallProductTagType> listTypeMapByIds(List<Long> typeIds) {
        if (typeIds == null || typeIds.isEmpty()) {
            return Map.of();
        }
        return mallProductTagTypeMapper.selectBatchIds(typeIds).stream()
                .filter(type -> Objects.equals(type.getStatus(), MallProductTagConstants.STATUS_ENABLED))
                .collect(Collectors.toMap(MallProductTagType::getId, type -> type));
    }

    /**
     * 规范化并去重ID集合。
     *
     * @param ids 原始ID集合
     * @return 去重后的ID集合
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
     * 将标签转换为视图对象。
     *
     * @param tag  标签实体
     * @param type 标签类型实体
     * @return 标签视图对象
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
     * 计算默认排序值。
     *
     * @param value 排序值
     * @return 规范化后的排序值
     */
    private Integer defaultSort(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
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
     * 解析标签类型绑定字符串。
     *
     * @param binding 标签类型绑定字符串
     * @return 标签类型绑定对象
     */
    private TagBinding parseTagBinding(String binding) {
        if (!StringUtils.hasText(binding)) {
            return null;
        }
        int separatorIndex = binding.indexOf(MallProductTagConstants.TYPE_BINDING_SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex >= binding.length() - 1) {
            return null;
        }
        String typeCode = binding.substring(0, separatorIndex);
        String tagIdText = binding.substring(separatorIndex + 1);
        if (!StringUtils.hasText(typeCode) || !StringUtils.hasText(tagIdText)) {
            return null;
        }
        try {
            return new TagBinding(typeCode.trim(), Long.parseLong(tagIdText.trim()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 标签类型绑定对象。
     *
     * @param typeCode 标签类型编码
     * @param tagId    标签ID
     */
    private record TagBinding(String typeCode, Long tagId) {
    }
}
