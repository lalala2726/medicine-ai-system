package com.zhangyichuang.medicine.common.core.enums;

import lombok.Getter;

/**
 * 响应码枚举，定义所有返回给前端的状态码及其含义
 *
 * @author Chuang
 */
@Getter
public enum ResponseCode {

    // region 通用成功与错误状态
    // 当操作成功完成时的响应
    SUCCESS(200, "操作成功"),
    // 当操作完成但有警告时的响应
    WARNING(400, "操作警告"),
    // 当操作执行过程中发生未知错误时的响应
    ERROR(500, "操作失败"),
    // 明确标识为服务器内部错误
    SERVER_ERROR(500, "服务器错误"),
    // 明确标识为系统级异常
    SYSTEM_ERROR(500, "系统错误！"),
    // 通用操作失败
    OPERATION_ERROR(400, "操作失败"),
    // 文件读写等失败
    FILE_OPERATION_FAILED(500, "文件操作失败！"),
    // endregion

    // region 请求相关错误状态
    // 请求参数错误
    PARAM_ERROR(400, "参数错误!"),
    // 请求参数缺失
    PARAM_NOT_NULL(400, "参数不能为空"),
    // 参数超出限制
    PARAM_ERROR_TOO_LARGE(400, "请求参数过大"),

    // 请求资源不存在
    NOT_FOUND(404, "未找到该资源"),
    // 请求方法不被允许
    NOT_SUPPORT(405, "不支持该请求"),
    // 下游业务模块尚未就绪
    SERVICE_UNAVAILABLE(503, "业务模块未就绪，请稍后再试"),
    // endregion

    // region 认证与授权错误状态
    // 请求未提供有效身份信息
    UNAUTHORIZED(401, "未授权"),
    // 有身份但无权限
    FORBIDDEN(403, "权限不足,禁止访问"),
    // 用户未登录
    NOT_LOGIN(401, "未登录"),
    USER_NOT_LOGIN(401, "用户未登录"),
    TOKEN_EXPIRED(401, "凭证已过期,请重新登录"),
    TOKEN_EXPIRE(401, "会话已过期,请重新登录"),
    TOKEN_MISS(401, "缺少令牌"),
    AUTHORIZED(401, "认证授权失败"),
    ILLEGAL_TOKEN(401, "非法的token"),
    TOKEN_ERROR(401, "token错误"),
    INVALID_TOKEN(401, "token非法"),
    // endregion

    // region 用户相关错误状态
    USER_NOT_EXIST(404, "用户不存在"),
    // 资源冲突
    USER_EXIST(409, "用户已存在"),
    USER_PASSWORD_ERROR(400, "用户名或密码错误"),
    USERNAME_FORMAT_ERROR(400, "用户名不合法"),
    PASSWORD_FORMAT_ERROR(400, "密码不合法"),
    LOGIN_ERROR(4013, "登录失败"),
    USER_NOT_ADMIN(403, "用户不是管理员"),
    USER_NOT_ACTIVE(403, "用户未激活"),
    USER_NOT_VERIFY(403, "用户未验证"),
    USER_NOT_BIND(403, "用户未绑定"),
    ACCOUNT_LOCKED(403, "账号被锁定"),
    ACCOUNT_ERROR(400, "账号异常"),
    // endregion

    // region 数据相关错误状态
    DATA_NOT_FOUND(404, "数据未找到"),
    // 无内容返回，语义更标准
    RESULT_IS_NULL(204, "查询为空"),
    DELETE_ERROR(400, "删除失败"),
    // endregion

    // region 字典相关错误状态
    DICT_NAME_EXIST(409, "字典名称已存在"),
    DICT_TYPE_EXIST(409, "字典键已存在"),
    DICT_NAME_ERROR(400, "只能是英文数字和下划线"),
    // endregion

    // region 文件与配置相关
    FileNameIsNull(400, "文件名不能为空"),
    FileUploadFailed(500, "文件上传失败"),
    PROFILE_ERROR(500, "配置文件错误"),
    // endregion

    EXCEL_ERROR(500, "Excel文件错误"),

    // region 其他错误状态
    NOT_EXIST(404, "不存在"),
    NOT_ALLOW(403, "不允许"),
    ACCESS_DENIED(403, "您没有权限访问本资源"),
    // 使用 429 更标准
    REQUEST_LIMIT(429, "请求超过限制"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    // endregion

    FILE_OPERATION_ERROR(500, "文件操作失败"),
    ACCESS_TOKEN_INVALID(4011, "访问令牌已过期"),
    REFRESH_TOKEN_INVALID(4012, "刷新令牌无效或已过期"),
    SERVER_CANNOT_SUPPORT(500, "服务器无法支持本次请求");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 提示信息
     */
    private final String message;

    ResponseCode(Integer code, String msg) {
        this.code = code;
        this.message = msg;
    }

}
