package com.zhangyichuang.medicine.admin.listener;

import com.zhangyichuang.medicine.admin.service.LoginLogService;
import com.zhangyichuang.medicine.common.rabbitmq.constants.LoginLogQueueConstants;
import com.zhangyichuang.medicine.model.entity.SysLoginLog;
import com.zhangyichuang.medicine.model.mq.LoginLogMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 登录日志 MQ 消费者。
 */
@Component
@RequiredArgsConstructor
public class LoginLogConsumer {

    private final LoginLogService loginLogService;

    /**
     * 消费登录日志消息并持久化。
     */
    @RabbitListener(queues = LoginLogQueueConstants.QUEUE)
    public void handle(LoginLogMessage message) {
        if (message == null) {
            return;
        }
        SysLoginLog log = SysLoginLog.builder()
                .userId(message.getUserId())
                .username(message.getUsername())
                .loginSource(message.getLoginSource())
                .loginStatus(message.getLoginStatus())
                .failReason(message.getFailReason())
                .loginType(message.getLoginType())
                .ipAddress(message.getIpAddress())
                .userAgent(message.getUserAgent())
                .deviceType(message.getDeviceType())
                .os(message.getOs())
                .browser(message.getBrowser())
                .loginTime(message.getLoginTime() == null ? new Date() : message.getLoginTime())
                .build();
        loginLogService.save(log);
    }
}
