package com.zhangyichuang.medicine.common.core.exception;

import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 全局异常处理
 *
 * @author Chuang
 * <p>
 * created on 2025/1/11
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandel {

    /**
     * 浏览器默认请求的网站图标路径。
     */
    private static final String FAVICON_PATH = "/favicon.ico";

    /**
     * 处理业务逻辑中抛出的自定义业务异常。
     * ServiceException 通常表示在业务流程中检测到的错误，例如非法操作或不符合预期的状态。
     *
     * @param exception 包含错误信息和状态码的异常对象
     * @return 返回包含错误信息和状态码的 AjaxResult 对象
     */
    @ExceptionHandler(ServiceException.class)
    public AjaxResult<Void> serviceExceptionHandel(ServiceException exception) {
        log.error("业务异常", exception);
        return AjaxResult.error(exception.getMessage(), exception.getCode());
    }

    /**
     * 处理分布式锁获取失败等锁相关异常。
     *
     * @param exception 分布式锁异常对象
     * @return 返回包含错误信息和状态码的 AjaxResult 对象
     */
    @ExceptionHandler(DistributedLockException.class)
    public AjaxResult<Void> distributedLockExceptionHandel(DistributedLockException exception) {
        log.error("分布式锁异常", exception);
        return AjaxResult.error(exception.getMessage(), exception.getCode());
    }

    /**
     * 处理重复提交异常。
     *
     * @param exception 重复提交异常对象
     * @return 返回包含错误信息和状态码的 AjaxResult 对象
     */
    @ExceptionHandler(DuplicateSubmitException.class)
    public AjaxResult<Void> duplicateSubmitExceptionHandel(DuplicateSubmitException exception) {
        log.error("重复提交异常", exception);
        return AjaxResult.error(exception.getMessage(), exception.getCode());
    }

    /**
     * 处理访问限流异常。
     *
     * @param exception 访问限流异常对象
     * @return 返回包含错误信息和状态码的 AjaxResult 对象
     */
    @ExceptionHandler(AccessLimitException.class)
    public AjaxResult<Void> accessLimitExceptionHandel(AccessLimitException exception) {
        log.error("访问限流异常", exception);
        return AjaxResult.error(exception.getMessage(), exception.getCode());
    }

    /**
     * 处理请求方法不被支持的情况。
     * HttpRequestMethodNotSupportedException 表示客户端使用了服务器端不支持的 HTTP 方法（如 GET、POST 等）。
     *
     * @param exception 包含错误信息的异常对象
     * @return 返回表示“请求方法不支持”的 AjaxResult 对象
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult<Void> httpRequestMethodNotSupportedExceptionHandel(HttpRequestMethodNotSupportedException exception) {
        log.error("请求方法不支持", exception);
        return AjaxResult.error(ResponseCode.NOT_SUPPORT);
    }

    /**
     * 处理请求参数无法解析的情况。
     * HttpMessageNotReadableException 表示 Spring MVC 在反序列化请求体时遇到问题，比如 JSON 格式错误。
     *
     * @param exception 包含错误信息的异常对象
     * @return 返回表示“请求参数非法”的 AjaxResult 对象
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public AjaxResult<Void> httpMessageNotReadableExceptionHandel(HttpMessageNotReadableException exception) {
        log.error("请求参数非法: ", exception);
        String message = resolveReadableMessage(exception);
        return AjaxResult.error(ResponseCode.PARAM_ERROR, message);
    }

    /**
     * 处理请求参数类型不匹配的情况。
     * MethodArgumentTypeMismatchException 表示控制器方法期望的参数类型与实际传入的值不兼容，例如期望整数但收到字符串。
     *
     * @param exception 包含参数名、期望类型及实际值的异常对象
     * @return 返回详细描述类型转换失败原因的 AjaxResult 对象
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult<Void> methodArgumentTypeMismatchExceptionHandel(MethodArgumentTypeMismatchException exception) {
        log.error("请求参数类型不匹配", exception);
        String paramName = exception.getName();
        String errorMessage = null;
        if (exception.getRequiredType() != null) {
            errorMessage = String.format("参数 '%s': 类型转换失败 - 期望类型 %s, 实际值: '%s'",
                    paramName,
                    exception.getRequiredType().getSimpleName(),
                    exception.getValue());
        }
        return AjaxResult.error(ResponseCode.PARAM_ERROR, errorMessage);
    }

    /**
     * 处理非法参数导致的 IllegalArgumentException。
     * IllegalArgumentException 表示传递给方法的参数非法或格式不正确。
     *
     * @param exception 包含错误信息的异常对象
     * @return 返回表示“请求参数非法”的 AjaxResult 对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public AjaxResult<Void> illegalArgumentExceptionHandel(IllegalArgumentException exception) {
        log.error("请求参数非法: ", exception);
        return AjaxResult.error(ResponseCode.PARAM_ERROR, exception.getMessage());
    }


    /**
     * 处理登录相关的自定义异常。
     * AuthorizationException 表示在身份验证过程中发生错误，如无效凭证或登录失败。
     *
     * @param exception 包含错误信息的异常对象
     * @return 返回表示“登录失败”的 AjaxResult 对象
     */
    @ExceptionHandler(AuthorizationException.class)
    public AjaxResult<Void> authorizationExceptionHandel(AuthorizationException exception) {
        log.error("认证失败:", exception);
        return AjaxResult.error(exception.getCode(), exception.getMessage());
    }

    /**
     * 权限不足异常
     *
     * @param exception 权限不足异常
     * @return 响应权限不足
     */
    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult<Void> accessDeniedExceptionHandel(AccessDeniedException exception) {
        log.error("权限不足:", exception);
        return AjaxResult.error(exception.getCode(), exception.getMessage());
    }

    /**
     * 认证失败异常
     *
     * @param exception 认证失败异常
     * @return 响应认证失败
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public AjaxResult<Void> authorizationDeniedExceptionHandel(AuthorizationDeniedException exception) {
        log.error("授权失败", exception);
        return AjaxResult.error(ResponseCode.FORBIDDEN.getCode(), "您没有权限访问此资源!");
    }

    /**
     * 登录异常
     */
    @ExceptionHandler(LoginException.class)
    public AjaxResult<Void> loginExceptionHandel(LoginException exception) {
        log.error("登录失败:", exception);
        return AjaxResult.error(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理参数校验失败的情况。
     * ParamException 是一个通用的参数校验异常类，用于表示参数不满足业务需求。
     *
     * @param exception 包含错误信息的异常对象
     * @return 返回表示“参数异常”的 AjaxResult 对象
     */
    @ExceptionHandler(ParamException.class)
    public AjaxResult<Void> paramExceptionHandel(ParamException exception) {
        log.error("参数异常:", exception);
        return AjaxResult.error(ResponseCode.PARAM_ERROR, exception.getMessage());
    }


    /**
     * 处理控制器方法参数校验失败的情况。
     * MethodArgumentNotValidException 表示通过 JSR-380 注解（如 @Valid）校验失败。
     *
     * @param exception 包含字段错误信息的异常对象
     * @return 返回具体的字段校验错误信息的 AjaxResult 对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult<Void> methodArgumentNotValidExceptionHandel(MethodArgumentNotValidException exception) {
        log.error("参数校验失败:", exception);
        return AjaxResult.error(ResponseCode.PARAM_ERROR, resolveBindingErrorMessage(exception.getBindingResult().getFieldErrors()));
    }

    /**
     * 处理查询参数/表单参数校验失败的情况。
     *
     * @param exception 包含字段错误信息的异常对象
     * @return 返回具体的字段校验错误信息的 AjaxResult 对象
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult<Void> bindExceptionHandel(BindException exception) {
        log.error("参数绑定校验失败:", exception);
        return AjaxResult.error(ResponseCode.PARAM_ERROR, resolveBindingErrorMessage(exception.getBindingResult().getFieldErrors()));
    }

    /**
     * 处理找不到请求的资源的情况。
     * NoResourceFoundException 表示客户端请求了一个不存在的 URL 资源。
     *
     * @param exception 包含错误信息的异常对象
     * @param request   当前的 HttpServletRequest 对象，用于获取请求 URI
     * @return 返回表示“资源不存在”的 AjaxResult 对象，并附带请求的 URI
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public AjaxResult<Void> noResourceFoundExceptionHandel(NoResourceFoundException exception, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (FAVICON_PATH.equals(requestUri)) {
            log.debug("忽略浏览器默认 favicon 请求：{}", exception.toString());
        } else {
            log.warn("资源不存在：{}", exception.toString());
        }
        String message = String.format("资源不存在: %s", requestUri);
        return AjaxResult.error(ResponseCode.NOT_FOUND, message);
    }


    /**
     * 处理缺少必需的请求参数的情况。
     * MissingServletRequestParameterException 表示控制器方法需要某个请求参数，但该参数未提供。
     *
     * @param exception 包含缺失参数名称的异常对象
     * @return 返回表示“缺少请求参数”的 AjaxResult 对象，并指出具体缺失的参数名
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public AjaxResult<Void> missingServletRequestParameterExceptionHandel(MissingServletRequestParameterException exception) {
        log.error("缺少请求参数", exception);
        String message = exception.getMessage();
        String paramName = "";
        if (message.contains("'")) {
            int start = message.indexOf("'") + 1;
            int end = message.indexOf("'", start);
            if (end > start) {
                paramName = message.substring(start, end);
            }
        }
        return AjaxResult.error(ResponseCode.PARAM_ERROR, "缺少请求参数: " + paramName);
    }

    /**
     * 处理所有未明确捕获的系统异常。
     * Exception 是通用的捕获所有异常的兜底处理器，用于防止未预料的错误导致服务崩溃。
     *
     * @param exception 包含错误信息的异常对象
     * @return 返回表示“系统异常”的 AjaxResult 对象
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult<Void> exceptionHandel(Exception exception) {
        log.error("系统异常", exception);
        return AjaxResult.error(ResponseCode.SERVER_ERROR, exception.getMessage());
    }

    private String resolveReadableMessage(HttpMessageNotReadableException exception) {
        if (isMissingRequestBody(exception)) {
            return "请求体不能为空";
        }
        Throwable root = exception.getMostSpecificCause();
        switch (root) {
            case StreamReadException ignored -> {
                return "请求体 JSON 格式错误";
            }
            case InvalidFormatException invalidFormatException -> {
                String field = buildPath(invalidFormatException.getPath());
                String target = friendlyType(invalidFormatException.getTargetType());
                String value = invalidFormatException.getValue() == null ? "null" : String.valueOf(invalidFormatException.getValue());
                if (field.isBlank()) {
                    return String.format("请求体字段类型不匹配，期望 %s，实际值: %s", target, value);
                }
                return String.format("参数 '%s' 类型不匹配，期望 %s，实际值: %s", field, target, value);
            }
            case MismatchedInputException mismatchedInputException -> {
                String field = buildPath(mismatchedInputException.getPath());
                String target = friendlyType(mismatchedInputException.getTargetType());
                String actual = resolveActualType(mismatchedInputException);
                if (field.isBlank()) {
                    return actual == null
                            ? String.format("请求体字段类型不匹配，期望 %s", target)
                            : String.format("请求体字段类型不匹配，期望 %s，实际为 %s", target, actual);
                }
                return actual == null
                        ? String.format("参数 '%s' 类型不匹配，期望 %s", field, target)
                        : String.format("参数 '%s' 类型不匹配，期望 %s，实际为 %s", field, target, actual);
            }
            default -> {
            }
        }
        return "请求体 JSON 解析失败";
    }

    private boolean isMissingRequestBody(HttpMessageNotReadableException exception) {
        String message = exception.getMessage();
        return message != null && message.contains("Required request body is missing");
    }

    private String buildPath(List<JacksonException.Reference> path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JacksonException.Reference reference : path) {
            if (reference.getPropertyName() != null) {
                if (!builder.isEmpty()) {
                    builder.append('.');
                }
                builder.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                builder.append('[').append(reference.getIndex()).append(']');
            }
        }
        return builder.toString();
    }

    private String friendlyType(Class<?> targetType) {
        if (targetType == null) {
            return "未知类型";
        }
        if (Collection.class.isAssignableFrom(targetType) || targetType.isArray()) {
            return "数组";
        }
        if (Map.class.isAssignableFrom(targetType)) {
            return "对象";
        }
        return targetType.getSimpleName();
    }

    private String resolveActualType(MismatchedInputException exception) {
        String message = exception.getOriginalMessage();
        if (message == null) {
            return null;
        }
        if (message.contains("from String value")) {
            return "字符串";
        }
        if (message.contains("from Array value")) {
            return "数组";
        }
        if (message.contains("from Object value")) {
            return "对象";
        }
        if (message.contains("from Number value")) {
            return "数字";
        }
        if (message.contains("from Boolean value")) {
            return "布尔值";
        }
        return null;
    }

    private String resolveBindingErrorMessage(List<org.springframework.validation.FieldError> fieldErrors) {
        StringBuilder errorMessage = new StringBuilder();
        fieldErrors.forEach(fieldError -> {
            if (!errorMessage.isEmpty()) {
                errorMessage.append("; ");
            }
            errorMessage.append(fieldError.getDefaultMessage());
        });
        return !errorMessage.isEmpty() ? errorMessage.toString() : "参数校验失败";
    }

}
