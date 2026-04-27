package com.zhangyichuang.medicine.admin.listener;

import com.zhangyichuang.medicine.admin.service.AgentPromptSyncTaskService;
import com.zhangyichuang.medicine.common.rabbitmq.constants.AgentPromptSyncQueueConstants;
import com.zhangyichuang.medicine.model.mq.AgentPromptSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Agent 提示词同步任务消息监听器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentPromptSyncListener {

    /**
     * Agent 提示词同步任务服务。
     */
    private final AgentPromptSyncTaskService agentPromptSyncTaskService;

    /**
     * 消费 Agent 提示词同步任务消息，并委托服务层执行同步。
     *
     * @param message 同步任务消息
     * @return 无返回值
     */
    @RabbitListener(queues = AgentPromptSyncQueueConstants.QUEUE, concurrency = "1")
    public void handlePromptSync(AgentPromptSyncMessage message) {
        if (message == null) {
            log.warn("跳过Agent提示词同步任务消息: message is null");
            return;
        }
        agentPromptSyncTaskService.executeSyncTask(message);
    }
}
