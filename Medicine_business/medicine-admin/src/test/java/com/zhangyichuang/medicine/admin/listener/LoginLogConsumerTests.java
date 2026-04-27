package com.zhangyichuang.medicine.admin.listener;

import com.zhangyichuang.medicine.admin.service.LoginLogService;
import com.zhangyichuang.medicine.model.entity.SysLoginLog;
import com.zhangyichuang.medicine.model.mq.LoginLogMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginLogConsumerTests {

    @Mock
    private LoginLogService loginLogService;

    @InjectMocks
    private LoginLogConsumer loginLogConsumer;

    /**
     * 验证登录日志消费者会把 MQ 消息正确映射到实体并调用保存，
     * 保证异步登录审计数据能够落库。
     */
    @Test
    void handle_ShouldMapAndSaveLog() {
        Date now = new Date();
        LoginLogMessage message = LoginLogMessage.builder()
                .userId(1L)
                .username("admin")
                .loginSource("admin")
                .loginStatus(1)
                .loginType("password")
                .ipAddress("localhost")
                .loginTime(now)
                .build();

        loginLogConsumer.handle(message);

        ArgumentCaptor<SysLoginLog> captor = ArgumentCaptor.forClass(SysLoginLog.class);
        verify(loginLogService).save(captor.capture());
        SysLoginLog log = captor.getValue();
        assertEquals(1L, log.getUserId());
        assertEquals("admin", log.getUsername());
        assertEquals("admin", log.getLoginSource());
        assertEquals(1, log.getLoginStatus());
        assertEquals("password", log.getLoginType());
        assertEquals("localhost", log.getIpAddress());
        assertEquals(now, log.getLoginTime());
    }
}
