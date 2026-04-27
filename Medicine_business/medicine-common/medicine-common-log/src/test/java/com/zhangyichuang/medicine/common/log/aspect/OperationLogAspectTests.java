package com.zhangyichuang.medicine.common.log.aspect;

import com.zhangyichuang.medicine.common.log.annotation.OperationLog;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
     * 验证写操作请求（POST）在切面中会被记录，
     * 并且请求参数与响应体中的敏感字段会按规则脱敏。
     */
    @Test
    void around_WhenPostRequest_ShouldRecordLog() throws Throwable {
        Method method = DummyController.class.getMethod("createUser", Map.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, "createUser", new Object[]{Map.of("password", "123456")});
        when(joinPoint.proceed()).thenReturn(Map.of("token", "abc", "ok", true));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/system/user");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        operationLogAspect.around(joinPoint);

        ArgumentCaptor<OperationLogInfo> infoCaptor = ArgumentCaptor.forClass(OperationLogInfo.class);
        verify(operationLogExecutor).record(infoCaptor.capture(), org.mockito.ArgumentMatchers.any(HttpServletRequest.class));

        OperationLogInfo info = infoCaptor.getValue();
        assertEquals("用户管理", info.getModule());
        assertEquals("新增用户", info.getAction());
        assertEquals("POST", info.getHttpMethod());
        assertEquals("/system/user", info.getRequestUri());
        assertEquals(1, info.getSuccess());
        assertTrue(info.getRequestParams().contains("\"password\":\"***\""));
        assertTrue(info.getResponseResult().contains("\"token\":\"***\""));
    }

    /**
     * 验证只读请求（GET）默认不会记录操作日志，
     * 以符合“仅记录管理端写操作”的采集策略。
     */
    @Test
    void around_WhenGetRequest_ShouldSkipRecord() throws Throwable {
        Method method = DummyController.class.getMethod("updateUser", Map.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, "updateUser", new Object[]{Map.of("id", 1)});
        when(joinPoint.proceed()).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/user/1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        operationLogAspect.around(joinPoint);

        verifyNoInteractions(operationLogExecutor);
    }

    /**
     * 验证登录接口虽然可能是 POST，但会被切面显式排除，
     * 避免与登录日志链路重复记录。
     */
    @Test
    void around_WhenLoginEndpoint_ShouldSkipRecord() throws Throwable {
        Method method = DummyController.class.getMethod("updateUser", Map.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, "updateUser", new Object[]{Map.of("id", 1)});
        when(joinPoint.proceed()).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        operationLogAspect.around(joinPoint);

        verifyNoInteractions(operationLogExecutor);
    }

    /**
     * 验证业务方法抛异常时仍会生成失败日志，
     * 并在日志中记录错误信息用于审计排障。
     */
    @Test
    void around_WhenException_ShouldRecordFailureLog() throws Throwable {
        Method method = DummyController.class.getMethod("updateUser", Map.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, "updateUser", new Object[]{Map.of("id", 1)});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("boom"));

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/system/user");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThrows(RuntimeException.class, () -> operationLogAspect.around(joinPoint));

        ArgumentCaptor<OperationLogInfo> infoCaptor = ArgumentCaptor.forClass(OperationLogInfo.class);
        verify(operationLogExecutor).record(infoCaptor.capture(), org.mockito.ArgumentMatchers.any(HttpServletRequest.class));

        OperationLogInfo info = infoCaptor.getValue();
        assertEquals(0, info.getSuccess());
        assertTrue(info.getErrorMsg().contains("boom"));
    }

    private ProceedingJoinPoint mockJoinPoint(Method method, String methodName, Object[] args) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getDeclaringType()).thenReturn(DummyController.class);
        when(signature.getDeclaringTypeName()).thenReturn(DummyController.class.getName());
        when(signature.getName()).thenReturn(methodName);
        when(signature.getParameterNames()).thenReturn(new String[]{"request"});
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
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
