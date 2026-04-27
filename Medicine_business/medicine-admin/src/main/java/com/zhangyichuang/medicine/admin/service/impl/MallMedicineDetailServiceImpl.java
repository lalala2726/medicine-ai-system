package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.MallMedicineDetailMapper;
import com.zhangyichuang.medicine.admin.service.MallMedicineDetailService;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.model.entity.DrugDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 药品详情服务实现类
 *
 * @author Chuang
 */
@Service
@RequiredArgsConstructor
public class MallMedicineDetailServiceImpl extends ServiceImpl<MallMedicineDetailMapper, DrugDetail>
        implements MallMedicineDetailService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addMedicineDetail(DrugDetail drugDetail) {
        if (drugDetail == null) {
            throw new ServiceException("药品详情不能为空");
        }
        if (drugDetail.getProductId() == null) {
            throw new ServiceException("商品ID不能为空");
        }

        // 检查是否已存在该商品的药品详情
        DrugDetail existingDetail = lambdaQuery()
                .eq(DrugDetail::getProductId, drugDetail.getProductId())
                .one();

        if (existingDetail != null) {
            // 如果已存在，更新现有记录
            drugDetail.setId(existingDetail.getId());
            drugDetail.setUpdateTime(new Date());
            return updateById(drugDetail);
        }

        // 不存在则新增
        drugDetail.setCreateTime(new Date());
        return save(drugDetail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMedicineDetail(DrugDetail drugDetail) {
        if (drugDetail == null) {
            throw new ServiceException("药品详情不能为空");
        }
        if (drugDetail.getProductId() == null) {
            throw new ServiceException("商品ID不能为空");
        }

        // 查询现有的药品详情
        DrugDetail existingDetail = lambdaQuery()
                .eq(DrugDetail::getProductId, drugDetail.getProductId())
                .one();

        if (existingDetail == null) {
            // 不存在则新增
            addMedicineDetail(drugDetail);
            return;
        }

        // 更新记录
        drugDetail.setId(existingDetail.getId());
        drugDetail.setUpdateTime(new Date());
        updateById(drugDetail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMedicineDetailByProductId(Long productId) {
        if (productId == null) {
            throw new ServiceException("商品ID不能为空");
        }

        return lambdaUpdate()
                .eq(DrugDetail::getProductId, productId)
                .remove();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMedicineDetailByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        lambdaUpdate()
                .in(DrugDetail::getProductId, productIds)
                .remove();
    }
}
