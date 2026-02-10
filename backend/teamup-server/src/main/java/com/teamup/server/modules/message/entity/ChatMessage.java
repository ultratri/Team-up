package com.teamup.server.modules.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Data
@TableName("chat_messages")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long conversationId;  // 会话ID
    private Long senderId;        // 发送者ID
    private String messageType;   // TEXT, IMAGE, FILE, SYSTEM
    private String content;       // 消息内容
    private String fileUrl;       // 文件URL（图片/文件消息）
    private String fileName;      // 文件名
    private Long fileSize;        // 文件大小
    private Boolean isRead;       // 是否已读
    private Boolean isRecalled;   // 是否撤回
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
