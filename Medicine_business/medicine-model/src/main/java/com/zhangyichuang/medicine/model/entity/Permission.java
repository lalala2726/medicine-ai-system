package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 权限表
 */
@TableName(value = "permission")
@Data
public class Permission {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 父权限ID
     */
    private Long parentId;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态（0:启用, 1:禁用）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;
}
