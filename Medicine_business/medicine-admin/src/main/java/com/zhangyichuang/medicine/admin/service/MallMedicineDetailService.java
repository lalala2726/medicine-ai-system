package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.DrugDetail;

import java.util.List;

/**
 * 药品详情服务接口
 *
 * @author Chuang
 */
public interface MallMedicineDetailService extends IService<DrugDetail> {

    /**
     * 添加药品详情
     *
     * @param drugDetail 药品详情
     * @return 添加结果
     */
    boolean addMedicineDetail(DrugDetail drugDetail);

    /**
     * 更新药品详情
     *
     * @param drugDetail 药品详情
     */
    void updateMedicineDetail(DrugDetail drugDetail);

    /**
     * 根据商品ID删除药品详情
     *
     * @param productId 商品ID
     * @return 删除结果
     */
    boolean deleteMedicineDetailByProductId(Long productId);

    /**
     * 批量删除药品详情
     *
     * @param productIds 商品ID列表
     */
    void deleteMedicineDetailByProductIds(List<Long> productIds);
}
