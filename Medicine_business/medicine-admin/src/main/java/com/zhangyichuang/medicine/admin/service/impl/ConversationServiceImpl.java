package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.ConversationMapper;
import com.zhangyichuang.medicine.admin.service.ConversationService;
import com.zhangyichuang.medicine.model.entity.Conversation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chuang
 */
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation>
        implements ConversationService {
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createConversation(Conversation conversation) {
        return save(conversation);
    }
}




