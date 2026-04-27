package com.zhangyichuang.medicine.shared.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangyichuang.medicine.model.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;

/**
 * 基础用户优惠券 Mapper。
 */
@Mapper
public interface BasicUserCouponMapper extends BaseMapper<UserCoupon> {
}
