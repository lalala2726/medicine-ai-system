package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.client.model.request.MallProductSearchRequest;
import com.zhangyichuang.medicine.client.model.vo.MallProductSearchTagFilterVo;
import com.zhangyichuang.medicine.model.entity.MallProductTag;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;

import java.util.List;
import java.util.Map;

/**
 * 商品标签服务（客户端）。
 *
 * @author Chuang
 */
public interface MallProductTagService extends IService<MallProductTag> {

    /**
     * 为搜索请求补充按类型分组后的标签筛选条件。
     *
     * @param request 搜索请求
     */
    void fillSearchTagGroups(MallProductSearchRequest request);

    /**
     * 查询商品ID与启用标签列表的映射。
     *
     * @param productIds 商品ID列表
     * @return 商品ID到标签列表的映射
     */
    Map<Long, List<MallProductTagVo>> listEnabledTagVoMapByProductIds(List<Long> productIds);

    /**
     * 根据 ES 聚合结果构建搜索标签筛选列表。
     *
     * @param tagBindingCountMap 标签类型绑定与命中数量映射
     * @return 搜索标签筛选列表
     */
    List<MallProductSearchTagFilterVo> buildSearchTagFilters(Map<String, Long> tagBindingCountMap);

    /**
     * 查询数据库中全部启用的商品标签筛选分组。
     *
     * @return 按标签类型分组后的筛选项列表
     */
    List<MallProductSearchTagFilterVo> listAllEnabledSearchTagFilters();
}
