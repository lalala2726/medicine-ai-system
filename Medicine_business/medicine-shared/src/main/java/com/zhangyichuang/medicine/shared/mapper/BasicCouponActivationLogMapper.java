package com.zhangyichuang.medicine.shared.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.model.coupon.ActivationRedeemLogQueryDto;
import com.zhangyichuang.medicine.model.coupon.ActivationRedeemLogRowDto;
import com.zhangyichuang.medicine.model.entity.CouponActivationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 基础激活码日志 Mapper。
 */
@Mapper
public interface BasicCouponActivationLogMapper extends BaseMapper<CouponActivationLog> {

    /**
     * 分页查询激活码兑换日志。
     *
     * @param page  分页对象
     * @param query 查询参数
     * @return 激活码兑换日志分页结果
     */
    Page<ActivationRedeemLogRowDto> selectRedeemLogPage(Page<ActivationRedeemLogRowDto> page,
                                                        @Param("query") ActivationRedeemLogQueryDto query);
}
