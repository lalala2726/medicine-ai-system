package com.zhangyichuang.medicine.common.log.executor;

import com.zhangyichuang.medicine.common.core.utils.IpAddressUtils;
import com.zhangyichuang.medicine.common.log.model.OperationLogInfo;
import com.zhangyichuang.medicine.common.log.spi.OperationLogStorage;
import com.zhangyichuang.medicine.common.log.spi.OperationLogStorageLoader;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * 操作日志执行器。
 */
public class OperationLogExecutor {

    private static final Logger log = LoggerFactory.getLogger(OperationLogExecutor.class);
    private static final String USER_AGENT_HEADER = "User-Agent";

    /**
     * 记录操作日志。
     */
    public void record(OperationLogInfo logInfo, HttpServletRequest request) {
        if (logInfo == null) {
            return;
        }
        if (logInfo.getCreateTime() == null) {
            logInfo.setCreateTime(new Date());
        }
        enrichRequestInfo(logInfo, request);

        List<OperationLogStorage> storages = OperationLogStorageLoader.getStorages();
        if (CollectionUtils.isEmpty(storages)) {
            log.debug("No OperationLogStorage SPI implementation found.");
            return;
        }
        for (OperationLogStorage storage : storages) {
            try {
                storage.save(logInfo);
            } catch (Exception ex) {
                log.warn("Failed to persist operation log via SPI: {}", storage.getClass().getName(), ex);
            }
        }
    }

    private void enrichRequestInfo(OperationLogInfo logInfo, HttpServletRequest request) {
        if (request == null) {
            return;
        }
        try {
            String ip = IpAddressUtils.getIpAddress(request);
            logInfo.setIp(ip);
            logInfo.setUserAgent(request.getHeader(USER_AGENT_HEADER));
        } catch (Exception ex) {
            log.warn("Failed to resolve request info for operation log", ex);
        }
    }
}
