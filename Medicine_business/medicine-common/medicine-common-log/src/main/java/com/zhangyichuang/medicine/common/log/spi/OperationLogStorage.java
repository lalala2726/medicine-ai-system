package com.zhangyichuang.medicine.common.log.spi;

import com.zhangyichuang.medicine.common.log.model.OperationLogInfo;

/**
 * 操作日志存储 SPI 接口。
 */
public interface OperationLogStorage {

    /**
     * 保存操作日志。
     */
    void save(OperationLogInfo logInfo);
}
