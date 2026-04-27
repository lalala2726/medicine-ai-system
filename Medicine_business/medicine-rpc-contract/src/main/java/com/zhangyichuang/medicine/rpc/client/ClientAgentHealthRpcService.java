package com.zhangyichuang.medicine.rpc.client;

/**
 * 客户端 Agent 健康检查 RPC。
 */
public interface ClientAgentHealthRpcService {

    /**
     * 探测客户端 RPC 链路是否可用。
     *
     * @return true 表示 Provider 可调用
     */
    boolean ping();
}
