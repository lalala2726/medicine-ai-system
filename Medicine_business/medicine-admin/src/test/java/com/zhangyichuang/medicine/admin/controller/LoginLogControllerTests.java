package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.LoginLogQueryRequest;
import com.zhangyichuang.medicine.admin.service.LoginLogService;
import com.zhangyichuang.medicine.model.entity.SysLoginLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginLogControllerTests {

    @Mock
    private LoginLogService loginLogService;

    @InjectMocks
    private LoginLogController controller;

    /**
     * 验证登录日志列表接口会透传查询参数并返回分页结果。
     */
    @Test
    void logList_ShouldReturnPagedResult() {
        LoginLogQueryRequest request = new LoginLogQueryRequest();
        Page<SysLoginLog> page = new Page<>(1, 10, 1);
        SysLoginLog log = new SysLoginLog();
        log.setId(1L);
        log.setUsername("admin");
        page.setRecords(List.of(log));
        when(loginLogService.logList(request)).thenReturn(page);

        var result = controller.logList(request);

        assertEquals(200, result.getCode());
        verify(loginLogService).logList(request);
    }

    /**
     * 验证清空登录日志接口会调用 service 清理逻辑并返回成功响应。
     */
    @Test
    void clearLog_ShouldDelegateToService() {
        when(loginLogService.clearLogs()).thenReturn(true);

        var result = controller.clearLog();

        assertEquals(200, result.getCode());
        verify(loginLogService).clearLogs();
    }

    /**
     * 验证登录日志相关接口都声明了权限注解，
     * 防止未授权用户访问审计数据。
     */
    @Test
    void methods_ShouldHavePreAuthorizeAnnotations() throws NoSuchMethodException {
        Method listMethod = LoginLogController.class.getMethod("logList", LoginLogQueryRequest.class);
        Method getMethod = LoginLogController.class.getMethod("getLogById", Long.class);
        Method clearMethod = LoginLogController.class.getMethod("clearLog");

        assertTrue(listMethod.isAnnotationPresent(PreAuthorize.class));
        assertTrue(getMethod.isAnnotationPresent(PreAuthorize.class));
        assertTrue(clearMethod.isAnnotationPresent(PreAuthorize.class));
    }
}
