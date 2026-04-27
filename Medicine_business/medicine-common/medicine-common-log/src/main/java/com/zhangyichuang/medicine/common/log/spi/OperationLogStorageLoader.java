package com.zhangyichuang.medicine.common.log.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 操作日志 SPI 加载器。
 */
public final class OperationLogStorageLoader {

    private static final Logger log = LoggerFactory.getLogger(OperationLogStorageLoader.class);
    private static volatile List<OperationLogStorage> storages;

    private OperationLogStorageLoader() {
    }

    /**
     * 获取 SPI 实现列表。
     */
    public static List<OperationLogStorage> getStorages() {
        List<OperationLogStorage> cached = storages;
        if (cached != null) {
            return cached;
        }
        synchronized (OperationLogStorageLoader.class) {
            if (storages == null) {
                storages = loadStorages();
            }
            return storages;
        }
    }

    private static List<OperationLogStorage> loadStorages() {
        List<OperationLogStorage> result = new ArrayList<>();
        try {
            ServiceLoader<OperationLogStorage> loader = ServiceLoader.load(OperationLogStorage.class);
            loader.forEach(result::add);
        } catch (Exception ex) {
            log.warn("Failed to load OperationLogStorage SPI implementations", ex);
        }
        return result.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(result);
    }
}
