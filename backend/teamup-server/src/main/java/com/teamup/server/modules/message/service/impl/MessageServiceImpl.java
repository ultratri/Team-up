package com.teamup.server.modules.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.message.dto.ConversationDTO;
import com.teamup.server.modules.message.dto.MessageDTO;
import com.teamup.server.modules.message.entity.ChatMessage;
import com.teamup.server.modules.message.entity.Conversation;
import com.teamup.server.modules.message.entity.ConversationMember;
import com.teamup.server.modules.message.mapper.ChatMessageMapper;
import com.teamup.server.modules.message.mapper.ConversationMapper;
import com.teamup.server.modules.message.mapper.ConversationMemberMapper;
import com.teamup.server.modules.message.mongo.ChatMessageDocRepository;
import com.teamup.server.modules.message.mongo.ChatMessageMongoConverter;
import com.teamup.server.modules.message.service.MessageService;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import com.teamup.server.modules.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ChatMessageMapper messageMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final WebSocketService webSocketService;
    private final ChatMessageDocRepository chatMessageDocRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageDTO sendMessage(Long conversationId, Long senderId, String messageType,
                                  String content, String fileUrl, String fileName, Long fileSize) {
        // 验证会话权限
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, conversationId)
               .eq(ConversationMember::getUserId, senderId);
        
        if (memberMapper.selectCount(wrapper) == 0) {
            throw new RuntimeException("您不是该会话成员");
        }
        
        // 创建消息
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setMessageType(messageType);
        message.setContent(content);
        message.setFileUrl(fileUrl);
        message.setFileName(fileName);
        message.setFileSize(fileSize);
        message.setIsRead(false);
        message.setIsRecalled(false);
        message.setCreatedAt(LocalDateTime.now());
        
        messageMapper.insert(message);

        try {
            chatMessageDocRepository.save(ChatMessageMongoConverter.toDoc(message));
        } catch (Exception e) {
            log.warn("MongoDB message write failed: {}", e.getMessage());
        }
        
        // 更新会话最后消息
        Conversation conversation = conversationMapper.selectById(conversationId);
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageTime(message.getCreatedAt());
        conversationMapper.updateById(conversation);
        
        // 更新其他成员的未读数
        LambdaQueryWrapper<ConversationMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ConversationMember::getConversationId, conversationId)
                    .ne(ConversationMember::getUserId, senderId);
        
        List<ConversationMember> members = memberMapper.selectList(memberWrapper);
        MessageDTO messageDTO = convertToMessageDTO(message);
        
        for (ConversationMember member : members) {
            member.setUnreadCount(member.getUnreadCount() + 1);
            memberMapper.updateById(member);
            
            // WebSocket 实时推送消息给其他成员
            webSocketService.pushMessage(member.getUserId(), messageDTO);
        }
        
        log.info("用户 {} 在会话 {} 发送了消息", senderId, conversationId);
        
        return messageDTO;
    }

    @Override
    public Page<MessageDTO> getConversationMessages(Long conversationId, Long userId, int page, int size) {
        LambdaQueryWrapper<ConversationMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ConversationMember::getConversationId, conversationId)
                    .eq(ConversationMember::getUserId, userId);
        
        if (memberMapper.selectCount(memberWrapper) == 0) {
            throw new RuntimeException("您不是该会话成员");
        }

        try {
            PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            org.springframework.data.domain.Page<com.teamup.server.modules.message.mongo.ChatMessageDoc> mongoPage =
                chatMessageDocRepository.findByConversationIdAndIsRecalledFalse(conversationId, pageable);

            if (mongoPage.hasContent()) {
                log.debug("Reading chat history from MongoDB for conversation {}", conversationId);
                List<MessageDTO> dtos = mongoPage.getContent().stream()
                    .map(ChatMessageMongoConverter::toEntity)
                    .map(this::convertToMessageDTO)
                    .collect(Collectors.toList());
                Page<MessageDTO> mpPage = new Page<>(mongoPage.getNumber() + 1, mongoPage.getSize(), mongoPage.getTotalElements());
                mpPage.setRecords(dtos);
                return mpPage;
            }
        } catch (Exception e) {
            log.warn("MongoDB read failed, falling back to MySQL. Error: {}", e.getMessage());
        }

        log.debug("Reading chat history from MySQL for conversation {}", conversationId);
        Page<ChatMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId)
               .eq(ChatMessage::getIsRecalled, false)
               .orderByDesc(ChatMessage::getCreatedAt);
        
        Page<ChatMessage> messagePage = messageMapper.selectPage(pageParam, wrapper);
        
        Page<MessageDTO> dtoPage = new Page<>(page, size, messagePage.getTotal());
        dtoPage.setRecords(messagePage.getRecords().stream()
                .map(this::convertToMessageDTO)
                .collect(Collectors.toList()));
        
        return dtoPage;
    }

    @Override
    public List<ConversationDTO> getUserConversations(Long userId) {
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getUserId, userId);
        
        List<ConversationMember> members = memberMapper.selectList(wrapper);
        
        return members.stream().map(member -> {
            Conversation conversation = conversationMapper.selectById(member.getConversationId());
            return convertToConversationDTO(conversation, member);
        }).sorted((a, b) -> {
            // 置顶的排前面
            if (a.getIsPinned() && !b.getIsPinned()) return -1;
            if (!a.getIsPinned() && b.getIsPinned()) return 1;
            // 按最后消息时间排序
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationDTO createPrivateConversation(Long user1Id, Long user2Id) {
        // 检查是否已存在私聊会话
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getUserId, user1Id);
        
        List<ConversationMember> user1Conversations = memberMapper.selectList(wrapper);
        
        for (ConversationMember member : user1Conversations) {
            Conversation conversation = conversationMapper.selectById(member.getConversationId());
            if ("PRIVATE".equals(conversation.getConversationType())) {
                // 检查对方是否也在此会话
                LambdaQueryWrapper<ConversationMember> checkWrapper = new LambdaQueryWrapper<>();
                checkWrapper.eq(ConversationMember::getConversationId, conversation.getId())
                           .eq(ConversationMember::getUserId, user2Id);
                
                if (memberMapper.selectCount(checkWrapper) > 0) {
                    // 已存在，返回现有会话
                    return convertToConversationDTO(conversation, member);
                }
            }
        }
        
        // 创建新会话
        Conversation conversation = new Conversation();
        conversation.setConversationType("PRIVATE");
        conversation.setCreatedAt(LocalDateTime.now());
        conversationMapper.insert(conversation);
        
        // 添加成员
        ConversationMember member1 = new ConversationMember();
        member1.setConversationId(conversation.getId());
        member1.setUserId(user1Id);
        member1.setUnreadCount(0L);
        member1.setIsMuted(false);
        member1.setIsPinned(false);
        member1.setJoinedAt(LocalDateTime.now());
        memberMapper.insert(member1);
        
        ConversationMember member2 = new ConversationMember();
        member2.setConversationId(conversation.getId());
        member2.setUserId(user2Id);
        member2.setUnreadCount(0L);
        member2.setIsMuted(false);
        member2.setIsPinned(false);
        member2.setJoinedAt(LocalDateTime.now());
        memberMapper.insert(member2);
        
        log.info("创建私聊会话: {} <-> {}", user1Id, user2Id);
        
        return convertToConversationDTO(conversation, member1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationDTO createGroupConversation(String name, String avatar, List<Long> memberIds, Long creatorId) {
        // 创建群聊会话
        Conversation conversation = new Conversation();
        conversation.setConversationType("GROUP");
        conversation.setName(name);
        conversation.setAvatar(avatar);
        conversation.setCreatedAt(LocalDateTime.now());
        conversationMapper.insert(conversation);
        
        // 添加成员
        for (Long memberId : memberIds) {
            ConversationMember member = new ConversationMember();
            member.setConversationId(conversation.getId());
            member.setUserId(memberId);
            member.setUnreadCount(0L);
            member.setIsMuted(false);
            member.setIsPinned(false);
            member.setJoinedAt(LocalDateTime.now());
            memberMapper.insert(member);
        }
        
        log.info("创建群聊会话: {}, 成员数: {}", name, memberIds.size());
        
        // 返回创建者视角的会话
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, conversation.getId())
               .eq(ConversationMember::getUserId, creatorId);
        
        ConversationMember creatorMember = memberMapper.selectOne(wrapper);
        return convertToConversationDTO(conversation, creatorMember);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMessagesAsRead(Long conversationId, Long userId, Long messageId) {
        // 更新未读数
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, conversationId)
               .eq(ConversationMember::getUserId, userId);
        
        ConversationMember member = memberMapper.selectOne(wrapper);
        if (member != null) {
            member.setUnreadCount(0L);
            member.setLastReadMessageId(messageId);
            member.setLastReadAt(LocalDateTime.now());
            memberMapper.updateById(member);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recallMessage(Long messageId, Long userId) {
        ChatMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("消息不存在");
        }
        
        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("只能撤回自己的消息");
        }
        
        // 检查是否在2分钟内
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
        if (message.getCreatedAt().isBefore(twoMinutesAgo)) {
            throw new RuntimeException("只能撤回2分钟内的消息");
        }
        
        message.setIsRecalled(true);
        messageMapper.updateById(message);
        
        log.info("用户 {} 撤回了消息 {}", userId, messageId);
    }

    @Override
    public Long getTotalUnreadCount(Long userId) {
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getUserId, userId);
        
        List<ConversationMember> members = memberMapper.selectList(wrapper);
        return members.stream()
                .mapToLong(ConversationMember::getUnreadCount)
                .sum();
    }

    @Override
    public List<MessageDTO> searchMessages(Long userId, String keyword) {
        // 获取用户的所有会话
        LambdaQueryWrapper<ConversationMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ConversationMember::getUserId, userId);
        
        List<ConversationMember> members = memberMapper.selectList(memberWrapper);
        List<Long> conversationIds = members.stream()
                .map(ConversationMember::getConversationId)
                .collect(Collectors.toList());
        
        if (conversationIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 搜索消息
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ChatMessage::getConversationId, conversationIds)
               .like(ChatMessage::getContent, keyword)
               .eq(ChatMessage::getIsRecalled, false)
               .orderByDesc(ChatMessage::getCreatedAt)
               .last("LIMIT 50");
        
        List<ChatMessage> messages = messageMapper.selectList(wrapper);
        return messages.stream()
                .map(this::convertToMessageDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pinConversation(Long conversationId, Long userId, boolean pinned) {
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, conversationId)
               .eq(ConversationMember::getUserId, userId);
        
        ConversationMember member = memberMapper.selectOne(wrapper);
        if (member != null) {
            member.setIsPinned(pinned);
            memberMapper.updateById(member);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void muteConversation(Long conversationId, Long userId, boolean muted) {
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, conversationId)
               .eq(ConversationMember::getUserId, userId);
        
        ConversationMember member = memberMapper.selectOne(wrapper);
        if (member != null) {
            member.setIsMuted(muted);
            memberMapper.updateById(member);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long conversationId, Long userId) {
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, conversationId)
               .eq(ConversationMember::getUserId, userId);
        
        memberMapper.delete(wrapper);
        
        // 如果没有成员了，删除会话和消息
        LambdaQueryWrapper<ConversationMember> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(ConversationMember::getConversationId, conversationId);
        
        if (memberMapper.selectCount(checkWrapper) == 0) {
            conversationMapper.deleteById(conversationId);
            
            LambdaQueryWrapper<ChatMessage> messageWrapper = new LambdaQueryWrapper<>();
            messageWrapper.eq(ChatMessage::getConversationId, conversationId);
            messageMapper.delete(messageWrapper);
        }
    }

    private MessageDTO convertToMessageDTO(ChatMessage message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setMessageType(message.getMessageType());
        dto.setContent(message.getContent());
        dto.setFileUrl(message.getFileUrl());
        dto.setFileName(message.getFileName());
        dto.setFileSize(message.getFileSize());
        dto.setIsRead(message.getIsRead());
        dto.setIsRecalled(message.getIsRecalled());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setReadAt(message.getReadAt());
        
        // 填充发送者信息
        User sender = userMapper.selectById(message.getSenderId());
        if (sender != null) {
            dto.setSenderName(sender.getUsername());
        }
        UserProfile senderProfile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, message.getSenderId())
        );
        if (senderProfile != null) {
            dto.setSenderAvatar(senderProfile.getAvatarUrl());
        }
        
        return dto;
    }

    private ConversationDTO convertToConversationDTO(Conversation conversation, ConversationMember member) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setConversationType(conversation.getConversationType());
        dto.setName(conversation.getName());
        dto.setAvatar(conversation.getAvatar());
        dto.setUnreadCount(member.getUnreadCount());
        dto.setIsMuted(member.getIsMuted());
        dto.setIsPinned(member.getIsPinned());
        dto.setLastMessageTime(conversation.getLastMessageTime());
        
        // 填充最后一条消息
        if (conversation.getLastMessageId() != null) {
            ChatMessage lastMessage = messageMapper.selectById(conversation.getLastMessageId());
            if (lastMessage != null) {
                dto.setLastMessage(convertToMessageDTO(lastMessage));
            }
        }
        
        // 对于私聊，使用对方的信息作为会话名称和头像
        if ("PRIVATE".equals(conversation.getConversationType())) {
            LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ConversationMember::getConversationId, conversation.getId())
                   .ne(ConversationMember::getUserId, member.getUserId());
            
            ConversationMember otherMember = memberMapper.selectOne(wrapper);
            if (otherMember != null) {
                User otherUser = userMapper.selectById(otherMember.getUserId());
                if (otherUser != null) {
                    dto.setName(otherUser.getUsername());
                    UserProfile otherProfile = userProfileMapper.selectOne(
                            new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, otherUser.getId())
                    );
                    if (otherProfile != null) {
                        dto.setAvatar(otherProfile.getAvatarUrl());
                    }
                }
            }
        }
        
        return dto;
    }
}
