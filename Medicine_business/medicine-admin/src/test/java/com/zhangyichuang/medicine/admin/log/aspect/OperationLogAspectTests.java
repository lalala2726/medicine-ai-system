package com.zhangyichuang.medicine.admin.log.aspect;

import com.zhangyichuang.medicine.common.log.annotation.OperationLog;
import com.zhangyichuang.medicine.common.log.aspect.OperationLogAspect;
import com.zhangyichuang.medicine.common.log.enums.OperationType;
import com.zhangyichuang.medicine.common.log.executor.OperationLogExecutor;
import com.zhangyichuang.medicine.common.log.model.OperationLogInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationLogAspectTests {

    @Mock
    private OperationLogExecutor operationLogExecutor;

    @InjectMocks
    private OperationLogAspect operationLogAspect;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 验证切面在成功场景下会构建日志模型并交给执行器处理，
     * 同时确认敏感参数脱敏与基础字段映射正确。
     */
    @Test
    void around_WhenSuccess_ShouldBuildAndRecordLogInfo() throws Throwable {
        Method method = DummyController.class.getMethod("createUser", Map.class);
        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getDeclaringTypeName()).thenReturn(DummyController.class.getName());
        when(signature.getName()).thenReturn("createUser");
        when(signature.getParameterNames()).thenReturn(new String[]{"request"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{Map.of("password", "123456", "name", "alice")});
        when(joinPoint.proceed()).thenReturn(Map.of("ok", true, "token", "abc"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/system/user");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "Mozilla/5.0 Chrome/120.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        operationLogAspect.around(joinPoint);

        ArgumentCaptor<OperationLogInfo> infoCaptor = ArgumentCaptor.forClass(OperationLogInfo.class);
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(operationLogExecutor).record(infoCaptor.capture(), requestCaptor.capture());

        OperationLogInfo info = infoCaptor.getValue();
        assertEquals("用户管理", info.getModule());
        assertEquals("新增用户", info.getAction());
        assertEquals(1, info.getSuccess());
        assertEquals("/system/user", info.getRequestUri());
        assertTrue(info.getRequestParams().contains("\"password\":\"***\""));
        assertTrue(info.getResponseResult().contains("\"token\":\"***\""));
        assertEquals("/system/user", requestCaptor.getValue().getRequestURI());
    }

    /**
     * 验证切面在异常场景下仍会记录失败日志，
     * 并包含请求方法、请求地址与错误信息。
     */
    @Test
    void around_WhenException_ShouldRecordFailureLogInfo() throws Throwable {
        Method method = DummyController.class.getMethod("updateUser", Map.class);
        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getDeclaringTypeName()).thenReturn(DummyController.class.getName());
        when(signature.getName()).thenReturn("updateUser");
        when(signature.getParameterNames()).thenReturn(new String[]{"request"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{Map.of("id", 1)});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("boom"));

        HttpServletRequest request = new MockHttpServletRequest("PUT", "/system/user");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThrows(RuntimeException.class, () -> operationLogAspect.around(joinPoint));

        ArgumentCaptor<OperationLogInfo> infoCaptor = ArgumentCaptor.forClass(OperationLogInfo.class);
        verify(operationLogExecutor).record(infoCaptor.capture(), org.mockito.ArgumentMatchers.any(HttpServletRequest.class));
        OperationLogInfo info = infoCaptor.getValue();
        assertEquals(0, info.getSuccess());
        assertEquals("/system/user", info.getRequestUri());
        assertEquals("PUT", info.getHttpMethod());
        assertTrue(info.getErrorMsg().contains("boom"));
    }

    private static class DummyController {

        @OperationLog(module = "用户管理", action = "新增用户", type = OperationType.ADD, recordResult = true)
        public Map<String, Object> createUser(Map<String, Object> request) {
            return request;
        }

        @OperationLog(module = "用户管理", action = "修改用户", type = OperationType.UPDATE)
        public void updateUser(Map<String, Object> request) {
        }
    }
}
