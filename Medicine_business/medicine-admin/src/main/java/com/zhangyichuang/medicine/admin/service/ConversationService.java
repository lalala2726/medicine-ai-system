package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.Conversation;

/**
 * @author Chuang
 */
public interface ConversationService extends IService<Conversation> {
    /**
     * 新建会话（事务）
     */
    boolean createConversation(Conversation conversation);
}
