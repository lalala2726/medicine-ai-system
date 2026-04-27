package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 会话实体类
 */
@TableName(value = "conversation")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Conversation {

    /**
     * 会话 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话 UUID
     */
    private String uuid;

    /**
     * 用户 ID
     */
    private Long userId;

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

    /**
     * 删除时间
     */
    private Date deleteTime;

    /**
     * 是否删除
     */
    private Integer isDelete;
}
