package com.zhangyichuang.medicine.shared.service;

import com.zhangyichuang.medicine.shared.entity.Region;

import java.util.List;

/**
 * 地址区域服务接口
 *
 * @author Chuang
 */
public interface RegionService {

    /**
     * 获取所有省份
     *
     * @return 省份列表
     */
    List<Region> getProvinces();

    /**
     * 根据省份ID获取城市列表
     *
     * @param provinceId 省份ID
     * @return 城市列表
     */
    List<Region> getCitiesByProvinceId(String provinceId);

    /**
     * 根据城市ID获取区县列表
     *
     * @param cityId 城市ID
     * @return 区县列表
     */
    List<Region> getDistrictsByCityId(String cityId);

    /**
     * 根据区县ID获取街道列表
     *
     * @param districtId 区县ID
     * @return 街道列表
     */
    List<Region> getStreetsByDistrictId(String districtId);

    /**
     * 根据父ID获取子级区域列表(通用方法)
     *
     * @param parentId 父级ID
     * @return 子级区域列表
     */
    List<Region> getChildrenByParentId(String parentId);

    /**
     * 根据ID获取单个区域
     *
     * @param id 区域ID
     * @return 区域信息,不存在返回null
     */
    Region getRegionById(String id);

    /**
     * 获取完整路径
     * 返回从省到当前区域的完整名称路径
     *
     * @param id 区域ID
     * @return 路径名称列表,例如: ["北京市", "朝阳区", "东花市街道"]
     */
    List<String> getFullPath(String id);

    /**
     * 按名称搜索区域
     *
     * @param keyword 关键词
     * @return 匹配的区域列表
     */
    List<Region> searchByName(String keyword);

    /**
     * 按拼音搜索区域
     *
     * @param pinyin 拼音关键词
     * @return 匹配的区域列表
     */
    List<Region> searchByPinyin(String pinyin);

    /**
     * 按拼音首字母搜索区域
     *
     * @param prefix 拼音首字母
     * @return 匹配的区域列表
     */
    List<Region> searchByPinyinPrefix(String prefix);
}
