package com.zhangyichuang.medicine.shared.controller;

import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.shared.entity.Region;
import com.zhangyichuang.medicine.shared.service.RegionService;
import com.zhangyichuang.medicine.shared.vo.RegionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 地址管理 Controller。
 * 提供省市区街道地址查询功能。
 *
 * @author Chuang
 */
@Tag(name = "地址管理", description = "省市区街道地址查询接口")
@RestController
@RequestMapping("/common/regions")
@RequiredArgsConstructor
public class RegionController extends BaseController {

    /**
     * 区域服务。
     */
    private final RegionService regionService;

    /**
     * 获取所有省份。
     *
     * @return 省份列表
     */
    @Operation(summary = "获取所有省份", description = "获取全国所有省级行政区")
    @GetMapping("/provinces")
    public AjaxResult<List<RegionVo>> getProvinces() {
        List<Region> provinces = regionService.getProvinces();
        List<RegionVo> regionVos = BeanCotyUtils.copyListProperties(provinces, RegionVo.class);
        return success(regionVos);
    }

    /**
     * 根据省份ID获取城市列表。
     *
     * @param provinceId 省份ID
     * @return 城市列表
     */
    @Operation(summary = "获取城市列表", description = "根据省份ID获取该省下的所有城市")
    @GetMapping("/cities/{provinceId}")
    public AjaxResult<List<RegionVo>> getCities(
            @Parameter(description = "省份ID", required = true)
            @PathVariable String provinceId) {
        List<Region> cities = regionService.getCitiesByProvinceId(provinceId);
        List<RegionVo> regionVos = BeanCotyUtils.copyListProperties(cities, RegionVo.class);
        return success(regionVos);
    }

    /**
     * 根据城市ID获取区县列表。
     *
     * @param cityId 城市ID
     * @return 区县列表
     */
    @Operation(summary = "获取区县列表", description = "根据城市ID获取该市下的所有区县")
    @GetMapping("/districts/{cityId}")
    public AjaxResult<List<RegionVo>> getDistricts(
            @Parameter(description = "城市ID", required = true)
            @PathVariable String cityId) {
        List<Region> districts = regionService.getDistrictsByCityId(cityId);
        List<RegionVo> regionVos = BeanCotyUtils.copyListProperties(districts, RegionVo.class);
        return success(regionVos);
    }

    /**
     * 根据区县ID获取街道列表。
     *
     * @param districtId 区县ID
     * @return 街道列表
     */
    @Operation(summary = "获取街道列表", description = "根据区县ID获取该区下的所有街道")
    @GetMapping("/streets/{districtId}")
    public AjaxResult<List<RegionVo>> getStreets(
            @Parameter(description = "区县ID", required = true)
            @PathVariable String districtId) {
        List<Region> streets = regionService.getStreetsByDistrictId(districtId);
        List<RegionVo> regionVos = BeanCotyUtils.copyListProperties(streets, RegionVo.class);
        return success(regionVos);
    }

    /**
     * 根据父ID获取子级区域(通用接口)。
     * parentId为"0"时返回所有省份。
     *
     * @param parentId 父级ID
     * @return 子级区域列表
     */
    @Operation(summary = "获取子级区域", description = "根据父级ID获取下一级所有区域，parentId为\"0\"时返回所有省份")
    @GetMapping("/children")
    public AjaxResult<List<RegionVo>> getChildrenByParentId(
            @Parameter(description = "父级ID，\"0\"表示获取省份", required = true)
            @RequestParam String parentId) {

        List<Region> regions;
        if ("0".equals(parentId)) {
            regions = regionService.getProvinces();
        } else {
            regions = regionService.getChildrenByParentId(parentId);
        }

        List<RegionVo> regionVos = BeanCotyUtils.copyListProperties(regions, RegionVo.class);
        return success(regionVos);
    }

    /**
     * 根据父ID获取子级区域(路径参数版本，保持兼容)。
     *
     * @param parentId 父级ID
     * @return 子级区域列表
     */
    @Operation(summary = "获取子级区域", description = "根据父级ID获取下一级所有区域")
    @GetMapping("/children/{parentId}")
    public AjaxResult<List<RegionVo>> getChildren(
            @Parameter(description = "父级ID", required = true)
            @PathVariable String parentId) {
        List<Region> children = regionService.getChildrenByParentId(parentId);
        List<RegionVo> regionVos = BeanCotyUtils.copyListProperties(children, RegionVo.class);
        return success(regionVos);
    }

