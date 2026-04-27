package com.zhangyichuang.medicine.rpc.admin;

/**
 * 管理端 Agent 健康检查 RPC。
 */
public interface AdminAgentHealthRpcService {

    /**
     * 探测管理端 RPC 链路是否可用。
     *
     * @return true 表示 Provider 可调用
     */
    boolean ping();
}
