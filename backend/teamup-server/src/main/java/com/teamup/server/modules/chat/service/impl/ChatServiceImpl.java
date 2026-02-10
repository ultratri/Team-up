package com.teamup.server.modules.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.chat.entity.Message;
import com.teamup.server.modules.chat.mapper.MessageMapper;
import com.teamup.server.modules.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageMapper messageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Message saveMessage(Message message) {
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }
        message.setUpdatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    @Override
    public List<Message> getTeamHistory(Long teamId, Integer limit) {
        // Using Page to limit results, sorted by ID desc (latest first)
        // Note: In real chat, we usually want latest N messages. 
        // If we sort DESC, we get latest. Frontend usually reverses them or prepends.
        Page<Message> page = new Page<>(1, limit);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getTeamId, teamId)
               .orderByDesc(Message::getCreatedAt);
        
        return messageMapper.selectPage(page, wrapper).getRecords();
    }
}
