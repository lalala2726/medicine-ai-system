package com.zhangyichuang.medicine.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "流式聊天响应块")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StreamChatResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "会话UUID")
    private String uuid;

    @Schema(description = "消息UUID（完成时返回）")
    private String messageUuid;

    @Schema(description = "响应唯一标识", example = "chat_123456789")
    private String responseId;

    @Schema(description = "工作流执行阶段编码")
    private String stage;

    @Schema(description = "阶段说明")
    private String stageMessage;

    @Schema(description = "工具名称")
    private String toolName;

    @Schema(description = "工具输出或提示信息")
    private String toolMessage;

    @Schema(description = "流式内容块", example = "你好")
    private String content;

    @Schema(description = "是否完成响应", example = "false")
    private Boolean finished;

}
