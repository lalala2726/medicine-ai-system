package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.mapper.LoginLogMapper;
import com.zhangyichuang.medicine.admin.model.request.LoginLogQueryRequest;
import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.model.entity.SysLoginLog;
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
class LoginLogServiceImplTests {

    @Mock
    private LoginLogMapper loginLogMapper;

    @InjectMocks
    private LoginLogServiceImpl sysLoginLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sysLoginLogService, "baseMapper", loginLogMapper);
    }

    /**
     * 验证分页查询会转发到 Mapper 并返回 Mapper 结果，
     * 保证 service 层不破坏分页行为。
     */
    @Test
    void logList_ShouldDelegateToMapper() {
        LoginLogQueryRequest query = new LoginLogQueryRequest();
        Page<SysLoginLog> page = new Page<>(1, 10, 1);
        when(loginLogMapper.logList(any(Page.class), any(LoginLogQueryRequest.class))).thenReturn(page);

        var result = sysLoginLogService.logList(query);

        assertEquals(page, result);
        verify(loginLogMapper).logList(any(Page.class), any(LoginLogQueryRequest.class));
    }

    /**
     * 验证非法日志 ID（<=0）会触发参数异常，
     * 防止无效请求进入数据层。
     */
    @Test
    void getLogById_WhenIdInvalid_ShouldThrowException() {
        assertThrows(ParamException.class, () -> sysLoginLogService.getLogById(0L));
    }

    /**
     * 验证按 ID 查询会返回 Mapper 查询到的实体数据。
     */
    @Test
    void getLogById_ShouldReturnEntity() {
        SysLoginLog log = new SysLoginLog();
        log.setId(1L);
        when(loginLogMapper.selectById(1L)).thenReturn(log);

        var result = sysLoginLogService.getLogById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    /**
     * 验证清理接口在 Mapper 删除成功时返回 true。
     */
    @Test
    void clearLogs_ShouldReturnTrue() {
        when(loginLogMapper.delete(any())).thenReturn(1);

        boolean result = sysLoginLogService.clearLogs();

        assertTrue(result);
        verify(loginLogMapper).delete(any());
    }
}
