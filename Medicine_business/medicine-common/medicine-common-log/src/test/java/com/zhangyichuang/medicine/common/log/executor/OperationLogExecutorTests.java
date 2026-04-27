package com.zhangyichuang.medicine.common.log.executor;

import com.zhangyichuang.medicine.common.log.model.OperationLogInfo;
import com.zhangyichuang.medicine.common.log.spi.OperationLogStorageLoader;
import com.zhangyichuang.medicine.common.log.testspi.CapturingOperationLogStorage;
import com.zhangyichuang.medicine.common.log.testspi.ThrowingOperationLogStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OperationLogExecutorTests {

    private final OperationLogExecutor operationLogExecutor = new OperationLogExecutor();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(OperationLogStorageLoader.class, "storages", null);
        CapturingOperationLogStorage.clear();
        ThrowingOperationLogStorage.reset();
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(OperationLogStorageLoader.class, "storages", null);
        CapturingOperationLogStorage.clear();
        ThrowingOperationLogStorage.reset();
    }

    /**
     * 验证当输入日志对象为空时执行器会直接返回，
     * 不触发任何 SPI 存储调用。
     */
    @Test
    void record_WhenLogInfoIsNull_ShouldDoNothing() {
        operationLogExecutor.record(null, null);

        assertEquals(0, CapturingOperationLogStorage.SAVED.size());
        assertEquals(0, ThrowingOperationLogStorage.CALL_COUNT.get());
    }

    /**
     * 验证执行器会补齐请求上下文信息并调用全部 SPI 存储实现，
     * 同时一个存储实现异常不会影响其他实现继续执行。
     */
    @Test
    void record_ShouldEnrichAndSaveAndTolerateStorageException() {
        OperationLogInfo logInfo = new OperationLogInfo();
        logInfo.setModule("用户管理");
        logInfo.setAction("新增用户");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("8.8.8.8");
        request.addHeader("User-Agent", "Mozilla/5.0");

        operationLogExecutor.record(logInfo, request);

        assertEquals(1, ThrowingOperationLogStorage.CALL_COUNT.get());
        assertEquals(1, CapturingOperationLogStorage.SAVED.size());

        OperationLogInfo saved = CapturingOperationLogStorage.SAVED.get(0);
        assertNotNull(saved.getCreateTime());
        assertEquals("8.8.8.8", saved.getIp());
        assertEquals("Mozilla/5.0", saved.getUserAgent());
    }
}
