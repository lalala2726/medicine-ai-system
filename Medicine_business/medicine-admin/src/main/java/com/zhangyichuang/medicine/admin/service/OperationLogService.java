package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.admin.model.request.OperationLogQueryRequest;
import com.zhangyichuang.medicine.model.entity.SysOperationLog;

/**
 * 系统操作日志服务。
 */
public interface OperationLogService extends IService<SysOperationLog> {

    /**
     * 分页查询操作日志列表。
     */
    Page<SysOperationLog> logList(OperationLogQueryRequest query);

    /**
     * 根据日志ID查询详情。
     */
    SysOperationLog getLogById(Long id);

    /**
     * 清空操作日志。
     */
    boolean clearLogs();
}
