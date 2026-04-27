package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 操作日志详情。
 */
@Data
@Schema(description = "操作日志详情")
public class OperationLogVo {

    @Schema(description = "日志ID", example = "1")
    private long id = 0;

    @Schema(description = "业务模块", example = "用户管理")
    private String module;

    @Schema(description = "操作说明", example = "新增用户")
    private String action;

    @Schema(description = "请求URI", example = "/system/user")
    private String requestUri;

    @Schema(description = "HTTP方法", example = "POST")
    private String httpMethod;

    @Schema(description = "方法名", example = "UserController.addUser")
    private String methodName;

    @Schema(description = "操作人ID", example = "1")
    private Long userId;

    @Schema(description = "操作人账号", example = "admin")
    private String username;

    @Schema(description = "请求IP", example = "localhost")
    private String ip;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "请求参数")
    private String requestParams;

    @Schema(description = "返回结果")
    private String responseResult;

    @Schema(description = "耗时(ms)", example = "120")
    private Long costTime;

    @Schema(description = "是否成功：1成功 0失败", example = "1")
    private Integer success;

    @Schema(description = "异常信息")
    private String errorMsg;

    @Schema(description = "创建时间", example = "2026-02-12 10:00:00")
    private Date createTime;
}
