package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangyichuang.medicine.model.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 用户优惠券 Mapper。
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 按ID集合批量将可用用户券更新为已过期状态。
     *
     * @param couponIds        用户券ID集合
     * @param fromStatus       原始状态
     * @param toStatus         目标状态
     * @param now              当前时间
     * @param updateOperatorId 更新操作人标识
     * @return 更新成功条数
     */
    int batchExpireCouponsByIds(@Param("couponIds") List<Long> couponIds,
                                @Param("fromStatus") String fromStatus,
                                @Param("toStatus") String toStatus,
                                @Param("now") Date now,
                                @Param("updateOperatorId") String updateOperatorId);
}
