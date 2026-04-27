package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.LoginLogQueryRequest;
import com.zhangyichuang.medicine.model.entity.SysLoginLog;
import org.apache.ibatis.annotations.Param;

/**
 * 系统登录日志 Mapper。
 */
public interface LoginLogMapper extends BaseMapper<SysLoginLog> {

    /**
     * 分页查询登录日志列表。
     */
    Page<SysLoginLog> logList(@Param("page") Page<SysLoginLog> page,
                              @Param("query") LoginLogQueryRequest query);
}
