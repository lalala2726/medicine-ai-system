package com.zhangyichuang.medicine.common.core.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 基础实体类,
 *
 * @author Chuang
 */
@Data
@Schema(description = "基础实体类")
public class BaseEntity implements Serializable {

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    private Date updateTime;

    /**
     * 创建人
     */
    @Schema(description = "创建者")
    private String createBy;

    /**
     * 修改人
     */
    @Schema(description = "修改者")
    private String updateBy;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;


}
