package com.zhangyichuang.medicine.admin.spi;

import com.zhangyichuang.medicine.admin.publisher.OperationLogPublisher;
import com.zhangyichuang.medicine.common.core.utils.SpringUtils;
import com.zhangyichuang.medicine.common.log.model.OperationLogInfo;
import com.zhangyichuang.medicine.model.mq.OperationLogMessage;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OperationLogStorageImplTests {

    /**
     * 验证 admin 侧 SPI 实现会将 OperationLogInfo 完整映射为 MQ 消息并发布，
     * 确保 common-log 与现有异步落库链路正确衔接。
     */
    @Test
    void save_ShouldMapAndPublishMessage() {
        OperationLogPublisher publisher = mock(OperationLogPublisher.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        OperationLogStorageImpl storage = new OperationLogStorageImpl();

        Date now = new Date();
        OperationLogInfo info = new OperationLogInfo();
        info.setModule("用户管理");
        info.setAction("新增用户");
        info.setRequestUri("/system/user");
        info.setHttpMethod("POST");
        info.setMethodName("UserController.addUser");
        info.setUserId(100L);
        info.setUsername("admin");
        info.setIp("127.0.0.1");
        info.setUserAgent("Mozilla/5.0");
        info.setRequestParams("{\"name\":\"alice\"}");
        info.setResponseResult("{\"code\":200}");
        info.setCostTime(88L);
        info.setSuccess(1);
        info.setErrorMsg(null);
        info.setCreateTime(now);

        when(applicationContext.getBean(OperationLogPublisher.class)).thenReturn(publisher);
        ReflectionTestUtils.setField(SpringUtils.class, "applicationContext", applicationContext);
        try {
            storage.save(info);

            org.mockito.ArgumentCaptor<OperationLogMessage> captor = org.mockito.ArgumentCaptor.forClass(OperationLogMessage.class);
            verify(publisher).publish(captor.capture());
            OperationLogMessage message = captor.getValue();
            assertEquals("用户管理", message.getModule());
            assertEquals("新增用户", message.getAction());
            assertEquals("/system/user", message.getRequestUri());
            assertEquals("POST", message.getHttpMethod());
            assertEquals("UserController.addUser", message.getMethodName());
            assertEquals(100L, message.getUserId());
            assertEquals("admin", message.getUsername());
            assertEquals("127.0.0.1", message.getIp());
            assertEquals("Mozilla/5.0", message.getUserAgent());
            assertEquals("{\"name\":\"alice\"}", message.getRequestParams());
            assertEquals("{\"code\":200}", message.getResponseResult());
            assertEquals(88L, message.getCostTime());
            assertEquals(1, message.getSuccess());
            assertEquals(now, message.getCreateTime());
        } finally {
            ReflectionTestUtils.setField(SpringUtils.class, "applicationContext", null);
        }
    }
}
