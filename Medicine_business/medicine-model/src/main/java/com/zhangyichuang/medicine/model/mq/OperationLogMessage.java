package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 操作日志消息体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务模块。
     */
    private String module;

    /**
     * 操作说明。
     */
    private String action;

    /**
     * 请求URI。
     */
    private String requestUri;

    /**
     * HTTP方法。
     */
    private String httpMethod;

    /**
     * Controller方法名。
     */
    private String methodName;

    /**
     * 操作人ID。
     */
    private Long userId;

    /**
     * 操作人账号。
     */
    private String username;

    /**
     * 请求IP。
     */
    private String ip;

    /**
     * User-Agent。
     */
    private String userAgent;

    /**
     * 请求参数(JSON)。
     */
    private String requestParams;

    /**
     * 返回结果(JSON)。
     */
    private String responseResult;

    /**
     * 耗时(ms)。
     */
    private Long costTime;

    /**
     * 是否成功：1成功 0失败。
     */
    private Integer success;

    /**
     * 异常信息。
     */
    private String errorMsg;

    /**
     * 创建时间。
     */
    private Date createTime;
}