    /**
     * 根据ID获取单个区域详情。
     *
     * @param id 区域ID
     * @return 区域详情
     */
    @Operation(summary = "获取区域详情", description = "根据区域ID获取详细信息")
    @GetMapping("/{id}")
    public AjaxResult<RegionVo> getRegionById(
            @Parameter(description = "区域ID", required = true)
            @PathVariable String id) {
        Region region = regionService.getRegionById(id);
        if (region == null) {
            return error("区域不存在");
        }
        RegionVo regionVo = BeanCotyUtils.copyProperties(region, RegionVo.class);
        return success(regionVo);
    }

    /**
     * 获取完整路径。
     *
     * @param id 区域ID
     * @return 完整路径
     */
    @Operation(summary = "获取完整路径", description = "获取从省到当前区域的完整路径")
    @GetMapping("/{id}/path")
    public AjaxResult<List<String>> getFullPath(
            @Parameter(description = "区域ID", required = true)
            @PathVariable String id) {
        List<String> path = regionService.getFullPath(id);
        return success(path);
    }

    /**
     * 搜索地址（合并搜索版本）。
     * 支持地名和拼音搜索，自动合并结果去重。
     *
     * @param keyword 搜索关键词
     * @return 区域列表
     */
    @Operation(summary = "搜索地址", description = "支持地名和拼音搜索，自动合并结果去重")
    @GetMapping("/search")
    public AjaxResult<List<RegionVo>> search(
            @Parameter(description = "搜索关键词，支持地名或拼音", required = true)
            @RequestParam String keyword) {

        if (!StringUtils.hasText(keyword)) {
            return error("搜索关键词不能为空");
        }

        keyword = keyword.trim();
        Set<String> regionIds = new HashSet<>();
        List<Region> results = new ArrayList<>();

        List<Region> nameResults = regionService.searchByName(keyword);
        for (Region region : nameResults) {
            if (regionIds.add(region.getId())) {
                results.add(region);
            }
        }

        List<Region> pinyinResults = regionService.searchByPinyin(keyword);
        for (Region region : pinyinResults) {
            if (regionIds.add(region.getId())) {
                results.add(region);
            }
        }

        if (keyword.length() == 1 && keyword.matches("[a-zA-Z]")) {
            List<Region> prefixResults = regionService.searchByPinyinPrefix(keyword);
            for (Region region : prefixResults) {
                if (regionIds.add(region.getId())) {
                    results.add(region);
                }
            }
        }

        List<RegionVo> regionVos = BeanCotyUtils.copyListProperties(results, RegionVo.class);
        return success(regionVos);
    }

    /**
     * 搜索地址（分开搜索版本，保持兼容）。
     * 支持按名称、拼音、拼音首字母分别搜索。
     *
     * @param name   名称关键词
     * @param pinyin 拼音关键词
     * @param prefix 拼音首字母
     * @return 区域列表
     */
    @Operation(summary = "搜索地址（分开搜索）", description = "支持按名称、拼音、拼音首字母分别搜索地址")
    @GetMapping("/search/separate")
    public AjaxResult<List<RegionVo>> searchSeparate(
            @Parameter(description = "名称关键词")
            @RequestParam(required = false) String name,
            @Parameter(description = "拼音关键词")
            @RequestParam(required = false) String pinyin,
            @Parameter(description = "拼音首字母")
            @RequestParam(required = false) String prefix) {

        List<Region> results;

        if (name != null && !name.trim().isEmpty()) {
            results = regionService.searchByName(name.trim());
        } else if (pinyin != null && !pinyin.trim().isEmpty()) {
            results = regionService.searchByPinyin(pinyin.trim());
        } else if (prefix != null && !prefix.trim().isEmpty()) {
            results = regionService.searchByPinyinPrefix(prefix.trim());
        } else {
            return error("请提供搜索关键词(name/pinyin/prefix)");
        }
        List<RegionVo> regionVos = BeanCotyUtils.copyListProperties(results, RegionVo.class);
        return success(regionVos);
    }
}
