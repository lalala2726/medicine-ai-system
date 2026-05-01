package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.AgentTraceRunListRequest;
import com.zhangyichuang.medicine.admin.model.vo.AgentTraceDetailVo;
import com.zhangyichuang.medicine.admin.model.vo.AgentTraceRunListVo;

/**
 * Agent Trace 管理服务。
 */
public interface AgentTraceService {

    /**
     * 分页查询 Agent Trace run 列表。
     *
     * @param request 查询参数。
     * @return Agent Trace run 分页列表。
     */
    Page<AgentTraceRunListVo> listRuns(AgentTraceRunListRequest request);

    /**
     * 查询 Agent Trace 详情。
     *
     * @param traceId Trace 唯一标识。
     * @return Agent Trace 详情。
     */
    AgentTraceDetailVo getTraceDetail(String traceId);

    /**
     * 删除 Agent Trace。
     *
     * @param traceId Trace 唯一标识。
     * @return 删除成功返回 true。
     */
    boolean deleteTrace(String traceId);
}
