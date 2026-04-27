package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangyichuang.medicine.model.entity.CouponLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 优惠券日志 Mapper。
 */
@Mapper
public interface CouponLogMapper extends BaseMapper<CouponLog> {

    /**
     * 按用户券ID集合批量写入系统过期日志。
     *
     * @param couponIds  用户券ID集合
     * @param fromStatus 过滤状态
     * @param operatorId 操作人标识
     * @param changeType 变更类型
     * @param sourceType 来源类型
     * @param remark     备注
     * @param createTime 创建时间
     * @return 插入成功条数
     */
    int batchInsertExpireLogsByCouponIds(@Param("couponIds") List<Long> couponIds,
                                         @Param("fromStatus") String fromStatus,
                                         @Param("operatorId") String operatorId,
                                         @Param("changeType") String changeType,
                                         @Param("sourceType") String sourceType,
                                         @Param("remark") String remark,
                                         @Param("createTime") Date createTime);
}
