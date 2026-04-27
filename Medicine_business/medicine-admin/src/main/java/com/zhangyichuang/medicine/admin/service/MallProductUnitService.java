package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.MallProductUnit;
import com.zhangyichuang.medicine.model.request.MallProductUnitAddRequest;
import com.zhangyichuang.medicine.model.vo.MallProductUnitVo;

import java.util.List;

/**
 * 商品单位服务。
 *
 * @author Chuang
 */
public interface MallProductUnitService extends IService<MallProductUnit> {

    /**
     * 查询启用的商品单位下拉选项。
     *
     * @return 商品单位下拉选项列表
     */
    List<MallProductUnitVo> option();

    /**
     * 新增商品单位。
     *
     * @param request 商品单位新增请求
     * @return 新增后的商品单位视图对象
     */
    MallProductUnitVo addUnit(MallProductUnitAddRequest request);

    /**
     * 删除商品单位。
     *
     * @param id 商品单位ID
     * @return 是否删除成功
     */
    boolean deleteUnit(Long id);

    /**
     * 在单位表为空时初始化默认单位。
     *
     * @return 本次是否执行了初始化
     */
    boolean initializeDefaultUnitsIfNeeded();
}
