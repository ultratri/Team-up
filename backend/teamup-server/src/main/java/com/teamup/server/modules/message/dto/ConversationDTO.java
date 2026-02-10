package com.teamup.server.modules.message.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话DTO
 */
@Data
public class ConversationDTO {
    private Long id;
    private String conversationType;
    private String name;
    private String avatar;
    private MessageDTO lastMessage;
    private Long unreadCount;
    private Boolean isMuted;
    private Boolean isPinned;
    private LocalDateTime lastMessageTime;
}
