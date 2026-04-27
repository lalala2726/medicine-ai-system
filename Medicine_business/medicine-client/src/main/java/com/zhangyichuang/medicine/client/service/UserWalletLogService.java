package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.client.model.request.UserWalletBillRequest;
import com.zhangyichuang.medicine.model.dto.UserWalletLogRecordDto;
import com.zhangyichuang.medicine.model.entity.UserWalletLog;

/**
 * @author Chuang
 */
public interface UserWalletLogService extends IService<UserWalletLog> {


    /**
     * 获取用户钱包流水
     *
     * @param userId        用户ID
     * @param request       查询参数
     * @param walletLogPage 分页参数
     * @return 用户钱包流水
     */
    Page<UserWalletLog> getBillPageByUserId(Long userId, UserWalletBillRequest request, Page<UserWalletLog> walletLogPage);

    /**
     * 根据用户ID与流水ID获取钱包流水详情。
     *
     * @param userId 用户ID
     * @param billId 流水ID
     * @return 钱包流水详情
     */
    UserWalletLog getBillDetailByUserId(Long userId, Long billId);

    /**
     * 记录钱包流水
     *
     * @param recordDto 钱包流水参数
     */
    void recordWalletLog(UserWalletLogRecordDto recordDto);
}
