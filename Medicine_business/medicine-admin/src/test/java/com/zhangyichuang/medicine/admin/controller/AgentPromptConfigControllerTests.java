package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.admin.model.request.AgentPromptKeyUpsertRequest;
import com.zhangyichuang.medicine.admin.model.request.AgentPromptUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.AgentPromptKeyOptionVo;
import com.zhangyichuang.medicine.admin.service.AgentPromptConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentPromptConfigControllerTests {

    @Mock
    private AgentPromptConfigService agentPromptConfigService;

    @InjectMocks
    private AgentPromptConfigController agentPromptConfigController;

    @Test
    void listPromptKeys_ShouldReturnDataFromService() {
        AgentPromptKeyOptionVo option = new AgentPromptKeyOptionVo();
        option.setPromptKey("admin_assistant_system_prompt");
        option.setDescription("管理端助手主系统提示词");
        option.setConfigured(true);
        option.setPromptVersion(2L);
        when(agentPromptConfigService.listPromptKeys()).thenReturn(List.of(option));

        var result = agentPromptConfigController.listPromptKeys();

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("admin_assistant_system_prompt", result.getData().get(0).getPromptKey());
        verify(agentPromptConfigService).listPromptKeys();
    }

    @Test
    void savePromptKey_ShouldDelegateToService() {
        AgentPromptKeyUpsertRequest request = new AgentPromptKeyUpsertRequest();
        request.setPromptKey("custom_prompt_key");
        request.setDescription("自定义提示词");
        when(agentPromptConfigService.savePromptKey(request)).thenReturn(true);

        var result = agentPromptConfigController.savePromptKey(request);

        assertEquals(200, result.getCode());
        verify(agentPromptConfigService).savePromptKey(request);
    }

    @Test
    void savePromptConfig_ShouldDelegateToService() {
        AgentPromptUpdateRequest request = new AgentPromptUpdateRequest();
        request.setPromptKey("admin_assistant_system_prompt");
        request.setPromptContent("提示词内容");
        when(agentPromptConfigService.savePromptConfig(request)).thenReturn(true);

        var result = agentPromptConfigController.savePromptConfig(request);

        assertEquals(200, result.getCode());
        verify(agentPromptConfigService).savePromptConfig(request);
    }
}
