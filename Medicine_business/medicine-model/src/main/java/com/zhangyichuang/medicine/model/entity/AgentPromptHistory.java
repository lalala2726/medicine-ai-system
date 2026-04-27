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
 * Agent 提示词历史版本记录表。
 */
@TableName(value = "agent_prompt_history")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentPromptHistory implements Serializable {

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
     * 该历史记录对应的版本号。
     */
    private Long promptVersion;

    /**
     * 历史提示词正文。
     */
    private String promptContent;

    /**
     * 创建人账号。
     */
    private String createBy;

    /**
     * 创建时间。
     */
    private Date createdAt;
}
