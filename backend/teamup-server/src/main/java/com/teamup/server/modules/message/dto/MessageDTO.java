package com.teamup.server.modules.message.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息DTO
 */
@Data
public class MessageDTO {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String messageType;
    private String content;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Boolean isRead;
    private Boolean isRecalled;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
