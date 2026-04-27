package com.zhangyichuang.medicine.client.rpc;

import com.zhangyichuang.medicine.rpc.client.ClientAgentHealthRpcService;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 客户端 Agent 健康检查 RPC Provider。
 */
@DubboService(interfaceClass = ClientAgentHealthRpcService.class, group = "medicine-client", version = "1.0.0")
public class ClientAgentHealthRpcServiceImpl implements ClientAgentHealthRpcService {

    @Override
    public boolean ping() {
        return true;
    }
}
