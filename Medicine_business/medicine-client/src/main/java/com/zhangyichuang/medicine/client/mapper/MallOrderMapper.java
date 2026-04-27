package com.zhangyichuang.medicine.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.client.model.dto.MallOrderDto;
import com.zhangyichuang.medicine.client.model.dto.OrderDetailDto;
import com.zhangyichuang.medicine.client.model.request.OrderListRequest;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import org.apache.ibatis.annotations.Param;

/**
 * @author Chuang
 */
public interface MallOrderMapper extends BaseMapper<MallOrder> {

    /**
     * 分页查询用户订单列表
     *
     * @param page    分页对象
     * @param request 查询条件
     * @param userId  用户ID
     * @return 订单列表DTO
     */
    Page<MallOrderDto> selectOrderList(@Param("page") Page<MallOrderDto> page,
                                       @Param("request") OrderListRequest request,
                                       @Param("userId") Long userId);

    /**
     * 查询订单详情
     *
     * @param orderNo 订单编号
     * @param userId  用户ID
     * @return 订单详情
     */
    OrderDetailDto getOrderDetailByOrderNo(@Param("orderNo") String orderNo,
                                           @Param("userId") Long userId);
}




