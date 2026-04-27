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
 * Agent 提示词当前生效版本配置表。
 */
@TableName(value = "agent_prompt_config")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentPromptConfig implements Serializable {

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
     * 当前生效提示词正文。
     */
    private String promptContent;

    /**
     * 当前生效版本号。
     */
    private Long promptVersion;

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
