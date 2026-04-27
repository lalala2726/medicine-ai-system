package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.OperationLogQueryRequest;
import com.zhangyichuang.medicine.model.entity.SysOperationLog;
import org.apache.ibatis.annotations.Param;

/**
 * 系统操作日志 Mapper。
 */
public interface OperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 分页查询操作日志列表。
     */
    Page<SysOperationLog> logList(@Param("page") Page<SysOperationLog> page,
                                  @Param("query") OperationLogQueryRequest query);
}
