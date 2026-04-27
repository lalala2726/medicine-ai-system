package com.zhangyichuang.medicine.common.log.testspi;

import com.zhangyichuang.medicine.common.log.model.OperationLogInfo;
import com.zhangyichuang.medicine.common.log.spi.OperationLogStorage;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试用 SPI 实现：用于验证异常容错。
 */
public class ThrowingOperationLogStorage implements OperationLogStorage {

    public static final AtomicInteger CALL_COUNT = new AtomicInteger(0);

    public static void reset() {
        CALL_COUNT.set(0);
    }

    @Override
    public void save(OperationLogInfo logInfo) {
        CALL_COUNT.incrementAndGet();
        throw new IllegalStateException("mock failure");
    }
}
