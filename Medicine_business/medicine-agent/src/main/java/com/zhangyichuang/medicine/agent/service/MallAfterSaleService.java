package com.zhangyichuang.medicine.agent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.model.dto.AfterSaleContextDto;
import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import com.zhangyichuang.medicine.model.request.MallAfterSaleListRequest;

import java.util.List;
import java.util.Map;

/**
 * 智能体售后服务接口。
 */
public interface MallAfterSaleService {

    /**
     * 分页查询售后列表。
     * <p>
     * 功能描述：查询管理端智能体可见的售后列表分页数据。
     *
     * @param request 查询参数，包含分页信息与筛选条件
     * @return 返回售后分页数据，记录元素类型为 {@link MallAfterSaleListDto}
     * @throws RuntimeException 异常说明：当 RPC 调用失败或参数校验异常时抛出运行时异常
     */
    Page<MallAfterSaleListDto> listAfterSales(MallAfterSaleListRequest request);

    /**
     * 根据售后单号批量查询售后详情。
     *
     * @param afterSaleNos 售后单号列表
     * @return 售后详情 DTO 列表
     */
    List<AfterSaleDetailDto> getAfterSaleDetails(List<String> afterSaleNos);

    /**
     * 根据售后单号批量查询智能体售后上下文。
     *
     * @param afterSaleNos 售后单号列表
     * @return 按售后单号分组的售后上下文
     */
    Map<String, AfterSaleContextDto> getAfterSaleContexts(List<String> afterSaleNos);
}
