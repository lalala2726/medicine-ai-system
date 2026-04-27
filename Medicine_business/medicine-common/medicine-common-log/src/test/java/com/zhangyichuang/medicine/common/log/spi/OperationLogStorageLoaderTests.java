package com.zhangyichuang.medicine.common.log.spi;

import com.zhangyichuang.medicine.common.log.testspi.CapturingOperationLogStorage;
import com.zhangyichuang.medicine.common.log.testspi.ThrowingOperationLogStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationLogStorageLoaderTests {

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(OperationLogStorageLoader.class, "storages", null);
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(OperationLogStorageLoader.class, "storages", null);
    }

    /**
     * 验证 SPI 加载器能够发现测试实现并进行缓存，
     * 连续调用返回同一缓存实例，避免重复扫描 ServiceLoader。
     */
    @Test
    void getStorages_ShouldLoadAndCacheSpiImplementations() {
        List<OperationLogStorage> first = OperationLogStorageLoader.getStorages();
        List<OperationLogStorage> second = OperationLogStorageLoader.getStorages();

        assertSame(first, second);
        assertTrue(first.stream().anyMatch(s -> s instanceof ThrowingOperationLogStorage));
        assertTrue(first.stream().anyMatch(s -> s instanceof CapturingOperationLogStorage));
    }
}
