package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Agent 提示词业务键配置表。
 */
@TableName(value = "agent_prompt_key")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentPromptKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 提示词业务键。
     */
    private String promptKey;

    /**
     * 提示词用途说明。
     */
    private String description;

    /**
     * 创建人账号。
     */
    private String createBy;

    /**
     * 最后更新人账号。
     */
    private String updateBy;

    /**
     * 创建时间。
     */
    private Date createdAt;

    /**
     * 最后更新时间。
     */
    private Date updatedAt;
}
