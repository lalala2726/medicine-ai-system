package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 登录日志列表查询参数。
 */
@Data
@Schema(description = "登录日志列表查询参数")
@EqualsAndHashCode(callSuper = true)
public class LoginLogQueryRequest extends PageRequest {

    @Schema(description = "登录账号", example = "admin")
    private String username;

    @Schema(description = "登录来源：admin/client", example = "admin")
    private String loginSource;

    @Schema(description = "登录状态：1成功 0失败", example = "1")
    private Integer loginStatus;

    @Schema(description = "登录方式", example = "password")
    private String loginType;

    @Schema(description = "IP地址", example = "localhost")
    private String ipAddress;

    @Schema(description = "开始时间", example = "2026-02-12 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @Schema(description = "结束时间", example = "2026-02-12 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
