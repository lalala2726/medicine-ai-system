package com.zhangyichuang.medicine.agent.service;

import com.zhangyichuang.medicine.agent.model.vo.health.RpcHealthVo;

/**
 * Agent RPC 健康检查服务。
 */
public interface RpcHealthService {

    /**
     * 探测 admin/client RPC 链路连通性。
     *
     * @return 聚合后的健康检查结果
     */
    RpcHealthVo checkRpcHealth();
}
