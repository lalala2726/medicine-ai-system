package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleAuditRequest;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleListRequest;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleProcessRequest;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.vo.AfterSaleDetailVo;

/**
 * 售后申请Service(管理端)
 *
 * @author Chuang
 * created 2025/11/08
 */
public interface MallAfterSaleService extends IService<MallAfterSale> {

    /**
     * 查询售后列表(管理端)
     * <p>
     * 功能描述：根据查询条件分页查询售后申请列表。
     *
     * @param request 查询条件对象，包含分页参数与售后筛选条件
     * @return 返回售后分页结果，记录元素类型为 {@link MallAfterSaleListDto}
     * @throws RuntimeException 异常说明：当查询参数异常或底层数据访问失败时抛出运行时异常
     */
    Page<MallAfterSaleListDto> getAfterSaleList(AfterSaleListRequest request);

    /**
     * 查询售后详情(管理端)
     *
     * @param afterSaleId 售后申请ID
     * @return 售后详情
     */
    AfterSaleDetailVo getAfterSaleDetail(Long afterSaleId);

    /**
     * 审核售后申请
     *
     * @param request 审核请求
     * @return 是否审核成功
     */
    boolean auditAfterSale(AfterSaleAuditRequest request);

    /**
     * 处理售后退款
     *
     * @param request 处理请求
     * @return 是否处理成功
     */
    boolean processRefund(AfterSaleProcessRequest request);

    /**
     * 处理换货
     *
     * @param request 处理请求
     * @return 是否处理成功
     */
    boolean processExchange(AfterSaleProcessRequest request);
}
