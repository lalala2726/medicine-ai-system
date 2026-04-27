package com.zhangyichuang.medicine.admin.listener;

import com.zhangyichuang.medicine.admin.service.OperationLogService;
import com.zhangyichuang.medicine.common.rabbitmq.constants.OperationLogQueueConstants;
import com.zhangyichuang.medicine.model.entity.SysOperationLog;
import com.zhangyichuang.medicine.model.mq.OperationLogMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 操作日志 MQ 消费者。
 */
@Component
@RequiredArgsConstructor
public class OperationLogConsumer {

    private final OperationLogService operationLogService;

    /**
     * 消费操作日志消息并持久化。
     */
    @RabbitListener(queues = OperationLogQueueConstants.QUEUE)
    public void handle(OperationLogMessage message) {
        if (message == null) {
            return;
        }
        SysOperationLog log = SysOperationLog.builder()
                .module(message.getModule())
                .action(message.getAction())
                .requestUri(message.getRequestUri())
                .httpMethod(message.getHttpMethod())
                .methodName(message.getMethodName())
                .userId(message.getUserId())
                .username(message.getUsername())
                .ip(message.getIp())
                .userAgent(message.getUserAgent())
                .requestParams(message.getRequestParams())
                .responseResult(message.getResponseResult())
                .costTime(message.getCostTime())
                .success(message.getSuccess())
                .errorMsg(message.getErrorMsg())
                .createTime(message.getCreateTime() == null ? new Date() : message.getCreateTime())
                .build();
        operationLogService.save(log);
    }
}
