package com.zhangyichuang.medicine.admin.startup;

import com.zhangyichuang.medicine.admin.mapper.LlmProviderMapper;
import com.zhangyichuang.medicine.admin.mapper.LlmProviderModelMapper;
import com.zhangyichuang.medicine.admin.service.AgentConfigRuntimeSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 历史迁移占位类。
 * <p>
 * 说明：
 * 1. 旧版本曾将 `RERANK` 自动迁移为 `CHAT`；
 * 2. 新版本已将 `RERANK` 作为独立模型类型，不再执行迁移逻辑；
 * 3. 保留该类仅用于兼容历史测试与依赖注入结构，运行时不注册为 Spring Bean。
 */
@RequiredArgsConstructor
public class LlmProviderModelTypeMigrationRunner implements ApplicationRunner {

    /**
     * 历史字段，仅用于保留构造参数结构，不再参与运行时逻辑。
     */
    private final LlmProviderMapper llmProviderMapper;

    /**
     * 历史字段，仅用于保留构造参数结构，不再参与运行时逻辑。
     */
    private final LlmProviderModelMapper llmProviderModelMapper;

    /**
     * 历史字段，仅用于保留构造参数结构，不再参与运行时逻辑。
     */
    private final AgentConfigRuntimeSyncService agentConfigRuntimeSyncService;

    /**
     * 新版本不再执行模型类型迁移。
     *
     * @param args Spring 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        // no-op: RERANK 现已作为独立模型类型，不再迁移为 CHAT
    }
}
