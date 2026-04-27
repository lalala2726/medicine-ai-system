package com.zhangyichuang.medicine.agent.model.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 管理端智能体用户列表视图。
 */
@Schema(description = "管理端智能体用户列表视图")
@FieldDescription(description = "管理端智能体用户列表视图")
@Data
public class AgentUserListVo {

    @Schema(description = "用户ID", format = "int64", example = "1")
    @FieldDescription(description = "用户ID")
    private Long id;

    @Schema(description = "用户名", example = "zhangsan")
    @FieldDescription(description = "用户名")
    private String username;

    @Schema(description = "昵称", example = "张三")
    @FieldDescription(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    @FieldDescription(description = "头像URL")
    private String avatar;

    @Schema(description = "角色", example = "admin")
    @FieldDescription(description = "角色")
    private String roles;

    @Schema(description = "状态", example = "1")
    @FieldDescription(description = "状态")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_USER_STATUS)
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2023-01-01 00:00:00")
    @FieldDescription(description = "创建时间")
    private Date createTime;
}
