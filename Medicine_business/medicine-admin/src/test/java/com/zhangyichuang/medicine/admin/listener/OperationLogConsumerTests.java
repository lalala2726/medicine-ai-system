package com.zhangyichuang.medicine.admin.listener;

import com.zhangyichuang.medicine.admin.service.OperationLogService;
import com.zhangyichuang.medicine.model.entity.SysOperationLog;
import com.zhangyichuang.medicine.model.mq.OperationLogMessage;
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
class OperationLogConsumerTests {

    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private OperationLogConsumer operationLogConsumer;

    /**
     * 验证操作日志消费者会把 MQ 消息正确映射为操作日志实体并保存，
     * 确保管理端写操作审计链路完整。
     */
    @Test
    void handle_ShouldMapAndSaveLog() {
        Date now = new Date();
        OperationLogMessage message = OperationLogMessage.builder()
                .module("用户管理")
                .action("新增用户")
                .requestUri("/system/user")
                .httpMethod("POST")
                .methodName("UserController.addUser")
                .username("admin")
                .success(1)
                .costTime(35L)
                .createTime(now)
                .build();

        operationLogConsumer.handle(message);

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(operationLogService).save(captor.capture());
        SysOperationLog log = captor.getValue();
        assertEquals("用户管理", log.getModule());
        assertEquals("新增用户", log.getAction());
        assertEquals("/system/user", log.getRequestUri());
        assertEquals("POST", log.getHttpMethod());
        assertEquals("UserController.addUser", log.getMethodName());
        assertEquals("admin", log.getUsername());
        assertEquals(1, log.getSuccess());
        assertEquals(35L, log.getCostTime());
        assertEquals(now, log.getCreateTime());
    }
}
