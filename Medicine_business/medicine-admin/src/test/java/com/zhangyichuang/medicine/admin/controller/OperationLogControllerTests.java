package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.OperationLogQueryRequest;
import com.zhangyichuang.medicine.admin.service.OperationLogService;
import com.zhangyichuang.medicine.model.entity.SysOperationLog;
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
class OperationLogControllerTests {

    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private OperationLogController controller;

    /**
     * 验证操作日志列表接口会调用 service 并返回分页结构。
     */
    @Test
    void logList_ShouldReturnPagedResult() {
        OperationLogQueryRequest request = new OperationLogQueryRequest();
        Page<SysOperationLog> page = new Page<>(1, 10, 1);
        SysOperationLog log = new SysOperationLog();
        log.setId(1L);
        log.setModule("用户管理");
        page.setRecords(List.of(log));
        when(operationLogService.logList(request)).thenReturn(page);

        var result = controller.logList(request);

        assertEquals(200, result.getCode());
        verify(operationLogService).logList(request);
    }

    /**
     * 验证清空操作日志接口会委托 service 执行清理。
     */
    @Test
    void clearLog_ShouldDelegateToService() {
        when(operationLogService.clearLogs()).thenReturn(true);

        var result = controller.clearLog();

        assertEquals(200, result.getCode());
        verify(operationLogService).clearLogs();
    }

    /**
     * 验证操作日志控制器关键接口具备权限注解，
     * 保障审计数据访问受 RBAC 控制。
     */
    @Test
    void methods_ShouldHavePreAuthorizeAnnotations() throws NoSuchMethodException {
        Method listMethod = OperationLogController.class.getMethod("logList", OperationLogQueryRequest.class);
        Method getMethod = OperationLogController.class.getMethod("getLogById", Long.class);
        Method clearMethod = OperationLogController.class.getMethod("clearLog");

        assertTrue(listMethod.isAnnotationPresent(PreAuthorize.class));
        assertTrue(getMethod.isAnnotationPresent(PreAuthorize.class));
        assertTrue(clearMethod.isAnnotationPresent(PreAuthorize.class));
    }
}
