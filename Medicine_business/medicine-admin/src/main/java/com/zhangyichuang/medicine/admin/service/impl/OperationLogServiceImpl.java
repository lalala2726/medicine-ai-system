package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.OperationLogMapper;
import com.zhangyichuang.medicine.admin.model.request.OperationLogQueryRequest;
import com.zhangyichuang.medicine.admin.service.OperationLogService;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.model.entity.SysOperationLog;
import org.springframework.stereotype.Service;

/**
 * 系统操作日志服务实现。
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, SysOperationLog>
        implements OperationLogService {

    @Override
    public Page<SysOperationLog> logList(OperationLogQueryRequest query) {
        Page<SysOperationLog> page = query.toPage();
        return baseMapper.logList(page, query);
    }

    @Override
    public SysOperationLog getLogById(Long id) {
        Assert.isPositive(id, "日志ID必须大于0");
        return getById(id);
    }

    @Override
    public boolean clearLogs() {
        baseMapper.delete(new LambdaQueryWrapper<SysOperationLog>().isNotNull(SysOperationLog::getId));
        return true;
    }
}
