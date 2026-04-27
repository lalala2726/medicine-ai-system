package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.common.core.base.PageRequest;
import com.zhangyichuang.medicine.model.dto.UserWalletLogRecordDto;
import com.zhangyichuang.medicine.model.entity.UserWalletLog;

/**
 * @author Chuang
 */
public interface UserWalletLogService extends IService<UserWalletLog> {

    /**
     * 获取用户钱包流水
     *
     * @param userId  用户ID
     * @param request 列表查询参数
     * @return 用户钱包流水
     */
    Page<UserWalletLog> getUserWalletFlow(Long userId, PageRequest request);

    /**
     * 记录钱包流水
     *
     * @param recordDto 钱包流水参数
     */
    void recordWalletLog(UserWalletLogRecordDto recordDto);
}
