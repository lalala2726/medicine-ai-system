package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.MallProductTag;
import com.zhangyichuang.medicine.model.request.*;
import com.zhangyichuang.medicine.model.vo.MallProductTagAdminVo;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;

import java.util.List;
import java.util.Map;

/**
 * 商品标签服务。
 *
 * @author Chuang
 */
public interface MallProductTagService extends IService<MallProductTag> {

    /**
     * 分页查询标签列表。
     *
     * @param request 查询参数
     * @return 标签分页列表
     */
    Page<MallProductTagAdminVo> listTags(MallProductTagListQueryRequest request);

    /**
     * 查询标签详情。
     *
     * @param id 标签ID
     * @return 标签详情
     */
    MallProductTagAdminVo getTagById(Long id);

    /**
     * 查询启用标签下拉列表。
     *
     * @param typeCode 标签类型编码，可为空
     * @return 标签下拉列表
     */
    List<MallProductTagVo> option(String typeCode);

    /**
     * 新增标签。
     *
     * @param request 新增请求
     * @return 是否成功
     */
    boolean addTag(MallProductTagAddRequest request);

    /**
     * 修改标签。
     *
     * @param request 修改请求
     * @return 是否成功
     */
    boolean updateTag(MallProductTagUpdateRequest request);

    /**
     * 修改标签状态。
     *
     * @param request 状态修改请求
     * @return 是否成功
     */
    boolean updateTagStatus(MallProductTagStatusUpdateRequest request);

    /**
     * 删除标签。
     *
     * @param id 标签ID
     * @return 是否成功
     */
    boolean deleteTag(Long id);

    /**
     * 校验并返回可绑定的启用标签ID集合。
     *
     * @param tagIds 标签ID集合
     * @return 去重后的标签ID集合
     */
    List<Long> normalizeEnabledTagIds(List<Long> tagIds);

    /**
     * 校验并返回商品可保存的标签ID集合。
     * 已绑定且已禁用的标签在未变更时允许继续保存。
     *
     * @param tagIds         商品当前提交的标签ID集合
     * @param existingTagIds 商品原已绑定的标签ID集合
     * @return 去重后的标签ID集合
     */
    List<Long> normalizeProductTagIds(List<Long> tagIds, List<Long> existingTagIds);

    /**
     * 将标签筛选条件按类型拆分到商品查询参数中。
     *
     * @param request 商品查询参数
     */
    void fillTagFilterGroups(MallProductListQueryRequest request);

    /**
     * 按商品ID列表查询标签视图映射，包含已停用标签。
     *
     * @param productIds 商品ID列表
     * @return 商品ID到标签列表的映射
     */
    Map<Long, List<MallProductTagVo>> listTagVoMapByProductIds(List<Long> productIds);

    /**
     * 按商品ID列表查询启用标签视图映射。
     *
     * @param productIds 商品ID列表
     * @return 商品ID到启用标签列表的映射
     */
    Map<Long, List<MallProductTagVo>> listEnabledTagVoMapByProductIds(List<Long> productIds);

}
