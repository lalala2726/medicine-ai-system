package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.mapper.OperationLogMapper;
import com.zhangyichuang.medicine.admin.model.request.OperationLogQueryRequest;
import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.model.entity.SysOperationLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationLogServiceImplTests {

    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private OperationLogServiceImpl sysOperationLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sysOperationLogService, "baseMapper", operationLogMapper);
    }

    /**
     * 验证操作日志分页查询会委托给 Mapper 执行。
     */
    @Test
    void logList_ShouldDelegateToMapper() {
        OperationLogQueryRequest query = new OperationLogQueryRequest();
        Page<SysOperationLog> page = new Page<>(1, 10, 1);
        when(operationLogMapper.logList(any(Page.class), any(OperationLogQueryRequest.class))).thenReturn(page);

        var result = sysOperationLogService.logList(query);

        assertEquals(page, result);
        verify(operationLogMapper).logList(any(Page.class), any(OperationLogQueryRequest.class));
    }

    /**
     * 验证非法操作日志 ID 会抛出参数异常，
     * 防止错误请求污染调用链路。
     */
    @Test
    void getLogById_WhenIdInvalid_ShouldThrowException() {
        assertThrows(ParamException.class, () -> sysOperationLogService.getLogById(0L));
    }

    /**
     * 验证按 ID 查询会返回对应操作日志实体。
     */
    @Test
    void getLogById_ShouldReturnEntity() {
        SysOperationLog log = new SysOperationLog();
        log.setId(1L);
        when(operationLogMapper.selectById(1L)).thenReturn(log);

        var result = sysOperationLogService.getLogById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    /**
     * 验证批量清理在 Mapper 返回受影响行数时判定为成功。
     */
    @Test
    void clearLogs_ShouldReturnTrue() {
        when(operationLogMapper.delete(any())).thenReturn(1);

        boolean result = sysOperationLogService.clearLogs();

        assertTrue(result);
        verify(operationLogMapper).delete(any());
    }
}
