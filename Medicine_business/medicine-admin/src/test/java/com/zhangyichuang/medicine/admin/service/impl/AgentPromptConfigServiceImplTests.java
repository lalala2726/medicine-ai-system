package com.zhangyichuang.medicine.admin.service.impl;

import com.zhangyichuang.medicine.admin.mapper.AgentPromptConfigMapper;
import com.zhangyichuang.medicine.admin.mapper.AgentPromptHistoryMapper;
import com.zhangyichuang.medicine.admin.mapper.AgentPromptKeyMapper;
import com.zhangyichuang.medicine.admin.model.request.AgentPromptKeyUpsertRequest;
import com.zhangyichuang.medicine.admin.model.request.AgentPromptUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.AgentPromptKeyOptionVo;
import com.zhangyichuang.medicine.admin.service.AgentPromptRuntimeSyncService;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import com.zhangyichuang.medicine.model.entity.AgentPromptConfig;
import com.zhangyichuang.medicine.model.entity.AgentPromptKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentPromptConfigServiceImplTests {

    @Mock
    private AgentPromptConfigMapper agentPromptConfigMapper;

    @Mock
    private AgentPromptHistoryMapper agentPromptHistoryMapper;

    @Mock
    private AgentPromptKeyMapper agentPromptKeyMapper;

    @Mock
    private AgentPromptRuntimeSyncService agentPromptRuntimeSyncService;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    private AgentPromptConfigServiceImpl agentPromptConfigService;

    @BeforeEach
    void setUp() {
        agentPromptConfigService = new AgentPromptConfigServiceImpl(
                agentPromptConfigMapper,
                agentPromptHistoryMapper,
                agentPromptKeyMapper,
                agentPromptRuntimeSyncService,
                captchaService,
                new RedisCache(redisTemplate)
        );
    }

    @Test
    void savePromptKey_WhenKeyNotExists_ShouldInsert() {
        AgentPromptKeyUpsertRequest request = new AgentPromptKeyUpsertRequest();
        request.setPromptKey("custom_prompt_key");
        request.setDescription("自定义提示词");
        when(agentPromptKeyMapper.selectOne(any())).thenReturn(null);

        boolean saved = agentPromptConfigService.savePromptKey(request);

        assertTrue(saved);
        ArgumentCaptor<AgentPromptKey> keyCaptor = ArgumentCaptor.forClass(AgentPromptKey.class);
        verify(agentPromptKeyMapper).insert((AgentPromptKey) keyCaptor.capture());
        AgentPromptKey inserted = keyCaptor.getValue();
        assertEquals("custom_prompt_key", inserted.getPromptKey());
        assertEquals("自定义提示词", inserted.getDescription());
    }

    @Test
    void savePromptKey_WhenKeyExists_ShouldUpdateDescription() {
        AgentPromptKeyUpsertRequest request = new AgentPromptKeyUpsertRequest();
        request.setPromptKey("custom_prompt_key");
        request.setDescription("新的说明");
        AgentPromptKey existing = AgentPromptKey.builder()
                .id(1L)
                .promptKey("custom_prompt_key")
                .description("旧说明")
                .build();
        when(agentPromptKeyMapper.selectOne(any())).thenReturn(existing);

        boolean saved = agentPromptConfigService.savePromptKey(request);

        assertTrue(saved);
        verify(agentPromptKeyMapper, never()).insert(any(AgentPromptKey.class));
        ArgumentCaptor<AgentPromptKey> keyCaptor = ArgumentCaptor.forClass(AgentPromptKey.class);
        verify(agentPromptKeyMapper).updateById(keyCaptor.capture());
        AgentPromptKey updated = keyCaptor.getValue();
        assertEquals(1L, updated.getId());
        assertEquals("custom_prompt_key", updated.getPromptKey());
        assertEquals("新的说明", updated.getDescription());
    }

    @Test
    void listPromptKeys_ShouldBuildConfiguredStateFromRegisteredKeys() {
        AgentPromptKey key1 = AgentPromptKey.builder()
                .promptKey("prompt_a")
                .description("提示词A")
                .build();
        AgentPromptKey key2 = AgentPromptKey.builder()
                .promptKey("prompt_b")
                .description("提示词B")
                .build();
        when(agentPromptKeyMapper.selectList(any())).thenReturn(List.of(key1, key2));
        AgentPromptConfig config = AgentPromptConfig.builder()
                .promptKey("prompt_b")
                .promptContent("提示词B内容")
                .promptVersion(3L)
                .build();
        when(agentPromptConfigMapper.selectList(any())).thenReturn(List.of(config));
        when(redisTemplate.hasKey(RedisConstants.AgentConfig.PROMPT_CONFIG_KEY_TEMPLATE.formatted("prompt_b")))
                .thenReturn(true);

        List<AgentPromptKeyOptionVo> options = agentPromptConfigService.listPromptKeys();
        Map<String, AgentPromptKeyOptionVo> optionMap = options.stream()
                .collect(Collectors.toMap(AgentPromptKeyOptionVo::getPromptKey, item -> item));

        assertEquals(2, options.size());
        assertEquals("提示词A", optionMap.get("prompt_a").getDescription());
        assertFalse(optionMap.get("prompt_a").getConfigured());
        assertNull(optionMap.get("prompt_a").getPromptVersion());
        assertEquals("提示词B", optionMap.get("prompt_b").getDescription());
        assertTrue(optionMap.get("prompt_b").getConfigured());
        assertEquals(3L, optionMap.get("prompt_b").getPromptVersion());
    }

    @Test
    void savePromptConfig_WhenPromptKeyNotRegistered_ShouldThrowParamException() {
        AgentPromptUpdateRequest request = new AgentPromptUpdateRequest();
        request.setPromptKey("unknown_prompt_key");
        request.setPromptContent("提示词内容");
        when(agentPromptKeyMapper.selectList(any())).thenReturn(List.of());

        assertThrows(ParamException.class, () -> agentPromptConfigService.savePromptConfig(request));
        verify(agentPromptConfigMapper, never()).insert(any(AgentPromptConfig.class));
        verify(agentPromptConfigMapper, never()).updateById(any(AgentPromptConfig.class));
    }
}
