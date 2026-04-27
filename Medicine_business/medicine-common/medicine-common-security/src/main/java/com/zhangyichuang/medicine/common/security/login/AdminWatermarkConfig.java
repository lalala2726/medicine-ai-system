package com.zhangyichuang.medicine.common.security.login;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理端水印配置。
 */
@Data
public class AdminWatermarkConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否启用管理端水印。
     */
    private Boolean enabled;

    /**
     * 是否展示用户名。
     */
    private Boolean showUsername;

    /**
     * 是否展示用户ID。
     */
    private Boolean showUserId;
}
