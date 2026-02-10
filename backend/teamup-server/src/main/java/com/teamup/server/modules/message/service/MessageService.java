package com.teamup.server.modules.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.message.dto.ConversationDTO;
import com.teamup.server.modules.message.dto.MessageDTO;
import com.teamup.server.modules.message.entity.ChatMessage;

import java.util.List;

/**
 * 消息服务接口
 */
public interface MessageService {
    /**
     * 发送消息
     */
    MessageDTO sendMessage(Long conversationId, Long senderId, String messageType, 
                          String content, String fileUrl, String fileName, Long fileSize);
    
    /**
     * 获取会话消息列表
     */
    Page<MessageDTO> getConversationMessages(Long conversationId, Long userId, int page, int size);
    
    /**
     * 获取用户的所有会话
     */
    List<ConversationDTO> getUserConversations(Long userId);
    
    /**
     * 创建私聊会话
     */
    ConversationDTO createPrivateConversation(Long user1Id, Long user2Id);
    
    /**
     * 创建群聊会话
     */
    ConversationDTO createGroupConversation(String name, String avatar, List<Long> memberIds, Long creatorId);
    
    /**
     * 标记消息为已读
     */
    void markMessagesAsRead(Long conversationId, Long userId, Long messageId);
    
    /**
     * 撤回消息
     */
    void recallMessage(Long messageId, Long userId);
    
    /**
     * 获取未读总数
     */
    Long getTotalUnreadCount(Long userId);
    
    /**
     * 搜索消息
     */
    List<MessageDTO> searchMessages(Long userId, String keyword);
    
    /**
     * 设置会话置顶
     */
    void pinConversation(Long conversationId, Long userId, boolean pinned);
    
    /**
     * 设置会话免打扰
     */
    void muteConversation(Long conversationId, Long userId, boolean muted);
    
    /**
     * 删除会话
     */
    void deleteConversation(Long conversationId, Long userId);
}
