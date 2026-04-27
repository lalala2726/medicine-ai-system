package com.zhangyichuang.medicine.client.task;

import com.zhangyichuang.medicine.client.service.UserService;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/7
 */
@Slf4j
@Component
public class AsyncUserLogService {


    private final UserService userService;

    public AsyncUserLogService(UserService userService) {
        this.userService = userService;
    }

    /**
     * 记录用户登陆日志
     *
     * @param userId 用户ID
     * @param ip     用户IP
     */
    @Async
    public void recordUserLoginLog(Long userId, String ip) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notEmpty(ip, "用户IP不能为空");
        userService.updateLoginInfo(userId, ip);
        System.out.println("记录用户登陆日志");
        log.info("用户ID：{}，IP：{}，登录成功", userId, ip);
    }

}
