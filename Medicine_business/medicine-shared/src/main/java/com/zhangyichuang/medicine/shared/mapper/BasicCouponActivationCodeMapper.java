package com.zhangyichuang.medicine.shared.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.model.coupon.ActivationCodeRowDto;
import com.zhangyichuang.medicine.model.coupon.ActivationRedeemCodeDto;
import com.zhangyichuang.medicine.model.entity.CouponActivationCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 基础激活码 Mapper。
 */
@Mapper
public interface BasicCouponActivationCodeMapper extends BaseMapper<CouponActivationCode> {

    /**
     * 根据批次ID查询激活码明细列表。
     *
     * @param batchId 批次ID
     * @return 激活码明细列表
     */
    List<ActivationCodeRowDto> selectCodesByBatchId(@Param("batchId") Long batchId);

    /**
     * 根据批次ID分页查询激活码明细列表。
     *
     * @param page    分页参数
     * @param batchId 批次ID
     * @return 激活码明细分页结果
     */
    Page<ActivationCodeRowDto> selectCodePageByBatchId(Page<ActivationCodeRowDto> page,
                                                       @Param("batchId") Long batchId);

    /**
     * 根据哈希值查询兑换用激活码信息。
     *
     * @param codeHash 激活码哈希值
     * @return 兑换用激活码信息
     */
    ActivationRedeemCodeDto selectRedeemCodeByHash(@Param("codeHash") String codeHash);

    /**
     * 批量插入激活码，若存在唯一键冲突则忽略冲突记录。
     *
     * @param codes 激活码实体列表
     * @return 实际插入条数
     */
    int batchInsertIgnore(@Param("codes") List<CouponActivationCode> codes);

    /**
     * 标记唯一码为已使用并累加成功次数。
     *
     * @param codeId     激活码ID
     * @param operatorId 操作人标识
     * @param updateTime 更新时间
     * @return 更新条数
     */
    int markUniqueCodeUsed(@Param("codeId") Long codeId,
                           @Param("operatorId") String operatorId,
                           @Param("updateTime") Date updateTime);

    /**
     * 累加激活码成功使用次数。
     *
     * @param codeId     激活码ID
     * @param operatorId 操作人标识
     * @param updateTime 更新时间
     * @return 更新条数
     */
    int increaseSuccessUseCount(@Param("codeId") Long codeId,
                                @Param("operatorId") String operatorId,
                                @Param("updateTime") Date updateTime);

    /**
     * 按批次批量更新单码状态。
     *
     * @param batchId    批次ID
     * @param fromStatus 原状态
     * @param toStatus   目标状态
     * @param operatorId 操作人标识
     * @param updateTime 更新时间
     * @return 更新条数
     */
    int updateCodeStatusByBatchId(@Param("batchId") Long batchId,
                                  @Param("fromStatus") String fromStatus,
                                  @Param("toStatus") String toStatus,
                                  @Param("operatorId") String operatorId,
                                  @Param("updateTime") Date updateTime);

    /**
     * 按ID更新单码状态。
     *
     * @param codeId     激活码ID
     * @param fromStatus 原状态
     * @param toStatus   目标状态
     * @param operatorId 操作人标识
     * @param updateTime 更新时间
     * @return 更新条数
     */
    int updateCodeStatusById(@Param("codeId") Long codeId,
                             @Param("fromStatus") String fromStatus,
                             @Param("toStatus") String toStatus,
                             @Param("operatorId") String operatorId,
                             @Param("updateTime") Date updateTime);

    /**
     * 软删除单个激活码。
     *
     * @param codeId     激活码ID
     * @param operatorId 操作人标识
     * @param updateTime 更新时间
     * @return 更新条数
     */
    int softDeleteCodeById(@Param("codeId") Long codeId,
                           @Param("operatorId") String operatorId,
                           @Param("updateTime") Date updateTime);

    /**
     * 按批次软删除激活码。
     *
     * @param batchId    批次ID
     * @param operatorId 操作人标识
     * @param updateTime 更新时间
     * @return 更新条数
     */
    int softDeleteCodeByBatchId(@Param("batchId") Long batchId,
                                @Param("operatorId") String operatorId,
                                @Param("updateTime") Date updateTime);
}
