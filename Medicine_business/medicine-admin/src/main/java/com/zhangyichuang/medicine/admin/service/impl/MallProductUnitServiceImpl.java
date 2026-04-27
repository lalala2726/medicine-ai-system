package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.MallProductUnitMapper;
import com.zhangyichuang.medicine.admin.service.MallProductUnitService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.entity.MallProductUnit;
import com.zhangyichuang.medicine.model.request.MallProductUnitAddRequest;
import com.zhangyichuang.medicine.model.vo.MallProductUnitVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 商品单位服务实现。
 *
 * @author Chuang
 */
@Service
public class MallProductUnitServiceImpl extends ServiceImpl<MallProductUnitMapper, MallProductUnit>
        implements MallProductUnitService, BaseService {

    /**
     * 商品单位名称最大长度。
     */
    private static final int UNIT_NAME_MAX_LENGTH = 20;

    /**
     * 商品单位排序步长。
     */
    private static final int SORT_STEP = 10;

    /**
     * 默认商品单位列表。
     */
    private static final List<String> DEFAULT_UNIT_NAMES = List.of("盒", "瓶", "袋", "支", "包", "片", "贴", "板", "个", "件");

    /**
     * 启动初始化时使用的操作人标记。
     */
    private static final String SYSTEM_OPERATOR = "system";

    /**
     * 查询商品单位下拉选项。
     *
     * @return 商品单位视图列表
     */
    @Override
    public List<MallProductUnitVo> option() {
        return lambdaQuery()
                .orderByAsc(MallProductUnit::getSort, MallProductUnit::getId)
                .list()
                .stream()
                .map(this::toUnitVo)
                .toList();
    }

    /**
     * 新增商品单位。
     *
     * @param request 商品单位新增请求
     * @return 新增后的商品单位视图对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MallProductUnitVo addUnit(MallProductUnitAddRequest request) {
        Assert.notNull(request, "商品单位信息不能为空");
        String normalizedName = normalizeName(request.getName());
        MallProductUnit existingUnit = findExistingUnit(normalizedName);
        Assert.isTrue(existingUnit == null, "商品单位已存在");

        MallProductUnit unit = new MallProductUnit();
        unit.setName(normalizedName);
        unit.setSort(buildNextSort());
        unit.setCreateTime(new Date());
        unit.setCreateBy(resolveOperator());
        try {
            boolean saved = save(unit);
            Assert.isTrue(saved, "新增商品单位失败");
            return toUnitVo(unit);
        } catch (DuplicateKeyException exception) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "商品单位已存在");
        }
    }

    /**
     * 删除商品单位。
     *
     * @param id 商品单位ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUnit(Long id) {
        Assert.isPositive(id, "商品单位ID不能为空");
        MallProductUnit existingUnit = getById(id);
        if (existingUnit == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "商品单位不存在");
        }
        LambdaUpdateWrapper<MallProductUnit> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MallProductUnit::getId, id)
                .set(MallProductUnit::getUpdateTime, new Date())
                .set(MallProductUnit::getUpdateBy, resolveOperator())
                .set(MallProductUnit::getIsDeleted, 1);
        return update(updateWrapper);
    }

    /**
     * 在单位表为空时初始化默认单位。
     *
     * @return 本次是否执行了初始化
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initializeDefaultUnitsIfNeeded() {
        if (count() > 0) {
            return false;
        }
        Date now = new Date();
        String operator = SYSTEM_OPERATOR;
        List<MallProductUnit> defaultUnits = new ArrayList<>();
        for (int index = 0; index < DEFAULT_UNIT_NAMES.size(); index++) {
            MallProductUnit unit = new MallProductUnit();
            unit.setName(DEFAULT_UNIT_NAMES.get(index));
            unit.setSort((index + 1) * SORT_STEP);
            unit.setCreateTime(now);
            unit.setCreateBy(operator);
            defaultUnits.add(unit);
        }
        return saveBatch(defaultUnits);
    }

    /**
     * 归一化商品单位名称。
     *
     * @param name 原始单位名称
     * @return 归一化后的单位名称
     */
    private String normalizeName(String name) {
        Assert.notEmpty(name, "商品单位名称不能为空");
        String normalizedName = name.trim();
        Assert.isParamTrue(StringUtils.hasText(normalizedName), "商品单位名称不能为空");
        Assert.isParamTrue(
                normalizedName.length() <= UNIT_NAME_MAX_LENGTH,
                "商品单位名称不能超过20个字符"
        );
        return normalizedName;
    }

    /**
     * 按名称查找已存在的商品单位。
     *
     * @param normalizedName 归一化后的单位名称
     * @return 已存在的商品单位；不存在时返回 {@code null}
     */
    private MallProductUnit findExistingUnit(String normalizedName) {
        String targetName = normalizedName.toLowerCase(Locale.ROOT);
        return lambdaQuery()
                .list()
                .stream()
                .filter(unit -> normalizeComparableName(unit.getName()).equals(targetName))
                .findFirst()
                .orElse(null);
    }

    /**
     * 计算下一条商品单位的排序值。
     *
     * @return 新增单位应使用的排序值
     */
    private Integer buildNextSort() {
        MallProductUnit lastUnit = lambdaQuery()
                .orderByDesc(MallProductUnit::getSort, MallProductUnit::getId)
                .last("LIMIT 1")
                .one();
        if (lastUnit == null || lastUnit.getSort() == null || lastUnit.getSort() < SORT_STEP) {
            return SORT_STEP;
        }
        return lastUnit.getSort() + SORT_STEP;
    }

    /**
     * 将实体映射为视图对象。
     *
     * @param unit 商品单位实体
     * @return 商品单位视图对象
     */
    private MallProductUnitVo toUnitVo(MallProductUnit unit) {
        MallProductUnitVo unitVo = new MallProductUnitVo();
        unitVo.setId(unit.getId());
        unitVo.setName(unit.getName());
        unitVo.setSort(unit.getSort());
        return unitVo;
    }

    /**
     * 生成用于名称对比的标准化键值。
     *
     * @param name 商品单位名称
     * @return 小写且去除首尾空格后的键值
     */
    private String normalizeComparableName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 解析当前操作人。
     *
     * @return 当前登录用户名；未登录时返回系统标记
     */
    private String resolveOperator() {
        String username = getUsername();
        return StringUtils.hasText(username) ? username : SYSTEM_OPERATOR;
    }
}
