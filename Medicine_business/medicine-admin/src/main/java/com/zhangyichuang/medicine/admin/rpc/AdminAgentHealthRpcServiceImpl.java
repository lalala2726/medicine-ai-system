package com.zhangyichuang.medicine.admin.rpc;

import com.zhangyichuang.medicine.rpc.admin.AdminAgentHealthRpcService;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 管理端 Agent 健康检查 RPC Provider。
 */
@DubboService(interfaceClass = AdminAgentHealthRpcService.class, group = "medicine-admin", version = "1.0.0")
public class AdminAgentHealthRpcServiceImpl implements AdminAgentHealthRpcService {

    @Override
    public boolean ping() {
        return true;
    }
}
