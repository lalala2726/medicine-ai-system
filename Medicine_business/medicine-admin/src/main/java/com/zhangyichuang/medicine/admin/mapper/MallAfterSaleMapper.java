package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleListRequest;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 售后申请Mapper
 *
 * @author Chuang
 * created 2025/11/08
 */
@Mapper
public interface MallAfterSaleMapper extends BaseMapper<MallAfterSale> {

    /**
     * 分页查询售后列表(管理端)
     * <p>
     * 功能描述：分页查询售后申请列表，并返回包含联表字段的 DTO 结果。
     *
     * @param page    分页对象，类型为 {@link Page}{@code <MallAfterSaleListDto>}，包含分页参数与承载分页结果
     * @param request 查询条件对象，包含售后类型、售后状态、订单号、用户ID、申请原因等筛选条件
     * @return 返回售后申请分页结果，记录元素类型为 {@link MallAfterSaleListDto}
     * @throws RuntimeException 异常说明：当 SQL 执行异常或参数绑定失败时由 MyBatis 抛出运行时异常
     */
    Page<MallAfterSaleListDto> selectAfterSaleList(@Param("page") Page<MallAfterSaleListDto> page,
                                                   @Param("request") AfterSaleListRequest request);
}
