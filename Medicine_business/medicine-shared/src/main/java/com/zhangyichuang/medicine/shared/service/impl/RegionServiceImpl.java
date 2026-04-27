package com.zhangyichuang.medicine.shared.service.impl;

import com.zhangyichuang.medicine.shared.constants.MongoCollections;
import com.zhangyichuang.medicine.shared.entity.Region;
import com.zhangyichuang.medicine.shared.enums.RegionLevel;
import com.zhangyichuang.medicine.shared.mongodb.MongoCache;
import com.zhangyichuang.medicine.shared.service.RegionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 地址区域服务实现类
 *
 * @author Chuang
 */
@Service
public class RegionServiceImpl implements RegionService {

    private static final Logger log = LoggerFactory.getLogger(RegionServiceImpl.class);

    private final MongoCache mongoCache;

    public RegionServiceImpl(MongoCache mongoCache) {
        this.mongoCache = mongoCache;
    }

    @Override
    public List<Region> getProvinces() {
        Query query = new Query(Criteria.where("level").is(RegionLevel.PROVINCE.getCode()));
        return mongoCache.find(query, Region.class, MongoCollections.REGIONS);
    }

    @Override
    public List<Region> getCitiesByProvinceId(String provinceId) {
        if (!StringUtils.hasText(provinceId)) {
            return Collections.emptyList();
        }
        Query query = new Query(Criteria.where("parent_id").is(provinceId)
                .and("level").is(RegionLevel.CITY.getCode()));
        return mongoCache.find(query, Region.class, MongoCollections.REGIONS);
    }

    @Override
    public List<Region> getDistrictsByCityId(String cityId) {
        if (!StringUtils.hasText(cityId)) {
            return Collections.emptyList();
        }
        Query query = new Query(Criteria.where("parent_id").is(cityId)
                .and("level").is(RegionLevel.DISTRICT.getCode()));
        return mongoCache.find(query, Region.class, MongoCollections.REGIONS);
    }

    @Override
    public List<Region> getStreetsByDistrictId(String districtId) {
        if (!StringUtils.hasText(districtId)) {
            return Collections.emptyList();
        }
        Query query = new Query(Criteria.where("parent_id").is(districtId)
                .and("level").is(RegionLevel.STREET.getCode()));
        return mongoCache.find(query, Region.class, MongoCollections.REGIONS);
    }

    @Override
    public List<Region> getChildrenByParentId(String parentId) {
        if (!StringUtils.hasText(parentId)) {
            return Collections.emptyList();
        }
        Query query = new Query(Criteria.where("parent_id").is(parentId));
        return mongoCache.find(query, Region.class, MongoCollections.REGIONS);
    }

    @Override
    public Region getRegionById(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        Query query = new Query(Criteria.where("id").is(id));
        return mongoCache.findOne(query, Region.class, MongoCollections.REGIONS);
    }

    @Override
    public List<String> getFullPath(String id) {
        List<String> path = new ArrayList<>();

        if (!StringUtils.hasText(id)) {
            return path;
        }

        Region current = getRegionById(id);
        if (current == null) {
            return path;
        }

        // 从当前节点向上遍历到根节点
        while (current != null) {
            // 添加到列表开头
            path.addFirst(current.getName());

            // 如果是省级(parent_id为"0")或没有父节点,停止遍历
            if ("0".equals(current.getParentId()) || !StringUtils.hasText(current.getParentId())) {
                break;
            }

            // 获取父节点
            current = getRegionById(current.getParentId());
        }

        return path;
    }

    @Override
    public List<Region> searchByName(String keyword) {
        log.info("Searching regions by name: {}", keyword);
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        // 使用正则表达式进行模糊匹配
        Pattern pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
        Query query = new Query(Criteria.where("name").regex(pattern));
        // 限制返回数量
        query.limit(100);

        return mongoCache.find(query, Region.class, MongoCollections.REGIONS);
    }

    @Override
    public List<Region> searchByPinyin(String pinyin) {
        if (!StringUtils.hasText(pinyin)) {
            return Collections.emptyList();
        }

        // 使用正则表达式进行模糊匹配
        Pattern pattern = Pattern.compile(pinyin, Pattern.CASE_INSENSITIVE);
        Query query = new Query(Criteria.where("pinyin").regex(pattern));
        // 限制返回数量
        query.limit(100);

        return mongoCache.find(query, Region.class, MongoCollections.REGIONS);
    }

    @Override
    public List<Region> searchByPinyinPrefix(String prefix) {
        log.info("Searching regions by pinyin prefix: {}", prefix);
        if (!StringUtils.hasText(prefix)) {
            return Collections.emptyList();
        }

        // 转换为大写进行匹配
        String upperPrefix = prefix.toUpperCase();
        Query query = new Query(Criteria.where("pinyin_prefix").is(upperPrefix));
        query.limit(100); // 限制返回数量

        return mongoCache.find(query, Region.class, MongoCollections.REGIONS);
    }
}
