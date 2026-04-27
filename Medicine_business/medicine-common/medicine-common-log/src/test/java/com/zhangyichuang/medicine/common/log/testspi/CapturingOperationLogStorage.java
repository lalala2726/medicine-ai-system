package com.zhangyichuang.medicine.common.log.testspi;

import com.zhangyichuang.medicine.common.log.model.OperationLogInfo;
import com.zhangyichuang.medicine.common.log.spi.OperationLogStorage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 测试用 SPI 实现：捕获保存结果。
 */
public class CapturingOperationLogStorage implements OperationLogStorage {

    public static final List<OperationLogInfo> SAVED = new CopyOnWriteArrayList<>();

    public static void clear() {
        SAVED.clear();
    }

    @Override
    public void save(OperationLogInfo logInfo) {
        SAVED.add(logInfo);
    }
}
