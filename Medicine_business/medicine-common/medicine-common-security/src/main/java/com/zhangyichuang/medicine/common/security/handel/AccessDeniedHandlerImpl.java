package com.zhangyichuang.medicine.common.security.handel;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.utils.ResponseUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 授权失败处理类
 */
@Slf4j
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        String requestUri = request != null ? request.getRequestURI() : "";
        String message = String.format("请求访问:%s 权限不足，无法访问系统资源", requestUri);
        log.error("权限不足: {}", message, exception);
        ResponseUtils.writeErrMsg(response, HttpStatus.OK, ResponseCode.FORBIDDEN, message);
    }
}
