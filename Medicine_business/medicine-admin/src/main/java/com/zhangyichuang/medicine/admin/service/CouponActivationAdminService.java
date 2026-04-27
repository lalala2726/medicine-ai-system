package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 管理端激活码服务。
 */
public interface CouponActivationAdminService {

    /**
     * 生成激活码。
     *
     * @param request 生成请求
     * @return 生成结果
     */
    ActivationCodeGenerateResultVo generateActivationCodes(ActivationCodeGenerateRequest request);

    /**
     * 查询激活码批次详情。
     *
     * @param id 激活码批次ID
     * @return 激活码批次详情
     */
    ActivationCodeDetailVo getActivationCodeDetail(Long id);

    /**
     * 查询激活码批次列表。
     *
     * @param request 查询请求
     * @return 激活码批次分页结果
     */
    Page<ActivationCodeVo> listActivationCodes(ActivationCodeListRequest request);

    /**
     * 查询批次下的全部激活码。
     *
     * @param batchId 批次ID
     * @param request 分页查询请求
     * @return 激活码明细分页结果
     */
    Page<ActivationCodeGeneratedItemVo> listActivationBatchCodes(Long batchId, ActivationBatchCodeListRequest request);

    /**
     * 导出批次下的全部激活码。
     *
     * @param batchId  批次ID
     * @param response Http 响应对象
     * @return 无返回值
     * @throws IOException IO 异常
     */
    void exportActivationBatchCodes(Long batchId, HttpServletResponse response) throws IOException;

    /**
     * 更新激活码批次状态。
     *
     * @param request 状态更新请求
     * @return 是否更新成功
     */
    boolean updateActivationCodeStatus(ActivationCodeStatusUpdateRequest request);

    /**
     * 删除激活码批次。
     *
     * @param id 激活码批次ID
     * @return 是否删除成功
     */
    boolean deleteActivationBatch(Long id);

    /**
     * 更新激活码单码状态。
     *
     * @param request 状态更新请求
     * @return 是否更新成功
     */
    boolean updateActivationCodeItemStatus(ActivationCodeItemStatusUpdateRequest request);

    /**
     * 删除激活码单码。
     *
     * @param id 激活码ID
     * @return 是否删除成功
     */
    boolean deleteActivationCodeItem(Long id);

    /**
     * 查询激活码兑换日志列表。
     *
     * @param request 查询请求
     * @return 激活码兑换日志分页结果
     */
    Page<ActivationLogVo> listActivationLogs(ActivationLogListRequest request);

    /**
     * 查询指定用户激活码兑换日志列表。
     *
     * @param userId  用户ID
     * @param request 查询请求
     * @return 激活码兑换日志分页结果
     */
    Page<ActivationLogVo> listUserActivationLogs(Long userId, ActivationLogListRequest request);
}
