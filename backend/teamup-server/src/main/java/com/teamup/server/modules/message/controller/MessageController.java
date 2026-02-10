package com.teamup.server.modules.message.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.message.dto.ConversationDTO;
import com.teamup.server.modules.message.dto.MessageDTO;
import com.teamup.server.modules.message.service.MessageService;
import com.teamup.server.modules.user.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息控制器
 */
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 获取用户的所有会话
     */
    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public Result<List<ConversationDTO>> getConversations() {
        Long userId = UserContext.getCurrentUserId();
        List<ConversationDTO> conversations = messageService.getUserConversations(userId);
        return Result.success(conversations);
    }

    /**
     * 创建私聊会话
     */
    @PostMapping("/conversations/private")
    @PreAuthorize("isAuthenticated()")
    public Result<ConversationDTO> createPrivateConversation(@RequestBody Map<String, Long> request) {
        Long userId = UserContext.getCurrentUserId();
        Long otherUserId = request.get("userId");
        ConversationDTO conversation = messageService.createPrivateConversation(userId, otherUserId);
        return Result.success(conversation);
    }

    /**
     * 创建群聊会话
     */
    @PostMapping("/conversations/group")
    @PreAuthorize("isAuthenticated()")
    public Result<ConversationDTO> createGroupConversation(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getCurrentUserId();
        String name = (String) request.get("name");
        String avatar = (String) request.get("avatar");
        @SuppressWarnings("unchecked")
        List<Long> memberIds = (List<Long>) request.get("memberIds");
        
        ConversationDTO conversation = messageService.createGroupConversation(name, avatar, memberIds, userId);
        return Result.success(conversation);
    }

    /**
     * 获取会话消息列表
     */
    @GetMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("isAuthenticated()")
    public Result<Page<MessageDTO>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Long userId = UserContext.getCurrentUserId();
        Page<MessageDTO> messages = messageService.getConversationMessages(conversationId, userId, page, size);
        return Result.success(messages);
    }

    /**
     * 发送消息
     */
    @PostMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("isAuthenticated()")
    public Result<MessageDTO> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody Map<String, Object> request
    ) {
        Long userId = UserContext.getCurrentUserId();
        String messageType = (String) request.get("messageType");
        String content = (String) request.get("content");
        String fileUrl = (String) request.get("fileUrl");
        String fileName = (String) request.get("fileName");
        Long fileSize = request.get("fileSize") != null ? 
                       ((Number) request.get("fileSize")).longValue() : null;
        
        MessageDTO message = messageService.sendMessage(
                conversationId, userId, messageType, content, fileUrl, fileName, fileSize
        );
        return Result.success(message);
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/conversations/{conversationId}/read")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> markAsRead(
            @PathVariable Long conversationId,
            @RequestBody Map<String, Long> request
    ) {
        Long userId = UserContext.getCurrentUserId();
        Long messageId = request.get("messageId");
        messageService.markMessagesAsRead(conversationId, userId, messageId);
        return Result.success();
    }

    /**
     * 撤回消息
     */
    @PutMapping("/messages/{messageId}/recall")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> recallMessage(@PathVariable Long messageId) {
        Long userId = UserContext.getCurrentUserId();
        messageService.recallMessage(messageId, userId);
        return Result.success();
    }

    /**
     * 获取未读总数
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public Result<Long> getUnreadCount() {
        Long userId = UserContext.getCurrentUserId();
        Long count = messageService.getTotalUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 搜索消息
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public Result<List<MessageDTO>> searchMessages(@RequestParam String keyword) {
        Long userId = UserContext.getCurrentUserId();
        List<MessageDTO> messages = messageService.searchMessages(userId, keyword);
        return Result.success(messages);
    }

    /**
     * 置顶会话
     */
    @PutMapping("/conversations/{conversationId}/pin")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> pinConversation(
            @PathVariable Long conversationId,
            @RequestBody Map<String, Boolean> request
    ) {
        Long userId = UserContext.getCurrentUserId();
        Boolean pinned = request.get("pinned");
        messageService.pinConversation(conversationId, userId, pinned);
        return Result.success();
    }

    /**
     * 免打扰会话
     */
    @PutMapping("/conversations/{conversationId}/mute")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> muteConversation(
            @PathVariable Long conversationId,
            @RequestBody Map<String, Boolean> request
    ) {
        Long userId = UserContext.getCurrentUserId();
        Boolean muted = request.get("muted");
        messageService.muteConversation(conversationId, userId, muted);
        return Result.success();
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> deleteConversation(@PathVariable Long conversationId) {
        Long userId = UserContext.getCurrentUserId();
        messageService.deleteConversation(conversationId, userId);
        return Result.success();
    }
}
