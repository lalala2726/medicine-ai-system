package com.zhangyichuang.medicine.shared.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.model.coupon.ActivationBatchQueryDto;
import com.zhangyichuang.medicine.model.coupon.ActivationBatchRowDto;
import com.zhangyichuang.medicine.model.entity.CouponActivationBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 基础激活码批次 Mapper。
 */
@Mapper
public interface BasicCouponActivationBatchMapper extends BaseMapper<CouponActivationBatch> {

    /**
     * 分页查询激活码批次列表。
     *
     * @param page  分页对象
     * @param query 查询参数
     * @return 激活码批次分页结果
     */
    Page<ActivationBatchRowDto> selectBatchPage(Page<ActivationBatchRowDto> page,
                                                @Param("query") ActivationBatchQueryDto query);

    /**
     * 查询激活码批次详情。
     *
     * @param batchId 批次ID
     * @return 激活码批次详情
     */
    ActivationBatchRowDto selectBatchDetail(@Param("batchId") Long batchId);

    /**
     * 更新激活码批次状态。
     *
     * @param batchId    批次ID
     * @param status     目标状态
     * @param operatorId 操作人标识
     * @param updateTime 更新时间
     * @return 更新条数
     */
    int updateBatchStatus(@Param("batchId") Long batchId,
                          @Param("status") String status,
                          @Param("operatorId") String operatorId,
                          @Param("updateTime") java.util.Date updateTime);

    /**
     * 累加批次成功使用次数。
     *
     * @param batchId    批次ID
     * @param operatorId 操作人标识
     * @param updateTime 更新时间
     * @return 更新条数
     */
    int increaseSuccessUseCount(@Param("batchId") Long batchId,
                                @Param("operatorId") String operatorId,
                                @Param("updateTime") java.util.Date updateTime);

    /**
     * 软删除激活码批次。
     *
     * @param batchId    批次ID
     * @param operatorId 操作人标识
     * @param updateTime 更新时间
     * @return 更新条数
     */
    int softDeleteBatchById(@Param("batchId") Long batchId,
                            @Param("operatorId") String operatorId,
                            @Param("updateTime") java.util.Date updateTime);
}
