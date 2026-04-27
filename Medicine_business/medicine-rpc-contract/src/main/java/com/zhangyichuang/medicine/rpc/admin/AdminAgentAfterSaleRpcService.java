package com.zhangyichuang.medicine.rpc.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.model.dto.AfterSaleContextDto;
import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import com.zhangyichuang.medicine.model.request.MallAfterSaleListRequest;

import java.util.List;
import java.util.Map;

/**
 * 管理端 Agent 售后只读 RPC。
 */
public interface AdminAgentAfterSaleRpcService {

    /**
     * 分页查询售后列表。
     *
     * @param query 售后查询参数
     * @return 售后分页结果
     */
    Page<MallAfterSaleListDto> listAfterSales(MallAfterSaleListRequest query);

    /**
     * 根据售后单号列表批量查询售后详情。
     *
     * @param afterSaleNos 售后单号列表
     * @return 售后详情列表
     */
    List<AfterSaleDetailDto> getAfterSaleDetailsByAfterSaleNos(List<String> afterSaleNos);

    /**
     * 根据售后单号列表批量查询智能体售后上下文。
     *
     * @param afterSaleNos 售后单号列表
     * @return 按售后单号分组的售后上下文
     */
    Map<String, AfterSaleContextDto> getAfterSaleContextsByAfterSaleNos(List<String> afterSaleNos);
}
