package com.zhangyichuang.medicine.admin.spi;

import com.zhangyichuang.medicine.admin.publisher.OperationLogPublisher;
import com.zhangyichuang.medicine.common.core.utils.SpringUtils;
import com.zhangyichuang.medicine.common.log.model.OperationLogInfo;
import com.zhangyichuang.medicine.common.log.spi.OperationLogStorage;
import com.zhangyichuang.medicine.model.mq.OperationLogMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 操作日志 SPI 保存实现，交由 admin 侧投递 MQ。
 */
public class OperationLogStorageImpl implements OperationLogStorage {

    private static final Logger log = LoggerFactory.getLogger(OperationLogStorageImpl.class);

    @Override
    public void save(OperationLogInfo logInfo) {
        if (logInfo == null) {
            return;
        }
        try {
            OperationLogPublisher publisher = SpringUtils.getBean(OperationLogPublisher.class);
            OperationLogMessage message = OperationLogMessage.builder()
                    .module(logInfo.getModule())
                    .action(logInfo.getAction())
                    .requestUri(logInfo.getRequestUri())
                    .httpMethod(logInfo.getHttpMethod())
                    .methodName(logInfo.getMethodName())
                    .userId(logInfo.getUserId())
                    .username(logInfo.getUsername())
                    .ip(logInfo.getIp())
                    .userAgent(logInfo.getUserAgent())
                    .requestParams(logInfo.getRequestParams())
                    .responseResult(logInfo.getResponseResult())
                    .costTime(logInfo.getCostTime())
                    .success(logInfo.getSuccess())
                    .errorMsg(logInfo.getErrorMsg())
                    .createTime(logInfo.getCreateTime())
                    .build();
            publisher.publish(message);
        } catch (Exception ex) {
            log.warn("Failed to save operation log via SPI", ex);
        }
    }
}
