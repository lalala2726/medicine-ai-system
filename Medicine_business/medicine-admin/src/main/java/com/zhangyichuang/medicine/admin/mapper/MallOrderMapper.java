package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.dto.OrderDetailRow;
import com.zhangyichuang.medicine.admin.model.dto.OrderOverviewStats;
import com.zhangyichuang.medicine.model.dto.OrderWithProductDto;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.request.MallOrderListRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Chuang
 */
public interface MallOrderMapper extends BaseMapper<MallOrder> {

    /**
     * 订单列表
     *
     * @param request 订单列表参数
     * @return 订单列表
     */
    Page<MallOrder> orderList(Page<MallOrder> mallOrderPage, @Param("request") MallOrderListRequest request);


    /**
     * 订单列表（带商品信息）
     *
     * @param request 订单列表参数
     * @return 订单列表
     */
    Page<OrderWithProductDto> orderListWithProduct(Page<OrderWithProductDto> orderWithProductDtoPage, @Param("request") MallOrderListRequest request);

    /**
     * 获取过期的订单
     *
     * @param expiredTime 过期时间, 单位毫秒,小于此时间则视为过期
     * @return 过期的订单
     */
    List<MallOrder> getExpiredOrderClean(long expiredTime);

    /**
     * 获取用户已付款的订单
     *
     * @param userId 用户id
     * @return 用户已付款的订单
     */
    Page<MallOrder> getPaidOrderPage(Page<MallOrder> page, @Param("userId") Long userId);

    /**
     * 聚合订单概况统计数据。
     */
    OrderOverviewStats getOrderOverviewStats();

    /**
     * 根据订单号列表批量查询订单详情行数据。
     */
    List<OrderDetailRow> selectOrderDetailRowsByOrderNos(@Param("orderNos") List<String> orderNos);
}

