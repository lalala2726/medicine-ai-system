package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 操作日志列表查询参数。
 */
@Data
@Schema(description = "操作日志列表查询参数")
@EqualsAndHashCode(callSuper = true)
public class OperationLogQueryRequest extends PageRequest {

    @Schema(description = "业务模块", example = "用户管理")
    private String module;

    @Schema(description = "操作说明", example = "新增用户")
    private String action;

    @Schema(description = "操作人账号", example = "admin")
    private String username;

    @Schema(description = "是否成功：1成功 0失败", example = "1")
    private Integer success;

    @Schema(description = "开始时间", example = "2026-02-12 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @Schema(description = "结束时间", example = "2026-02-12 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
