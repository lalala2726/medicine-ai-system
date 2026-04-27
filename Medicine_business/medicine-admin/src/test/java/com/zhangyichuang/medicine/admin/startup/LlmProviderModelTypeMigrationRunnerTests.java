package com.zhangyichuang.medicine.admin.startup;

import com.zhangyichuang.medicine.admin.mapper.LlmProviderMapper;
import com.zhangyichuang.medicine.admin.mapper.LlmProviderModelMapper;
import com.zhangyichuang.medicine.admin.service.AgentConfigRuntimeSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LlmProviderModelTypeMigrationRunnerTests {

    @Mock
    private LlmProviderMapper llmProviderMapper;

    @Mock
    private LlmProviderModelMapper llmProviderModelMapper;

    @Mock
    private AgentConfigRuntimeSyncService agentConfigRuntimeSyncService;

    @InjectMocks
    private LlmProviderModelTypeMigrationRunner runner;

    @Test
    void run_ShouldKeepLegacyMigrationAsNoOp() throws Exception {
        runner.run(new DefaultApplicationArguments(new String[0]));

        verifyNoInteractions(llmProviderMapper, llmProviderModelMapper, agentConfigRuntimeSyncService);
    }

    @Test
    void run_ShouldNotTouchLegacyModelsEvenWhenArgumentsPresent() throws Exception {
        runner.run(new DefaultApplicationArguments(new String[]{"--spring.profiles.active=test"}));

        verifyNoInteractions(llmProviderMapper, llmProviderModelMapper, agentConfigRuntimeSyncService);
    }
}
